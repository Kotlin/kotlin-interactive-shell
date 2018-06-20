package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.*
import com.github.khud.sparklin.kshell.configuration.Configuration
import sparklin.kshell.org.jline.reader.Highlighter
import sparklin.kshell.org.jline.reader.LineReader
import sparklin.kshell.org.jline.utils.AttributedString
import com.github.khud.kshell.repl.*
import com.github.khud.kshell.repl.ReplCompiler.Companion.RESULT_FIELD_NAME
import com.github.khud.kshell.repl.ReplCompiler.Companion.RUN_FIELD_NAME
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVariance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters

class RuntimePlugin : Plugin {
    inner class Imports(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "imports")
        override val short: String by conf.get(default = "i")

        override val description: String = "show imports"

        override fun execute(line: String) {
            repl.state.history
                    .filterIsInstance<ImportSnippet>()
                    .forEach { println(it.psi.text) }
        }
    }

    inner class InferType(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "type")
        override val short: String by conf.get(default = "t")
        override val description: String = "display the type of an expression without evaluating it"

        override val params = "<expr>"
        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val expr = line.substring(p + 1).trim()

            val compileResult = repl.compile(CodeExpr(counter.getAndIncrement(), expr))
            when (compileResult) {
                is Result.Error -> repl.handleError(compileResult.error)
                is Result.Success -> compileResult.data.classes.type?.let { println(it) }
            }
        }

        override fun highlighter(): Highlighter = customHighlighter
    }

    private class CustomHighlighter(val baseHighlighter: () -> BaseHighlighter): Highlighter {
        override fun highlight(reader: LineReader, buffer: String): AttributedString {
            val p = buffer.indexOf(' ')
            return baseHighlighter().highlight(buffer, p + 1)
        }
    }

    private data class CodeExpr(override val no: Int, override val code: String): SourceCode {
        override val part: Int = 0
        override fun mkFileName(): String = "TypeInference_$no"
        override fun nextPart(codePart: String): SourceCode = throw UnsupportedOperationException("Should never happen")
        override fun replace(code: String): CodeExpr = CodeExpr(no, code)
    }

    inner class ListSymbols(conf: Configuration) : com.github.khud.sparklin.kshell.BaseCommand() {
        override val name: String by conf.get(default = "list")
        override val short: String? by conf.get(default = "ls")
        override val description: String = "list defined symbols"

        override fun execute(line: String) {
            if (!table.isEmpty()) {
                println(table)
            }
        }
    }

    private lateinit var repl: KShell
    private var lastCompiledClasses: CompiledClasses? = null
    private lateinit var table: SymbolsTable
    private lateinit var customHighlighter: CustomHighlighter
    private val counter = AtomicInteger(0)

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl
        this.table = SymbolsTable(repl.state.history)


        repl.eventManager.registerEventHandler(OnCompile::class, object : EventHandler<OnCompile> {
            override fun handle(event: OnCompile) {
                lastCompiledClasses = event.data().classes
            }
        })

        repl.invokeWrapper = ExtractSymbols(repl.invokeWrapper, table)
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

class SymbolsTable(val history: List<Snippet>) {
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

    fun isShadowed(symbol: Symbol): Boolean = history
            .filterIsInstance<DeclarationSnippet>()
            .findLast { it.klass == symbol.namespace && it.name == symbol.name }
            ?.shadowed ?: false
}

class ExtractSymbols(private val wrapper: InvokeWrapper?, private val table: SymbolsTable): InvokeWrapper {
    override fun <T> invoke(body: () -> T): T {
        val r = wrapper?.invoke(body) ?: body()

        // cast carefully
        val embodiment = r as Any
        val namespace = embodiment::class.simpleName!!

        embodiment::class.nestedClasses.forEach { clazz ->
            clazz.simpleName?.let {
                table.add(ClassSymbol(namespace, it, clazz))
            }
        }

        embodiment::class.members.filter { callable -> val cname = callable.toString()
            cname.contains(" Line_")}.forEach {
            when (it) {
                is KProperty1<*, *> -> {
                    val obj = readProperty(embodiment, it.name)
                    if (it.name != RESULT_FIELD_NAME && it.name != RUN_FIELD_NAME)
                        table.add(InstanceSymbol(namespace, it.name, obj, it))
                }
                is KFunction<*> -> {
                    table.add(FunctionSymbol(namespace, it.name, it))
                }
                else -> throw IllegalStateException("Unknown symbol: $it of ${it::class}")
            }
        }
        return r
    }

    private fun readProperty(instance: Any, propertyName: String): Any? {
        val klass = instance.javaClass.kotlin
        return klass.declaredMemberProperties.first { it.name == propertyName }.get(instance)
    }
}