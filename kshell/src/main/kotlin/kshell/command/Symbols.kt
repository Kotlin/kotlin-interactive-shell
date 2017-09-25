package kshell.command

import kshell.Command
import kshell.KShell
import kshell.getInstrumentation
import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class Symbols : Command("symbols", "s", "list defined symbols", params = "<options>") {
    val table = SymbolsTable()

    override fun init(repl: KShell) {
        super.init(repl)
        repl.wrapper = ExtractSymbols(repl.wrapper, table)
    }

    override fun execute(line: String) {
        repl.apply {
            lastCompiledClasses?.let {
                println(table)
            }
        }
    }
}

sealed class Symbol(val name: String,  val kind: SymbolKind) {
    abstract fun show(verbose: VerboseLevel): String
}

class ClassSymbol(name: String, private val clazz: KClass<*>): Symbol(name, SymbolKind.CLASS) {
    override fun show(verbose: VerboseLevel): String =
            when (verbose) {
                VerboseLevel.LOW -> name
                VerboseLevel.MEDIUM, VerboseLevel.HIGH -> "class $name"
            }
}

class InstanceSymbol(name: String, private val obj: Any?, private val prop: KProperty1<*, *>): Symbol(name, SymbolKind.INSTANCE) {
    override fun show(verbose: VerboseLevel): String =
            when (verbose) {
                VerboseLevel.LOW -> name
                VerboseLevel.MEDIUM -> signature()
                VerboseLevel.HIGH -> {
                    val instrumentation = getInstrumentation()
                    if (instrumentation != null) {
                        if (obj != null) {
                            val size = human(instrumentation.getObjectSize(obj))
                            "${signature()} ($size)"
                        } else {
                            "${signature()} (null)"
                        }
                    } else signature()
                }
            }

    private fun human(size: Long): String = size.toString()

    private fun signature(): String {
        val def = if (prop.toString().startsWith("val")) "val" else "var"
        val type = prop.returnType
        return "$def $name: $type"
    }
}

class FunctionSymbol(name: String, private val func: KFunction<*>): Symbol(name, SymbolKind.FUNCTION) {
    override fun show(verbose: VerboseLevel): String =
            when(verbose) {
                VerboseLevel.LOW -> name
                VerboseLevel.MEDIUM -> {
                    val type = func.returnType
                    "fun $name(...): $type"
                }
                VerboseLevel.HIGH -> signature()
            }

    private fun signature(): String {
        return "signature"
    }
}

enum class VerboseLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class SymbolKind {
    CLASS,
    INSTANCE,
    FUNCTION
}

class SymbolsTable {

    val dict = HashMap<Pair<String, SymbolKind>, Symbol>()

    fun add(symbol: Symbol) {
        dict.put(Pair(symbol.name, symbol.kind), symbol)
    }

    override fun toString(): String = list().joinToString(separator = "\n")

    fun list(pattern: String? = null, kinds: List<SymbolKind> = listOf(SymbolKind.INSTANCE, SymbolKind.FUNCTION, SymbolKind.CLASS), verbose: VerboseLevel = VerboseLevel.MEDIUM): List<String> {
        val regex = pattern?.let { Regex(it) }
        return dict.values.filter { kinds.contains(it.kind) &&
                (regex == null || it.name.matches(regex)) }.map {
            it.show(verbose)
        }
    }
}

class ExtractSymbols(private val wrapper: InvokeWrapper, private val table: SymbolsTable): InvokeWrapper {
    override fun <T> invoke(body: () -> T): T {
        val r = wrapper.invoke(body)
        // cast carefully
        val scriptTemplate = r as ScriptTemplateWithArgs

        scriptTemplate::class.nestedClasses.forEach { clazz ->
            clazz.simpleName?.let {
                table.add(ClassSymbol(it, clazz))
            }
        }

        scriptTemplate::class.members.filter { callable -> callable.toString().contains(" Line_") }.forEach {
            when (it) {
                is KProperty1<*, *> -> {
                    val obj = readProperty(scriptTemplate, it.name)
                    table.add(InstanceSymbol(it.name, obj, it))
                }
                is KFunction<*> -> {
                    table.add(FunctionSymbol(it.name, it))
                }
                else -> throw IllegalStateException("Unknown symbol: $it of ${it::class}")
            }
        }
        return r
    }

    private fun readProperty(instance: Any, propertyName: String): Any? {
        val clazz = instance.javaClass.kotlin
        return clazz.declaredMemberProperties.first { it.name == propertyName }.get(instance)
    }
}

