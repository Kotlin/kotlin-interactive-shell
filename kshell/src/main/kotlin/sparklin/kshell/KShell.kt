package sparklin.kshell

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import sparklin.kshell.configuration.BooleanConverter
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.IntConverter
import sparklin.kshell.org.jline.reader.EndOfFileException
import sparklin.kshell.org.jline.reader.LineReader
import sparklin.kshell.org.jline.reader.LineReaderBuilder
import sparklin.kshell.org.jline.reader.UserInterruptException
import sparklin.kshell.org.jline.terminal.Terminal
import sparklin.kshell.org.jline.terminal.TerminalBuilder
import com.github.khud.kshell.repl.*
import sparklin.kshell.wrappers.ResultWrapper
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

open class KShell(val disposable: Disposable,
                  val configuration: Configuration,
                  val messageCollector: MessageCollector,
                  val classpath: List<File>,
                  val moduleName: String,
                  val classLoader: ClassLoader) {

    private val compilerConfiguration = CompilerConfiguration().apply {
        addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
        addJvmClasspathRoots(classpath)
        put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    }

    private val baseClassloader = URLClassLoader(compilerConfiguration.jvmClasspathRoots.map { it.toURI().toURL() }
            .toTypedArray(), classLoader)

    lateinit var compiler: ReplCompiler
        private set

    private lateinit var evaluator: ReplEvaluator

    val state = ReplState(ReentrantReadWriteLock())

    val incompleteLines = arrayListOf<String>()

    lateinit var readerBuilder: LineReaderBuilder
    lateinit var reader: LineReader

    val highlighter = ContextHighlighter({ s -> !isCommandMode(s)}, { s -> commands.firstOrNull { it.weakMatch(s) } })

    val commands = mutableListOf<sparklin.kshell.Command>(FakeQuit())
    val eventManager = EventManager()

    var invokeWrapper: InvokeWrapper? = null

    var prompt = {
        if (incompleteLines.isEmpty())
        "kotlin> "
        else
        "... "
    }

    var evaluationTimeMillis: Long = 0

    class EvalThread: Thread() {
        lateinit var evalBlock: () -> ResultWrapper
        var result: ResultWrapper = ResultWrapper(Result.Error(EvalError.RuntimeError("Interrupted")))

        override fun run() {
            result = evalBlock()
        }
    }

    var evalThread = EvalThread()

    class Settings(conf: Configuration) {
        val overrideSignals: Boolean by conf.get(BooleanConverter, default = true)
        val maxResultLength: Int by conf.get(IntConverter, default = 500)
    }

    lateinit var settings: Settings

    private class FakeQuit: sparklin.kshell.BaseCommand() {
        override val name: String = "quit"
        override val short: String = "q"
        override val description: String = "quit the kshell"
        override fun execute(line: String) {}
    }

    fun listCommands(): Iterable<sparklin.kshell.Command> = commands.asIterable()

    fun addClasspathRoots(files: List<File>) = compilerConfiguration.addJvmClasspathRoots(files)

    fun initEngine() {
        configuration.load()

        settings = Settings(configuration)

        val term = if (settings.overrideSignals) {
            TerminalBuilder.builder().nativeSignals(true).signalHandler({
                if (it == Terminal.Signal.INT) {
                    interrupt()
                }
            }).build()
        } else {
            TerminalBuilder.builder().build()
        }

        readerBuilder = LineReaderBuilder.builder().terminal(term).highlighter(highlighter)
        reader = readerBuilder.build()

        configuration.plugins().forEach { it.init(this, configuration) }

        reader.setVariable(LineReader.HISTORY_FILE, configuration.get(LineReader.HISTORY_FILE,
                System.getProperty("user.home") + File.separator + ".kshell_history"))
        reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "")

        compiler = ReplCompiler(disposable, compilerConfiguration, messageCollector)
        evaluator = ReplEvaluator(classpath, baseClassloader)
    }

    private fun interrupt() {
        evalThread.interrupt()
        evalThread = EvalThread()
    }

    private fun isCommandMode(buffer: String): Boolean = incompleteLines.isEmpty()
                && buffer.startsWith(":")
                && !buffer.startsWith("::")

    fun doRun() {
        initEngine()

        do {
            try {
            val line = reader.readLine(prompt())

            if (line == null || isQuitAction(line)) break

            if (isCommandMode(line)) {
                try {
                    val action = commands.first { it.match(line) }
                    action.execute(line)
                    state.lineIndex.getAndIncrement()
                } catch (_: NoSuchElementException) {
                    println("Unknown command $line")
                } catch (e: Exception) {
                    commandError(e)
                }
            } else {
                if (line.isBlank() && (incompleteLines.isNotEmpty() && incompleteLines.last().isBlank())) {
                    incompleteLines.clear()
                    println("You typed two blank lines. Starting a new command.")
                } else {
                    val source = (incompleteLines + line).joinToString(separator = "\n")
                    val time = System.nanoTime()
                    val result = eval(source).result
                    evaluationTimeMillis = (System.nanoTime() - time) / 1_000_000
                    when (result) {
                        is Result.Error -> {
                            if (result.error.isIncomplete) {
                                incompleteLines.add(line)
                            } else {
                                incompleteLines.clear()
                                handleError(result.error)
                            }
                        }
                        is Result.Success -> {
                            incompleteLines.clear()
                            handleSuccess(result.data)
                        }
                    }
                }
            } }
            catch (e: UserInterruptException) { if (settings.overrideSignals) state.lineIndex.getAndIncrement() else break }
            catch (ee: EndOfFileException) { break }
        } while (true)

        cleanUp()
    }

    fun registerCommand(command: sparklin.kshell.Command) {
        commands.add(command)
    }

    private fun isQuitAction(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    private fun nextLine(code: String) = CodeLine(state.lineIndex.getAndIncrement(), code)

    private fun compile(code: String) = compiler.compile(state, nextLine(code))

    fun compile(code: SourceCode) = compiler.compile(state, code)

    fun eval(source: String): ResultWrapper {
        return if (settings.overrideSignals) {
            evalThread.apply {
                evalBlock = { compileAndEval(source) }
                start()
                join()
            }
            val result = evalThread.result
            evalThread = EvalThread()
            result
        } else {
            compileAndEval(source)
        }
    }

    fun compileAndEval(source: String): ResultWrapper =
        state.lock.write {
            val compileResult = compile(source)
            ResultWrapper(when (compileResult) {
                is Result.Error -> {
                    Result.Error<EvalResult, EvalError>(compileResult.error)
                }
                is Result.Success -> {
                    eventManager.emitEvent(OnCompile(compileResult.data))
                    evaluator.eval(state, compileResult.data, invokeWrapper)
                }
            })
        }

    fun handleError(error: EvalError) = when (error) {
            is EvalError.RuntimeError -> {
                if (error.cause != null) error.cause?.printStackTrace() else println("Runtime Error: ${error.message}")
            }
            is EvalError.CompileError -> println(error.message)
        }

    fun handleSuccess(result: EvalResult) {
        if (result is EvalResult.ValueResult)
            println(result.toString().bound(settings.maxResultLength))
    }

    private fun commandError(e: Exception) {
        e.printStackTrace()
    }

    fun cleanUp() {
        reader.history.save()
    }

    fun checker() = compiler.checker
}

class OnCompile(private val data: CompilationData) : Event<CompilationData> {
    override fun data(): CompilationData = data
}