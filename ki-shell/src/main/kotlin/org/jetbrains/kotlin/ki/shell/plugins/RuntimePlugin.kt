package org.jetbrains.kotlin.ki.shell.plugins

import org.jetbrains.kotlin.ki.shell.*
import org.jetbrains.kotlin.ki.shell.configuration.ReplConfiguration
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVariance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaField
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet

class RuntimePlugin : Plugin {
    inner class Imports(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "imports")
        override val short: String by conf.get(default = "i")

        override val description: String = "show imports"

        override fun execute(line: String) {
            // TODO: restore
//            repl.state.history
//                    .filterIsInstance<ImportSnippet>()
//                    .forEach { println(it.psi.text) }
            println("!not implemented!")
        }
    }

    inner class InferType(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "type")
        override val short: String by conf.get(default = "t")
        override val description: String = "display the type of an expression without evaluating it"

        override val params = "<expr>"
        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val expr = line.substring(p + 1).trim()

            // TODO: restore
            val analysisResults = repl.analyze(expr, SourceCode.Position(0, 0))
            when (analysisResults) {
                is ResultWithDiagnostics.Failure -> repl.handleError(analysisResults)
                is ResultWithDiagnostics.Success<ReplAnalyzerResult> -> {
                    analysisResults.value[ReplAnalyzerResult.renderedResultType]?.let { println(it) }
                }
            }
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

        override fun execute(line: String) {
            if (!table.isEmpty()) {
                println(table)
            }
        }
    }

    private lateinit var repl: Shell
    private lateinit var table: SymbolsTable
    private lateinit var customHighlighter: CustomHighlighter
    private val counter = AtomicInteger(0)

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl
        this.table = SymbolsTable()


        repl.eventManager.registerEventHandler(OnEval::class, object : EventHandler<OnEval> {
            override fun handle(event: OnEval) {
                table.addNewSnippets(event.data())
            }
        })


        customHighlighter = CustomHighlighter({ repl.highlighter.syntaxHighlighter })

        repl.registerCommand(InferType(config))
        repl.registerCommand(ListSymbols(config))
        repl.registerCommand(Imports(config))
    }

    override fun cleanUp() { }
}

enum class SymbolKind {
    CLASS,
    INSTANCE,
    FUNCTION
}

sealed class Symbol(val namespace: String, val name: String, val kind: SymbolKind) {
    abstract fun show(): String
}

class ClassSymbol(namespace: String, name: String, private val clazz: KClass<*>): Symbol(namespace, name, SymbolKind.CLASS) {
    override fun show(): String {
        val constructor = clazz.primaryConstructor?.let { FunctionSymbol.show(it, true) } ?: ""
        val data = if (clazz.isData) "data " else ""
        return "${data}class $name$constructor"
    }
}

class InstanceSymbol(namespace: String, name: String, private val obj: Any?, private val prop: KProperty1<*, *>): Symbol(namespace, name, SymbolKind.INSTANCE) {
    override fun show(): String {
        val def = if (prop.toString().startsWith("val")) "val" else "var"
        val type = prop.returnType
        return "$def $name: $type"
    }
}

class FunctionSymbol(namespace: String, name: String, private val func: KFunction<*>): Symbol(namespace, name, SymbolKind.FUNCTION) {
    override fun show(): String  = Companion.show(func)

    companion object {
        fun show(func: KFunction<*>, isConstructor: Boolean = false): String {
            val tp = if (func.typeParameters.isNotEmpty()) "<" + func.typeParameters.joinToString(separator = ",") {
                (if (it.variance != KVariance.INVARIANT) "${it.variance} ${it.name}" else it.name) +
                        (if (it.upperBounds.isNotEmpty()) ": ${it.upperBounds.joinToString(separator = ",")}" else "")
            } + "> " else ""

            val vp = func.valueParameters.joinToString(separator = ", ") {
                it.name + ": " + it.type
            }

            return if (isConstructor) "${tp.trim()}($vp)" else "fun $tp${func.name}($vp): ${func.returnType}"
        }
    }
}

class SymbolsTable() {
    private val symbols = mutableListOf<Symbol>()

    fun add(symbol: Symbol) {
        symbols.add(symbol)
    }

    fun isEmpty() = symbols.isEmpty()

    override fun toString(): String = list().joinToString(separator = "\n")

    fun list(pattern: String? = null, kinds: List<SymbolKind> =
            listOf(SymbolKind.INSTANCE, SymbolKind.FUNCTION, SymbolKind.CLASS)): List<String> {
        val regex = pattern?.let { Regex(it) }
        return symbols.filter { kinds.contains(it.kind) && !isShadowed(it) &&
                (regex == null || it.name.matches(regex)) }.map {
            it.show()
        }
    }

    fun isShadowed(symbol: Symbol): Boolean = false
    // TODO: restore functionality
//            history
//            .filterIsInstance<DeclarationSnippet>()
//            .findLast { it.klass == symbol.namespace && it.name == symbol.name }
//            ?.shadowed ?: false

    fun addNewSnippets(snippets: LinkedSnippet<KJvmEvaluatedSnippet>): Unit {
        val r = snippets.get().result.scriptInstance
        val resultFieldName = snippets.get().compiledSnippet.compilationConfiguration[ScriptCompilationConfiguration.resultField]
        // cast carefully
        val embodiment = r as Any
        val namespace = embodiment::class.simpleName!!

        embodiment::class.nestedClasses.forEach { clazz ->
            clazz.simpleName?.let {
                add(ClassSymbol(namespace, it, clazz))
            }
        }

        embodiment::class.declaredMembers.filter { callable -> val cname = callable.toString()
            cname.contains(" Line_")}.forEach {
            when (it) {
                is KProperty1<*, *> -> {
                    val obj = readProperty(embodiment, it.name)
                    if (it.name != resultFieldName) // special names
                        add(InstanceSymbol(namespace, it.name, obj, it))
                }
                is KFunction<*> -> {
                    add(FunctionSymbol(namespace, it.name, it))
                }
                else -> throw IllegalStateException("Unknown symbol: $it of ${it::class}")
            }
        }
    }

    private fun readProperty(instance: Any, propertyName: String): Any? {
        val klass = instance.javaClass.kotlin
        return klass.declaredMemberProperties.first { it.name == propertyName }.apply {
            if (isConst) javaField!!.get(null) else get(instance)
        }
    }
}
