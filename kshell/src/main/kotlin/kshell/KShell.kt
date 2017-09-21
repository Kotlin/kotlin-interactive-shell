package kshell

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import lib.jline.console.ConsoleReader
import lib.jline.console.history.MemoryHistory
import lib.jline.console.history.PersistentHistory
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
open class KShell protected constructor(protected val disposable: Disposable,
                                        protected val scriptDefinition: KotlinScriptDefinitionEx,
                                        protected val compilerConfiguration: CompilerConfiguration,
                                        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                                        protected val sharedHostClassLoader: ClassLoader? = null,
                                        protected val emptyArgsProvider: ScriptTemplateEmptyArgsProvider,
                                        protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock(),
                                        protected val shellHistory: PersistentHistory = DefaultHistory()) : Closeable {

    constructor(disposable: Disposable = Disposer.newDisposable(),
                moduleName: String = "kotlin-script-module-${System.currentTimeMillis()}",
                additionalClasspath: List<File> = emptyList(),
                scriptDefinition: KotlinScriptDefinitionEx = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES)),
                messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                sharedHostClassLoader: ClassLoader? = null,
                shellHistory: PersistentHistory = DefaultHistory()) : this(disposable,
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
            emptyArgsProvider = scriptDefinition,
            shellHistory = shellHistory)

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

        protected val compiler: ReplCompiler by lazy { GenericReplCompiler(disposable, scriptDefinition, compilerConfiguration, messageCollector) }
        protected val evaluator: ReplFullEvaluator by lazy { GenericReplCompilingEvaluator(compiler, compilerConfiguration.jvmClasspathRoots, baseClassloader, fallbackScriptArgs, repeatingMode) }

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

    val reader = ConsoleReader()
    val commands: MutableList<Command> = mutableListOf(FakeQuit())

    private class FakeQuit: Command("quit", "q", "exit the interpreter") {
        override fun execute(line: String) {}
    }

    fun doRun() {
        reader.apply {
            expandEvents = false
            history = shellHistory
            addCompleter(ContextDependentCompleter(commands))
        }
        initImports()
        do {
            printPrompt()
            val line = reader.readLine()

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

        cleanUp()
    }

    fun initImports() {
        scriptDefinition.defaultImports.forEach { compileAndEval("import $it") }
    }

    fun registerCommand(command: Command): Unit {
        command.repl = this
        commands.add(command)
    }

    private fun isQuitCommand(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    fun compileAndEval(line: String): Unit {
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
                    val evalResult = engine.eval(state, compileResult, fallbackArgs)
                    incompleteLines.clear()
                    when (evalResult) {
                        is ReplEvalResult.Incomplete -> throw IllegalStateException("Should never happen")
                        is ReplEvalResult.ValueResult -> valueResult(evalResult)
                        is ReplEvalResult.UnitResult -> { }
                        is ReplEvalResult.Error,
                        is ReplEvalResult.HistoryMismatch -> evalError(evalResult)
                    }
                }
            }
        }
    }

    fun alterState(state: IReplStageState<*>) {
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

    open fun afterCompile(compiledClasses: ReplCompileResult.CompiledClasses) { }

    fun printPrompt() {
        if (incompleteLines.isEmpty())
            reader.prompt = "kotlin> "
        else
            reader.prompt = "... "
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
    fun clarifyType(rawType: String?) = rawType?.let {
        rawType.replace("(Mutable)", "Mutable").replace("!", "?")
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

    open fun cleanUp() {
        shellHistory.flush()
    }

}


class DefaultHistory: MemoryHistory(), PersistentHistory {
    override fun flush() {
        // do nothing
    }

    override fun purge() {
        // do nothing
    }
}
