package kshell

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import javafx.stage.Stage
import kshell.configuration.CachedInstance
import kshell.configuration.Configuration
import kshell.configuration.ConfigurationImpl
import kshell.console.Completer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompiler
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.utils.PathUtil
import java.io.Closeable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicInteger
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
open class KShell protected constructor(val disposable: Disposable,
                                        val scriptDefinition: KotlinScriptDefinitionEx,
                                        val compilerConfiguration: CompilerConfiguration,
                                        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                                        protected val sharedHostClassLoader: ClassLoader? = null,
                                        protected val emptyArgsProvider: ScriptTemplateEmptyArgsProvider,
                                        protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : Closeable, Repl {

    constructor(disposable: Disposable = Disposer.newDisposable(),
                moduleName: String = "kotlin-script-module-${System.currentTimeMillis()}",
                additionalClasspath: List<File> = emptyList(),
                scriptDefinition: KotlinScriptDefinitionEx = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES)),
                messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                sharedHostClassLoader: ClassLoader? = null) : this(disposable,
            compilerConfiguration = CompilerConfiguration().apply {
                addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
                addJvmClasspathRoots(findRequiredScriptingJarFiles(scriptDefinition.template,
                        includeScriptEngine = false,
                        includeKotlinCompiler = false,
                        includeStdLib = true,
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

    open class GenericRepl protected constructor(
            disposable: Disposable,
            scriptDefinition: KotlinScriptDefinition,
            compilerConfiguration: CompilerConfiguration,
            messageCollector: MessageCollector,
            baseClassloader: ClassLoader?,
            protected val fallbackScriptArgs: ScriptArgsWithTypes? = null,
            protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE
    ) : ReplCompiler, ReplEvaluator, ReplAtomicEvaluator {

        val compiler: GenericReplCompiler by lazy { GenericReplCompiler(disposable, scriptDefinition, compilerConfiguration, messageCollector) }
        val evaluator: ReplFullEvaluator by lazy { GenericReplCompilingEvaluator(compiler, compilerConfiguration.jvmClasspathRoots, baseClassloader, fallbackScriptArgs, repeatingMode) }

        override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = evaluator.createState(lock)

        override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = compiler.check(state, codeLine)

        override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult = compiler.compile(state, codeLine)

        override fun eval(state: IReplStageState<*>, compileResult: ReplCompileResult.CompiledClasses, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult =
                evaluator.eval(state, compileResult, scriptArgs, invokeWrapper)

        override fun compileAndEval(state: IReplStageState<*>, codeLine: ReplCodeLine, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult =
                evaluator.compileAndEval(state, codeLine, scriptArgs, invokeWrapper)
    }

    val engine: GenericRepl by lazy {
        object: GenericRepl(disposable = disposable,
                scriptDefinition = scriptDefinition,
                compilerConfiguration = compilerConfiguration,
                messageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                baseClassloader = baseClassloader,
                fallbackScriptArgs = fallbackArgs,
                repeatingMode = repeatingMode) {}
    }

    val state = engine.createState(stateLock)

    val incompleteLines = arrayListOf<String>()
    val nextLine: AtomicInteger = AtomicInteger(REPL_CODE_LINE_FIRST_NO)
    val resultCounter: AtomicInteger = AtomicInteger(1)

    val reader = configuration().getConsoleReader()

    val commands = mutableListOf<Command>(FakeQuit())

    private var lastCompiledClasses: ReplCompileResult.CompiledClasses? = null

    override fun getLastCompiledClasses(): ReplCompileResult.CompiledClasses?  = lastCompiledClasses

    private class FakeQuit: BaseCommand("quit", "q", "exit the interpreter") {
        override fun execute(line: String) {}
    }

    override var wrapper: InvokeWrapper = object: InvokeWrapper {
        override fun <T> invoke(body: () -> T): T {
            return body()
        }
    }

    open fun buildDefaultCompleter() = Completer.DEFAULT_COMPLETER

//    fun getReplEnvironment() = engine.compiler.getReplEnvironment()

    companion object {
        private val instance = CachedInstance<Configuration>()

        fun configuration(): Configuration {
            val klassName: String? = System.getProperty("config.class")

            return if (klassName != null) {
                instance.load(klassName, Configuration::class)
            } else {
                instance.get { ConfigurationImpl() }
            }
        }
    }

    override fun listCommands(): Iterable<Command> = commands.asIterable()

    fun doRun() {
        reader.apply {
            addCompleter(ContextDependentCompleter(commands, incompleteLines::isEmpty, buildDefaultCompleter()))
        }

        val config = configuration()
        config.load()

        config.plugins().forEach { it.init(this, config) }

        initImports()
        do {
            printPrompt()
            val line = reader.readLine()

            if (line == null || isQuitAction(line)) break

            if (incompleteLines.isEmpty() && line.startsWith(":")) {
                try {
                    val action = commands.first { it.match(line) }
                    action.execute(line)
                } catch (_: NoSuchElementException) {
                    reader.println("Unknown command $line")
                } catch (e: Exception) {
                    commandError(e)
                }
            } else {
                if (line.isBlank() && (incompleteLines.isNotEmpty() && incompleteLines.last().isBlank())) {
                    incompleteLines.clear()
                    reader.println("You typed two blank lines. Starting a new command.")
                } else {
                    compileAndEval(line)
                }
            }
        } while (true)

        cleanUp()
    }

    fun initImports() {
        scriptDefinition.defaultImports.forEach { compileAndEval("import $it") }
    }

    override fun registerCommand(command: Command) {
        commands.add(command)
    }

    override fun extensionPoint(body: () -> Unit) {
        state.lock.write {
            body()
            alterState(state)
        }
    }

    private fun isQuitAction(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    override fun compile(code: String): ReplCompileResult {
        val replCodeLine = ReplCodeLine(nextLine.incrementAndGet(), state.currentGeneration, code)
        return engine.compile(state, replCodeLine)
    }


    override fun compileAndEval(line: String) {
        state.lock.write {
            val source = (incompleteLines + line).joinToString(separator = "\n")
            val replCodeLine = ReplCodeLine(nextLine.incrementAndGet(), state.currentGeneration, source)
            val compileResult = engine.compile(state, replCodeLine)

            when (compileResult) {
                is ReplCompileResult.Incomplete -> {
                    incompleteLines.add(line)
                }
                is ReplCompileResult.Error -> {
                    compileError(compileResult)
                    alterState(state)
                    incompleteLines.clear()
                }
                is ReplCompileResult.CompiledClasses -> {
                    afterCompile(compileResult)
                    val evalResult = engine.eval(state, compileResult, fallbackArgs, wrapper)
                    incompleteLines.clear()
                    when (evalResult) {
                        is ReplEvalResult.Incomplete -> throw IllegalStateException("Should never happen")
                        is ReplEvalResult.ValueResult -> valueResult(evalResult)
                        is ReplEvalResult.UnitResult -> { lastCompiledClasses = compileResult }
                        is ReplEvalResult.Error,
                        is ReplEvalResult.HistoryMismatch -> evalError(evalResult)
                    }
                }
            }
        }
    }

    private fun alterState(state: IReplStageState<*>) {
        val aggregatedState = state.asState(AggregatedReplStageState::class.java)
        aggregatedState.apply {
            lock.write {
                if (state1.history.size > state2.history.size) {
                    if (state2.history.size == 0) {
                        state1.history.reset()
                    }
                    else {
                        state2.history.peek()?.let {
                            state1.history.resetTo(it.id)
                        }
                    }
                    assert(state1.history.size == state2.history.size)
                }
            }
        }
    }

    open fun afterCompile(compiledClasses: ReplCompileResult.CompiledClasses) {
        // do nothing
    }

    fun printPrompt() {
        if (incompleteLines.isEmpty())
            reader.setPrompt("kotlin> ")
        else
            reader.setPrompt("... ")
    }

    open fun valueResult(result: ReplEvalResult.ValueResult) {
        val name = "res${resultCounter.getAndIncrement()}"

        // store result of computations
        Shared.__res = result.value

        val type = clarifyType(result.type)
        compileAndEval("val $name = __res as $type")
        reader.println("$name: $type = ${result.value}")
    }

    /**
     * @see org.jetbrains.kotlin.renderer.DescriptorRendererImpl::renderFlexibleType()
     */


    open fun evalError(result: ReplEvalResult) {
        reader.println(result.toString())
    }

    override fun compileError(result: ReplCompileResult.Error) {
        reader.println("Message: ${result.message} Location: ${result.location}")
    }


    open fun commandError(e: Exception) {
        e.printStackTrace()
    }

    override fun close() {
        disposable.dispose()
    }

    open fun cleanUp() {
        reader.cleanUp()
    }
}


//class DefaultHistory: MemoryHistory(), PersistentHistory {
//    override fun flush() {
//        // do nothing
//    }
//
//    override fun purge() {
//        // do nothing
//    }
//}