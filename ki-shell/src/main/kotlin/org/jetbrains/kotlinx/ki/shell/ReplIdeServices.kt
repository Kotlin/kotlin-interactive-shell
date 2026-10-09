package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.completion.COMPLETION_MARKER
import org.jetbrains.kotlinx.ki.completion.CompletionCandidate
import org.jetbrains.kotlinx.ki.completion.CompletionSink
import org.jetbrains.kotlinx.ki.completion.TYPE_PROBE
import java.util.UUID
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asErrorDiagnostics

interface ReplIdeServices {
    fun complete(code: String, cursor: Int): List<CompletionItem>
    fun inferType(expr: String): ResultWithDiagnostics<String>
}

// [type] is the type of a property or the return type of a function, [parameters] is the parameter list of a function
data class CompletionItem(val text: String, val kind: Kind, val type: String? = null, val parameters: String? = null) {
    enum class Kind { FUNCTION, PROPERTY, CLASS, KEYWORD, PACKAGE }
}

class BasicReplIdeServices(private val declarations: () -> Iterable<CompletionItem>) : ReplIdeServices {

    override fun complete(code: String, cursor: Int): List<CompletionItem> {
        val beforeCursor = code.substring(0, cursor.coerceIn(0, code.length))
        val prefix = beforeCursor.takeLastWhile { Character.isJavaIdentifierPart(it) }
        if (beforeCursor.dropLast(prefix.length).endsWith(".")) return emptyList()
        val names = declarations().filter { it.text.startsWith(prefix) }.distinctBy { it.text }.sortedBy { it.text }
        val keywords = kotlinKeywords.filter { it.startsWith(prefix) }
            .map { CompletionItem(it, CompletionItem.Kind.KEYWORD) }
        return names + keywords
    }

    override fun inferType(expr: String): ResultWithDiagnostics<String> =
        ResultWithDiagnostics.Failure("Type inference is not available yet".asErrorDiagnostics())
}

class K2ReplIdeServices(
    private val compileProbe: (String) -> ResultWithDiagnostics<*>?,
    private val fallback: ReplIdeServices,
) : ReplIdeServices {

    override fun complete(code: String, cursor: Int): List<CompletionItem> {
        val beforeCursor = code.substring(0, cursor.coerceIn(0, code.length))
        val closing = closingBrackets(beforeCursor) ?: return emptyList()
        // A condition of `if`, `while` or `for` is only valid with a body after it
        val endings = if (closing.startsWith(")")) listOf(closing, ") {}" + closing.drop(1)) else listOf(closing)
        val candidates = endings.firstNotNullOfOrNull { ending ->
            probe {
                compileProbe(beforeCursor + COMPLETION_MARKER + ending)
                CompletionSink.collected(it)
            }
        } ?: return fallback.complete(code, cursor)
        val prefix = beforeCursor.takeLastWhile { Character.isJavaIdentifierPart(it) }
        val afterDot = beforeCursor.dropLast(prefix.length).trimEnd().endsWith(".")
        val items = candidates.map { CompletionItem(it.name, it.kind.toItemKind(), it.type, it.parameters) }.distinct().sortedBy { it.text }
        val keywords = if (afterDot) emptyList() else kotlinKeywords.filter { it.startsWith(prefix) }
            .map { CompletionItem(it, CompletionItem.Kind.KEYWORD) }
        return items + keywords
    }

    override fun inferType(expr: String): ResultWithDiagnostics<String> {
        var result: ResultWithDiagnostics<*>? = null
        val type = probe { result = compileProbe("val $TYPE_PROBE = (\n$expr\n)\n$PROBE_END"); CompletionSink.type(it) }
        if (type != null) return ResultWithDiagnostics.Success(type)
        val reports = result?.reports.orEmpty().filter { PROBE_END !in it.message }
        return ResultWithDiagnostics.Failure(reports.ifEmpty { listOf("Cannot infer the type of '$expr'".asErrorDiagnostics()) })
    }

    private fun <T> probe(body: (String) -> T): T {
        val requestId = UUID.randomUUID().toString()
        CompletionSink.begin(requestId)
        try {
            return body(requestId)
        } finally {
            CompletionSink.end(requestId)
        }
    }

    private fun CompletionCandidate.Kind.toItemKind(): CompletionItem.Kind = when (this) {
        CompletionCandidate.Kind.FUNCTION -> CompletionItem.Kind.FUNCTION
        CompletionCandidate.Kind.PROPERTY -> CompletionItem.Kind.PROPERTY
        CompletionCandidate.Kind.CLASS -> CompletionItem.Kind.CLASS
        CompletionCandidate.Kind.PACKAGE -> CompletionItem.Kind.PACKAGE
    }

    private companion object {
        // An unresolved name that fails the compilation of the `:type` probe on purpose, so the expression is never evaluated
        // and the compiler rolls its history back; completion probes fail on the unresolved marker itself
        const val PROBE_END = "__ki_probe_end__"
    }
}

// The brackets that close the ones left open in [code], or null if [code] ends inside a string or a comment
internal fun closingBrackets(code: String): String? {
    val stack = ArrayDeque<Char>()
    var i = 0
    fun at(text: String) = code.startsWith(text, i)
    while (i < code.length) {
        val inString = stack.lastOrNull()
        when {
            inString == '"' || inString == 'R' -> when {
                inString == '"' && code[i] == '\\' -> i++
                inString == '"' && code[i] == '"' -> stack.removeLast()
                inString == 'R' && at("\"\"\"") -> { stack.removeLast(); i += 2 }
                at("\${") -> { stack.addLast('$'); i++ }
            }
            at("//") -> {
                val end = code.indexOf('\n', i)
                if (end < 0) return null
                i = end
            }
            at("/*") -> {
                val end = code.indexOf("*/", i + 2)
                if (end < 0) return null
                i = end + 1
            }
            at("\"\"\"") -> { stack.addLast('R'); i += 2 }
            code[i] == '"' -> stack.addLast('"')
            code[i] == '\'' -> {
                val end = code.indexOf('\'', if (code.getOrNull(i + 1) == '\\') i + 3 else i + 2)
                if (end < 0) return null
                i = end
            }
            code[i] == '(' || code[i] == '[' || code[i] == '{' -> stack.addLast(code[i])
            code[i] == ')' || code[i] == ']' || code[i] == '}' -> if (stack.isNotEmpty()) stack.removeLast()
        }
        i++
    }
    if (stack.lastOrNull() == '"' || stack.lastOrNull() == 'R') return null
    return stack.reversed().joinToString("") {
        when (it) {
            '(' -> ")"
            '[' -> "]"
            '"' -> "\""
            'R' -> "\"\"\""
            else -> "}"
        }
    }
}

internal val kotlinKeywords: List<String> = listOf(
    "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
    "companion", "const", "constructor", "continue", "crossinline", "data", "do", "else",
    "enum", "expect", "external", "false", "final", "finally", "for", "fun",
    "get", "if", "import", "in", "infix", "init", "inline", "inner",
    "interface", "internal", "is", "lateinit", "noinline", "null", "object", "open",
    "operator", "out", "override", "package", "private", "protected", "public", "reified",
    "return", "sealed", "set", "super", "suspend", "tailrec", "this", "throw",
    "true", "try", "typealias", "val", "value", "var", "vararg", "when",
    "where", "while"
)
