package sparklin.kshell.plugins

import sparklin.kshell.configuration.Configuration
import sparklin.kshell.console.ConsoleReader
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import sparklin.kshell.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class RuntimePlugin : Plugin {
    inner class InferType(fullName: String, shortName: String, description: String):
        BaseCommand(fullName, shortName, description) {

        override val params = "<expr>"

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val expr = line.substring(p + 1).trim()

            repl.extensionPoint {
                val compileResult = repl.compile(expr)
                when (compileResult) {
                    is ReplCompileResult.Incomplete ->
                        console.println("Incomplete line")
                    is ReplCompileResult.Error ->
                        repl.compileError(compileResult)
                    is ReplCompileResult.CompiledClasses -> {
                        console.println(clarifyType(compileResult.type)!!)
                    }
                }
            }
        }
    }

    inner class PrintSymbols(fullName: String, shortName: String, description: String):
            sparklin.kshell.BaseCommand(fullName, shortName, description) {
        private val table = SymbolsTable()

        init {
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

    private lateinit var repl: Repl
    private lateinit var console: ConsoleReader
    private var lastCompiledClasses: ReplCompileResult.CompiledClasses? = null

    override fun init(repl: Repl, config: Configuration) {
        this.repl = repl
        this.console = config.getConsoleReader()

        val inferTypeCmdFullName = config.getLocal("inferTypeCmd", "name", "type")
        val inferTypeCmdShortName = config.getLocal("inferTypeCmd", "short", "t")
        val inferTypeCmdDescription = "display the type of an expression without evaluating it"

        val printSymbolsCmdFullName = config.getLocal("printSymbolsCmd", "name", "symbols")
        val printSymbolsCmdShortName = config.getLocal("printSymbolsCmd", "short", "s")
        val printSymbolsCmdDescription = "list defined symbols"

        EventManager.registerEventHandler(OnCompile::class, object : EventHandler<OnCompile> {
            override fun handle(event: OnCompile) {
                lastCompiledClasses = event.data()
            }
        })

        repl.registerCommand(InferType(inferTypeCmdFullName, inferTypeCmdShortName, inferTypeCmdDescription))
        repl.registerCommand(PrintSymbols(printSymbolsCmdFullName, printSymbolsCmdShortName, printSymbolsCmdDescription))
    }

    private fun Configuration.getLocal(cmd: String, key: String, default: String) =
            this.get("${LoadFilePlugin::class.qualifiedName!!}.$cmd.$key", default)

    override fun cleanUp() { }
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
    private val dict = HashMap<Pair<String, SymbolKind>, Symbol>()

    fun add(symbol: Symbol) {
        dict[Pair(symbol.name, symbol.kind)] = symbol
    }

    override fun toString(): String = list().joinToString(separator = "\n")

    fun list(pattern: String? = null, kinds: List<SymbolKind> =
        listOf(SymbolKind.INSTANCE, SymbolKind.FUNCTION, SymbolKind.CLASS),
             verbose: VerboseLevel = VerboseLevel.MEDIUM): List<String> {
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
        val klass = instance.javaClass.kotlin
        return klass.declaredMemberProperties.first { it.name == propertyName }.get(instance)
    }
}