package org.jetbrains.kotlin.ki.shell

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.ki.shell.configuration.BooleanConverter
import org.jetbrains.kotlin.ki.shell.configuration.IntConverter
import org.jetbrains.kotlin.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlin.ki.shell.wrappers.ResultWrapper
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmReplEvaluator
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.util.LinkedSnippet

open class Shell(val replConfiguration: ReplConfiguration,
                 val hostConfiguration: ScriptingHostConfiguration,
                 val baseCompilationConfiguration: ScriptCompilationConfiguration,
                 val baseEvaluationConfiguration: ScriptEvaluationConfiguration
) {

    var compilationConfiguration: ScriptCompilationConfiguration = baseCompilationConfiguration
        private set

    var evaluationConfiguration: ScriptEvaluationConfiguration = baseEvaluationConfiguration
        private set

    lateinit var compiler: KJvmReplCompilerWithIdeServices
        private set

    private lateinit var evaluator: BasicJvmReplEvaluator

    val currentSnippetNo = AtomicInteger()

    val incompleteLines = arrayListOf<String>()

    lateinit var readerBuilder: LineReaderBuilder
    lateinit var reader: LineReader

    val highlighter = ContextHighlighter({ s -> !isCommandMode(s)}, { s -> commands.firstOrNull { it.weakMatch(s) } })

    val commands = mutableListOf<org.jetbrains.kotlin.ki.shell.Command>(FakeQuit())
    val eventManager = EventManager()

    var prompt = {
        if (incompleteLines.isEmpty())
        "kotlin> "
        else
        "... "
    }

    var evaluationTimeMillis: Long = 0

    class EvalThread: Thread() {
        lateinit var evalBlock: () -> ResultWrapper
        var result: ResultWrapper = ResultWrapper(ResultWithDiagnostics.Failure("Interrupted".asErrorDiagnostics()))

        override fun run() {
            result = evalBlock()
        }
    }

    var evalThread = EvalThread()

    class Settings(conf: ReplConfiguration) {
        val overrideSignals: Boolean by conf.get(BooleanConverter, default = true)
        val maxResultLength: Int by conf.get(IntConverter, default = 10000)
        val blankLinesAllowed: Int by  conf.get(IntConverter, default = 2)
        val sayHello: Boolean by conf.get(BooleanConverter, default = true)
    }

    lateinit var settings: Settings

    private class FakeQuit: BaseCommand() {
        override val name: String = "quit"
        override val short: String = "q"
        override val description: String = "quit the shell"
        override fun execute(line: String) {}
    }

    fun updateCompilationConfiguration(body: ScriptCompilationConfiguration.Builder.() -> Unit) {
        compilationConfiguration = compilationConfiguration.with(body)
    }

    fun updateEvaluationConfiguration(body: ScriptEvaluationConfiguration.Builder.() -> Unit) {
        evaluationConfiguration = evaluationConfiguration.with(body)
    }

    fun listCommands(): Iterable<org.jetbrains.kotlin.ki.shell.Command> = commands.asIterable()

    fun initEngine() {
        replConfiguration.load()

        settings = Settings(replConfiguration)

        val term = if (settings.overrideSignals) {
            TerminalBuilder.builder().nativeSignals(true).signalHandler {
                if (it == Terminal.Signal.INT) {
                    interrupt()
                }
            }.build()
        } else {
            TerminalBuilder.builder().build()
        }

        readerBuilder = LineReaderBuilder.builder().terminal(term).highlighter(highlighter)
        reader = readerBuilder.build()

        replConfiguration.plugins().forEach { it.init(this, replConfiguration) }

        reader.setVariable(LineReader.HISTORY_FILE, replConfiguration.get(LineReader.HISTORY_FILE,
                System.getProperty("user.home") + File.separator + ".kshell_history"))
        reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "")
        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)

        compiler = KJvmReplCompilerWithIdeServices(hostConfiguration)
        evaluator = BasicJvmReplEvaluator()
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

        if (settings.sayHello) sayHello()

        var blankLines = 0

        do {
            try {
            val line = reader.readLine(prompt())

            if (line == null || isQuitAction(line)) break

            if (isCommandMode(line)) {
                try {
                    val action = commands.first { it.match(line) }
                    action.execute(line)
                    currentSnippetNo.incrementAndGet()
                } catch (_: NoSuchElementException) {
                    println("Unknown command $line")
                } catch (e: Exception) {
                    commandError(e)
                }
            } else {
                if (line.isBlank() && incompleteLines.isNotEmpty()) {
                    if (blankLines == settings.blankLinesAllowed - 1) {
                        incompleteLines.clear()
                        println("You typed ${settings.blankLinesAllowed} blank lines. Starting a new command.")
                    } else blankLines ++
                } else {
                    val source = (incompleteLines + line).joinToString(separator = "\n")
                    val time = System.nanoTime()
                    val result = eval(source)
                    evaluationTimeMillis = (System.nanoTime() - time) / 1_000_000
                    when (result.getStatus()) {
                        ResultWrapper.Status.INCOMPLETE -> incompleteLines.add(line)
                        ResultWrapper.Status.ERROR -> {
                            incompleteLines.clear()
                            handleError(result.result)
                        }
                        ResultWrapper.Status.SUCCESS -> {
                            incompleteLines.clear()
                            handleSuccess(result.result as ResultWithDiagnostics.Success<*>)
                        }
                    }
                }
            } }
            catch (e: UserInterruptException) { if (settings.overrideSignals) currentSnippetNo.incrementAndGet() else break }
            catch (ee: EndOfFileException) { break }
            catch (ex: Exception) { ex.printStackTrace() }
        } while (true)

        cleanUp()
    }

    fun registerCommand(command: org.jetbrains.kotlin.ki.shell.Command) {
        commands.add(command)
    }

    private fun isQuitAction(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    private fun nextLine(code: String) = code.toScriptSource("Line_${currentSnippetNo.incrementAndGet()}.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}")

    private fun tempLine(code: String) = code.toScriptSource("\$\$tempLine_${UUID.randomUUID()}.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}")

    suspend fun compile(code: String) = compiler.compile(nextLine(code), compilationConfiguration)

    fun compile(code: SourceCode) = runBlocking { compiler.compile(code, compilationConfiguration) }

    fun analyze(code: String, pos: SourceCode.Position) = runBlocking { compiler.analyze(tempLine(code), pos, compilationConfiguration) }

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
        runBlocking {
            val compileResult: ResultWithDiagnostics<LinkedSnippet<KJvmCompiledScript>> = compile(source)
            ResultWrapper(when (compileResult) {
                is ResultWithDiagnostics.Failure -> compileResult
                is ResultWithDiagnostics.Success -> {
                    eventManager.emitEvent(OnCompile(compileResult.value))
                    evaluator.eval(compileResult.value, evaluationConfiguration)
                }
            })
        }

    fun handleError(result: ResultWithDiagnostics<*>) = printDiagnostics(result)

    private fun printDiagnostics(result: ResultWithDiagnostics<*>) {
        result.reports.forEach { println(it.render(withStackTrace = true)) }
    }

    fun handleSuccess(result: ResultWithDiagnostics.Success<*>) {
        printDiagnostics(result)
        // TODO: avoid unchecked cast
        val snippets = result.value as LinkedSnippet<KJvmEvaluatedSnippet>
        eventManager.emitEvent(OnEval(snippets))
        val evalResultValue = snippets.get().result
        when (evalResultValue) {
            is ResultValue.Value ->
                println(evalResultValue.value.toString().bound(settings.maxResultLength))
            is ResultValue.Error -> {
                val error = evalResultValue.error
                error.printStackTrace()
//                if (error.cause != null) error.cause?.printStackTrace() else println("Runtime Error: ${error.message}")
            }
        }
    }

    private fun commandError(e: Exception) {
        e.printStackTrace()
    }

    fun cleanUp() {
        reader.history.save()
    }

    private fun sayHello() {
        println("ki-shell $VERSION/${KotlinVersion.CURRENT}")
        replConfiguration.plugins().forEach { it.sayHello() }
    }

    companion object {
        const val VERSION: String = "0.2.7"
    }
}

class OnCompile(private val data: LinkedSnippet<KJvmCompiledScript>) : Event<LinkedSnippet<KJvmCompiledScript>> {
    override fun data(): LinkedSnippet<KJvmCompiledScript> = data
}

class OnEval(private val data: LinkedSnippet<KJvmEvaluatedSnippet>) : Event<LinkedSnippet<KJvmEvaluatedSnippet>> {
    override fun data(): LinkedSnippet<KJvmEvaluatedSnippet> = data
}
