package kshell

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import lib.jline.console.ConsoleReader
import lib.jline.console.completer.FileNameCompleter
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericRepl
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.utils.PathUtil
import java.io.Closeable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

val EMPTY_SCRIPT_ARGS: Array<out Any?> = arrayOf(emptyArray<String>())
val EMPTY_SCRIPT_ARGS_TYPES: Array<out KClass<out Any>> = arrayOf(Array<String>::class)

/**
 * Basic class for Kotlin powered REPL.
 * Contains some stuff from https://github.com/kohesive/keplin
 */
open class KShell protected constructor(protected val disposable: Disposable,
                                        protected val scriptDefinition: KotlinScriptDefinition,
                                        protected val compilerConfiguration: CompilerConfiguration,
                                        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                                        protected val sharedHostClassLoader: ClassLoader? = null,
                                        protected val emptyArgsProvider: ScriptTemplateEmptyArgsProvider,
                                        protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : Closeable {

    constructor(disposable: Disposable = Disposer.newDisposable(),
                moduleName: String = "kotlin-script-module-${System.currentTimeMillis()}",
                additionalClasspath: List<File> = emptyList(),
                scriptDefinition: KotlinScriptDefinitionEx = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES)),
                messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                sharedHostClassLoader: ClassLoader? = null) : this(disposable,
            compilerConfiguration = CompilerConfiguration().apply {
                addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
                // do not include stdlib and runtime implicitly
                addJvmClasspathRoots(findRequiredScriptingJarFiles(scriptDefinition.template,
                        includeScriptEngine = false,
                        includeKotlinCompiler = false,
                        includeStdLib = false,
                        includeRuntime = false))
                addJvmClasspathRoots(additionalClasspath)
                put(CommonConfigurationKeys.MODULE_NAME, moduleName)
                put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            },
            repeatingMode = repeatingMode,
            sharedHostClassLoader = sharedHostClassLoader,
            scriptDefinition = scriptDefinition,
            emptyArgsProvider = scriptDefinition)

    var fallbackArgs: ScriptArgsWithTypes? = emptyArgsProvider.defaultEmptyArgs
        get() = stateLock.read { field }
        set(value) = stateLock.write { field = value }

    private val baseClassloader = URLClassLoader(compilerConfiguration.jvmClasspathRoots.map { it.toURI().toURL() }
            .toTypedArray(), sharedHostClassLoader)

    val engine: GenericRepl by lazy {
        object : GenericRepl(disposable = disposable,
                scriptDefinition = scriptDefinition,
                compilerConfiguration = compilerConfiguration,
                messageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                baseClassloader = baseClassloader,
                fallbackScriptArgs = fallbackArgs,
                repeatingMode = repeatingMode,
                stateLock = stateLock) {}
    }

    val incompleteLines = arrayListOf<String>()
    var lineNumber = 1
    var resultCounter = 0
    val reader = ConsoleReader()
    val commands: MutableList<Command> = mutableListOf(FakeQuit())

    private class FakeQuit: Command("quit", "q", "exit the interpreter") {
        override fun execute(line: String) {}
    }

    fun doRun() {
        reader.addCompleter(ContextDependentCompleter(commands))

        do {
            printPrompt()
            val line = reader.readLine()

            // handle :quit specially
            if (line == null || isQuitCommand(line)) break

            if (incompleteLines.isEmpty() && line.startsWith(":")) {
                try {
                    val command = commands.first { cmd -> cmd.match(line) }
                    command.execute(line)
                } catch (e: NoSuchElementException) {
                    reader.println("Unknown command $line")
                } catch (e: Exception) {
                    commandError(e)
                }
            } else {
                compileAndEval(line)
            }
        } while (true)
    }

    fun registerCommand(command: Command): Unit {
        command.repl = this
        commands.add(command)
    }

    private fun isQuitCommand(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    fun compileAndEval(line: String): Unit {
        val source = (incompleteLines + line).joinToString(separator = "\n")
        val replCodeLine = ReplCodeLine(lineNumber, source)
        val compileResult = engine.compile(replCodeLine)

        when (compileResult) {
            is ReplCompileResult.Incomplete -> {
                incompleteLines.add(line)
            }

            is ReplCompileResult.CompiledClasses -> {
                afterCompile(compileResult)
                val evalResult = engine.eval(compileResult)

                when (evalResult) {
                    is ReplEvalResult.ValueResult -> {
                        valueResult(evalResult)
                        lineNumber ++
                    }

                    is ReplEvalResult.UnitResult -> {
                        lineNumber ++
                    }

                    else -> evalError(evalResult)
                }
                incompleteLines.clear()
            }

            is ReplCompileResult.Error -> {
                engine.resetToLine(lineNumber)
                compileError(compileResult)
                incompleteLines.clear()
            }
        }
    }

    open fun afterCompile(compiledClasses: ReplCompileResult.CompiledClasses) {
       // do nothing by default
    }

    fun printPrompt() {
        if (incompleteLines.isEmpty())
            reader.prompt = "kotlin> "
        else
            reader.prompt = "... "
    }

    open fun valueResult(result: ReplEvalResult.ValueResult) {
        lineNumber ++

        val name = "res$resultCounter"
        val clazz = result.value!!::class.qualifiedName

        // store result of computations
        Shared.__res = result.value
        compileAndEval("val $name: $clazz = __res as $clazz")

        reader.println("$name: $clazz = ${result.value}")
        resultCounter ++
    }


    open fun evalError(result: ReplEvalResult) {
        reader.println(result.toString())
    }

    open fun compileError(result: ReplCompileResult.Error) {
        reader.println("Message: ${result.message} Location: ${result.location}")
    }


    open fun commandError(e: Exception) {
        e.printStackTrace()
    }

    override fun close() {
        disposable.dispose()
    }
}
