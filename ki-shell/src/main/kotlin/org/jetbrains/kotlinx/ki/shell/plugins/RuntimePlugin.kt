package org.jetbrains.kotlinx.ki.shell.plugins

import org.jetbrains.kotlinx.ki.shell.*
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import java.util.regex.Pattern
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet

class RuntimePlugin : Plugin {
    inner class InferType(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "type")
        override val short: String by conf.get(default = "t")
        override val description: String = "display the type of an expression without evaluating it"

        override val params = "<expr>"
        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            val expr = line.substring(p + 1).trim()

            // TODO: restore
            when (val analysisResults = repl.analyze(expr, SourceCode.Position(0, 0))) {
                is ResultWithDiagnostics.Failure -> repl.handleError(analysisResults, false)
                is ResultWithDiagnostics.Success<ReplAnalyzerResult> -> {
                    analysisResults.value[ReplAnalyzerResult.renderedResultType]?.let { println(it) }
                }
            }
            return Command.Result.Success()
        }

        override fun highlighter(): Highlighter = customHighlighter
    }

    private class CustomHighlighter(val baseHighlighter: () -> BaseHighlighter): Highlighter {
        override fun highlight(reader: LineReader, buffer: String): AttributedString {
            val p = buffer.indexOf(' ')
            return baseHighlighter().highlight(buffer, p + 1)
        }

        override fun setErrorPattern(p0: Pattern?) {
            TODO("Not yet implemented")
        }

        override fun setErrorIndex(p0: Int) {
            TODO("Not yet implemented")
        }
    }

//    private data class CodeExpr(override val no: Int, override val code: String): SourceCode {
//        override val part: Int = 0
//        override fun mkFileName(): String = "TypeInference_$no"
//        override fun nextPart(codePart: String): SourceCode = throw UnsupportedOperationException("Should never happen")
//        override fun replace(code: String): CodeExpr = CodeExpr(no, code)
//    }

    inner class ListSymbols(conf: ReplConfiguration) : BaseCommand() {
        override val name: String by conf.get(default = "list")
        override val short: String? by conf.get(default = "ls")
        override val description: String = "list defined symbols"

        override fun execute(line: String): Command.Result {
            if (!table.isEmpty()) {
                val p = line.indexOf(' ')
                val pattern = if (p >= 0)line.substring(p + 1).trim() else null
                println(table.list(pattern).joinToString("\n"))
            }
            return Command.Result.Success()
        }
    }

    private lateinit var repl: Shell
    private lateinit var table: SymbolsTable
    private lateinit var customHighlighter: CustomHighlighter

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl
        this.table = SymbolsTable()


        repl.eventManager.registerEventHandler(OnEval::class, object : EventHandler<OnEval> {
            override fun handle(event: OnEval) {
                val result = event.data().get().result
                if (result is ResultValue.Value || result is ResultValue.Unit) {
                    table.addNewSnippets(event.data())
                }
            }
        })


        customHighlighter = CustomHighlighter { repl.highlighter.syntaxHighlighter }

        repl.registerCommand(InferType(config))
        repl.registerCommand(ListSymbols(config))
        // TODO: restore the functionality
//        repl.registerCommand(Imports(config))
    }

    override fun cleanUp() { }
}

enum class SymbolKind {
    CLASS,
    INSTANCE,
    FUNCTION
}

sealed class Symbol(val name: String, val kind: SymbolKind, val namespace: String, val extensionReceiver: String?) {
    abstract fun show(): String
}

class ClassSymbol(name: String, private val clazz: KClass<*>, namespace: String): Symbol(name, SymbolKind.CLASS, namespace, null) {
    override fun show(): String {
        val constructor = clazz.primaryConstructor?.render(true) ?: ""
        val data = if (clazz.isData) "data " else ""
        return "${data}class $name$constructor"
    }
}

class InstanceSymbol(name: String, private val prop: KProperty<*>, namespace: String, extensionReceiver: String?): Symbol(name, SymbolKind.INSTANCE, namespace, extensionReceiver) {
    override fun show(): String {
        val def = if (prop.toString().startsWith("val")) "val" else "var"
        return "$def ${prop.renderReceiver()}$name: ${prop.renderReturnType()}"
    }
}

class FunctionSymbol(name: String, private val func: KFunction<*>, namespace: String, extensionReceiver: String?): Symbol(name, SymbolKind.FUNCTION, namespace, extensionReceiver) {
    override fun show(): String  = func.render()
}

class SymbolsTable {
    private val symbols = mutableListOf<Symbol>()

    fun add(symbol: Symbol) {
        symbols.removeIf { it.kind == symbol.kind && it.name == symbol.name && it.extensionReceiver == symbol.extensionReceiver }
        symbols.add(symbol)
    }

    fun isEmpty() = symbols.isEmpty()

    fun list(
            pattern: String? = null,
            kinds: List<SymbolKind> = listOf(SymbolKind.INSTANCE, SymbolKind.FUNCTION, SymbolKind.CLASS)
    ): List<String> {
        val regex = pattern?.takeIf { it.isNotBlank() }?.let { Regex(it) }
        return symbols.filter { symbol ->
            kinds.contains(symbol.kind)
                    && (
                        regex == null
                                || symbol.name.matches(regex)
                                || symbol.namespace.matches(regex)
                                || symbol.extensionReceiver?.matches(regex) == true
                    )
        }.map {
            it.show()
        }
    }

    fun addNewSnippets(snippets: LinkedSnippet<KJvmEvaluatedSnippet>) {
        val r = snippets.get().result.scriptInstance
        val resultFieldName = snippets.get().compiledSnippet.compilationConfiguration[ScriptCompilationConfiguration.resultField]
        // cast carefully
        val embodiment = r as Any
        val namespace = embodiment::class.simpleName!!

        embodiment::class.nestedClasses.forEach { clazz ->
            clazz.simpleName?.let {
                add(ClassSymbol(it, clazz, namespace))
            }
        }

        embodiment::class.declaredMembers.filter { callable ->
            val cname = callable.toString()
            cname.contains(" Line_")
        }.forEach {
            val extReceiver = it.renderReceiver(withDot = false)
            when (it) {
                is KProperty<*> -> {
                    if (it.name != resultFieldName) // special names
                        add(InstanceSymbol(it.name, it, namespace, extReceiver))
                }
                is KFunction<*> -> {
                    add(FunctionSymbol(it.name, it, namespace, extReceiver))
                }
                else -> throw IllegalStateException("Unknown symbol: $it of ${it::class}")
            }
        }
    }
}

private fun KCallable<*>.getExtensionReceiver() =
        parameters.find { it.kind == KParameter.Kind.EXTENSION_RECEIVER }?.type

private fun KCallable<*>.renderReceiver(withDot: Boolean = true): String =
        getExtensionReceiver()?.let {
            val typeName = renderKotlinType(it.toString())
            if (withDot) "$typeName."
            else typeName
        } ?: ""

private fun KCallable<*>.renderReturnType(): String = renderKotlinType(returnType.toString())

private fun KFunction<*>.render(isConstructor: Boolean = false): String {
    val tp = if (typeParameters.isNotEmpty()) "<" + typeParameters.joinToString(separator = ",") {
        (if (it.variance != KVariance.INVARIANT) "${it.variance} ${it.name}" else it.name) +
                (if (it.upperBounds.isNotEmpty()) ": ${it.upperBounds.joinToString(separator = ",")}" else "")
    } + "> " else ""

    val vp = valueParameters.joinToString(separator = ", ") {
        it.name + ": " + renderKotlinType(it.type.toString())
    }

    return if (isConstructor) "${tp.trim()}($vp)" else "fun $tp${renderReceiver()}${name}($vp): ${renderReturnType()}"
}
