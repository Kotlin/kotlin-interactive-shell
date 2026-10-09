package org.jetbrains.kotlinx.ki.shell

import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asErrorDiagnostics

interface ReplIdeServices {
    fun complete(code: String, cursor: Int): List<CompletionItem>
    fun inferType(expr: String): ResultWithDiagnostics<String>
}

data class CompletionItem(val text: String, val kind: Kind, val tail: String?) {
    enum class Kind { FUNCTION, PROPERTY, CLASS, KEYWORD, PACKAGE }
}

class BasicReplIdeServices(private val declarations: () -> Iterable<CompletionItem>) : ReplIdeServices {

    override fun complete(code: String, cursor: Int): List<CompletionItem> {
        val beforeCursor = code.substring(0, cursor.coerceIn(0, code.length))
        val prefix = beforeCursor.takeLastWhile { Character.isJavaIdentifierPart(it) }
        if (beforeCursor.dropLast(prefix.length).endsWith(".")) return emptyList()
        val names = declarations().filter { it.text.startsWith(prefix) }.distinctBy { it.text }.sortedBy { it.text }
        val keywords = kotlinKeywords.filter { it.startsWith(prefix) }
            .map { CompletionItem(it, CompletionItem.Kind.KEYWORD, null) }
        return names + keywords
    }

    override fun inferType(expr: String): ResultWithDiagnostics<String> =
        ResultWithDiagnostics.Failure("Type inference is not available yet".asErrorDiagnostics())
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
