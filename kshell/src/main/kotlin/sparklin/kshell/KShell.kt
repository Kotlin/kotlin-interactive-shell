package sparklin.kshell

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import sparklin.kshell.configuration.CachedInstance
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.ConfigurationImpl
import sparklin.kshell.console.Completer
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
import java.io.PrintStream
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

open class KShell protected constructor(val configuration: Configuration,
                                        val disposable: Disposable,
                                        val scriptDefinition: KotlinScriptDefinitionEx,
                                        val compilerConfiguration: CompilerConfiguration,
                                        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                                        protected val emptyArgsProvider: ScriptTemplateEmptyArgsProvider,
                                        protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : Closeable, Repl {

    constructor(configuration: Configuration,
                disposable: Disposable = Disposer.newDisposable(),
                moduleName: String = "kotlin-script-module-${System.currentTimeMillis()}",
                additionalClasspath: List<File> = emptyList(),
                scriptDefinition: KotlinScriptDefinitionEx = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES)),
                messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE,
                includeStdlib: Boolean) : this(configuration, disposable,
            compilerConfiguration = CompilerConfiguration().apply {
                addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
                addJvmClasspathRoots(findRequiredScriptingJarFiles(scriptDefinition.template,
                        includeScriptEngine = false,
                        includeKotlinCompiler = false,
                        includeStdLib = includeStdlib,
                        includeRuntime = false))
                addJvmClasspathRoots(additionalClasspath)
                put(CommonConfigurationKeys.MODULE_NAME, moduleName)
                put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            },
            repeatingMode = repeatingMode,
            scriptDefinition = scriptDefinition,
            emptyArgsProvider = scriptDefinition)

    constructor(configuration: Configuration, additionalClasspath: List<File>, scriptDefinition: KotlinScriptDefinitionEx, includeStdlib: Boolean = true) :
            this(configuration = configuration, additionalClasspath = additionalClasspath,
                    scriptDefinition = scriptDefinition, disposable = Disposer.newDisposable(), includeStdlib = includeStdlib)

    var fallbackArgs: ScriptArgsWithTypes? = emptyArgsProvider.defaultEmptyArgs
        get() = stateLock.read { field }
        set(value) = stateLock.write { field = value }

    private val baseClassloader = URLClassLoader(compilerConfiguration.jvmClasspathRoots.map { it.toURI().toURL() }
            .toTypedArray(), this.javaClass.classLoader)

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

    lateinit var engine: GenericRepl

    lateinit var state: IReplStageState<*>

    val incompleteLines = arrayListOf<String>()
    val nextLine: AtomicInteger = AtomicInteger(REPL_CODE_LINE_FIRST_NO)
    val resultCounter: AtomicInteger = AtomicInteger(1)

    val reader = configuration.getConsoleReader()

    val commands = mutableListOf<sparklin.kshell.Command>(FakeQuit())

    val additionalImports = mutableListOf<String>()

    private class FakeQuit: sparklin.kshell.BaseCommand("quit", "q", "exit the interpreter") {
        override fun execute(line: String) {}
    }

    override var wrapper: InvokeWrapper = object: InvokeWrapper {
        override fun <T> invoke(body: () -> T): T {
            return body()
        }
    }

    open fun buildDefaultCompleter() = Completer.DEFAULT_COMPLETER

//    fun getReplEnvironment() = engine.compiler.getReplEnvironment()


    override fun listCommands(): Iterable<sparklin.kshell.Command> = commands.asIterable()

    override fun addClasspathRoots(files: List<File>) = compilerConfiguration.addJvmClasspathRoots(files)

    override fun addImports(imports: List<String>) {
        additionalImports.addAll(imports)
    }

    fun initEngine() {
        reader.apply {
            addCompleter(sparklin.kshell.ContextDependentCompleter(commands, incompleteLines::isEmpty, buildDefaultCompleter()))
        }

        configuration.load()
        configuration.plugins().forEach { it.init(this, configuration) }

        engine = object: GenericRepl(disposable = disposable,
                scriptDefinition = scriptDefinition,
                compilerConfiguration = compilerConfiguration,
                messageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false),
                baseClassloader = baseClassloader,
                fallbackScriptArgs = fallbackArgs,
                repeatingMode = repeatingMode) {}

        state = engine.createState(stateLock)
    }

    fun doRun() {

        initEngine()
        initImports()

        do {
            printPrompt()
            val line = reader.readLine()

            if (line == null || isQuitAction(line)) break

            if (incompleteLines.isEmpty() && line.startsWith(":") && !line.startsWith("::")) {
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

    private fun initImports() {
        scriptDefinition.defaultImports.forEach { compileAndEval("import $it") }
        additionalImports.forEach { compileAndEval("import $it") }
    }

    override fun registerCommand(command: sparklin.kshell.Command) {
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

    private fun compileInternal(code: String): ReplCompileResult {
        val replCodeLine = ReplCodeLine(nextLine.incrementAndGet(), state.currentGeneration, code)
        return engine.compile(state, replCodeLine)
    }

    override fun compile(code: String): CompileResult = compileInternal(code).wrap()

    override fun compileAndEval(line: String, interactive: Boolean): EvalResult {
        return state.lock.write {
            val source = (incompleteLines + line).joinToString(separator = "\n")
            val compileResult = compileInternal(source)
            when (compileResult) {
                is ReplCompileResult.Incomplete -> {
                    incompleteLines.add(line)
                    if (!interactive) incompleteLines.clear()
                    return EvalResult.Incomplete
                }
                is ReplCompileResult.Error -> {
                    compileError(compileResult.wrap())
                    alterState(state)
                    incompleteLines.clear()
                    return EvalResult.Error(compileResult.message)
                }
                is ReplCompileResult.CompiledClasses -> {
                    val isMultiline = line.contains('\n')
                    // it is difficult to get the last expression
                    // which actually produce value, therefore
                    // we won't create resXXX variable
                    if (!isMultiline &&
                            compileResult.type != null &&
                            compileResult.type != "kotlin.Unit") {
                        alterState(state)
                        val code = if (incompleteLines.isNotEmpty()) {
                            incompleteLines.add(line)
                            incompleteLines.joinToString(separator = "\n")
                        } else line
                        return valueResult(compileResult, code)
                    }
                    EventManager.emitEvent(OnCompile(compileResult.wrap()))
                    val evalResult = engine.eval(state, compileResult, fallbackArgs, wrapper)
                    incompleteLines.clear()
                    return when (evalResult) {
                        is ReplEvalResult.Incomplete,
                        is ReplEvalResult.ValueResult -> {
                            if (isMultiline) {
                                reader.println(evalResult.toString())
                            }
                            EvalResult.Success
                        }
                        is ReplEvalResult.UnitResult -> {
                            EvalResult.Success
                        }
                        is ReplEvalResult.Error -> {
                            EvalResult.Error(evalResult.message)
                        }
                        is ReplEvalResult.HistoryMismatch -> {
                            evalError(evalResult)
                            EvalResult.Error(evalResult.toString())
                        }
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

    fun printPrompt() {
        if (incompleteLines.isEmpty())
            reader.setPrompt("kotlin> ")
        else
            reader.setPrompt("... ")
    }

    open fun valueResult(result: ReplCompileResult.CompiledClasses, line: String): EvalResult {
        val name = "res${resultCounter.getAndIncrement()}"
        val first = compileAndEval("val $name = $line")
        if (first != EvalResult.Success) return first
        return compileAndEval("println(\"$name: ${result.type} = \" + $name)")
    }

    open fun evalError(result: ReplEvalResult) {
        reader.println(result.toString())
    }

    override fun compileError(result: CompileResult.Error) {
        reader.println("Message: ${result.message}")
//        reader.println("Message: ${result.message} Location: ${result.location}")
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

class ReplClassData(private val classData: CompiledClassData): CompileResult.ClassData {
    override val path: String
        get() = classData.path
    override val bytes: ByteArray
        get() = classData.bytes
}

fun ReplCompileResult.Error.wrap(): CompileResult.Error {
    val that = this
    return object : CompileResult.Error {
        override val message: String
            get() = that.message
    }
}

fun ReplCompileResult.Incomplete.wrap(): CompileResult.Incomplete = CompileResult.Incomplete

fun ReplCompileResult.CompiledClasses.wrap(): CompileResult.CompiledClasses {
    val that = this
    return object : CompileResult.CompiledClasses {
        override val mainClassName: String
            get() = that.mainClassName
        override val classes: List<CompileResult.ClassData>
            get() = that.classes.map { ReplClassData(it) }
        override val hasResult: Boolean
            get() = that.hasResult
        override val type: String?
            get() = that.type
    }
}

fun ReplCompileResult.wrap(): CompileResult {
    return when (this) {
        is ReplCompileResult.CompiledClasses -> this.wrap()
        is ReplCompileResult.Error -> this.wrap()
        is ReplCompileResult.Incomplete -> this.wrap()
    }
}