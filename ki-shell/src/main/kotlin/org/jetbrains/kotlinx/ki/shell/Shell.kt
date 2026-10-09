package org.jetbrains.kotlinx.ki.shell

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.K2ReplCompiler
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptDiagnosticsMessageCollector
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.currentSnippetNo
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlinx.ki.shell.configuration.BooleanConverter
import org.jetbrains.kotlinx.ki.shell.configuration.IntConverter
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File
import java.io.PrintStream
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.K2ReplEvaluator
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet

open class Shell(val replConfiguration: ReplConfiguration,
                 baseHostConfiguration: ScriptingHostConfiguration,
                 baseCompilationConfiguration: ScriptCompilationConfiguration,
                 baseEvaluationConfiguration: ScriptEvaluationConfiguration
) {

    var hostConfiguration: ScriptingHostConfiguration = baseHostConfiguration
        private set

    var compilationConfiguration: ScriptCompilationConfiguration = baseCompilationConfiguration.with {
        hostConfiguration(this@Shell.hostConfiguration)
    }
        private set

    var evaluationConfiguration: ScriptEvaluationConfiguration = baseEvaluationConfiguration.with {
        hostConfiguration(this@Shell.hostConfiguration)
    }
        private set

    private class ReplState(
        val compiler: K2ReplCompiler,
        val evaluator: K2ReplEvaluator,
        val disposable: Disposable,
    )

    private var replState: ReplState? = null

    val replLock = ReentrantLock()

    var ideServices: ReplIdeServices = K2ReplIdeServices(::compileProbe, BasicReplIdeServices { emptyList() })

    val currentSnippetNo = AtomicInteger()

    val incompleteLines = arrayListOf<String>()

    lateinit var readerBuilder: LineReaderBuilder
    lateinit var reader: LineReader

    val commands = mutableListOf<Command>(FakeQuit())
    val eventManager = EventManager()

    val highlighter = ContextHighlighter({ s -> !isCommandMode(s)}, { s -> commands.firstOrNull { it.weakMatch(s) } })
    val completer = KotlinCompleter({ ideServices }, incompleteLines, ::isCommandMode)
    val parser = KotlinReplSnippetParser()

    var prompt = {
        if (incompleteLines.isEmpty())
        "kotlin> "
        else
        "... "
    }

    var evaluationTimeMillis: Long = 0

    class EvalThread: Thread() {
        lateinit var evalBlock: () -> ResultWrapper
        var result: ResultWrapper = ResultWrapper(ResultWithDiagnostics.Failure("Interrupted".asErrorDiagnostics()), true)
        @Volatile var abandoned = false

        init {
            isDaemon = true
        }

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
        override fun execute(line: String): Command.Result = Command.Result.Success()
    }

    fun updateHostConfiguration(body: ScriptingHostConfiguration.Builder.() -> Unit) {
        hostConfiguration = hostConfiguration.with(body)
        updateCompilationConfiguration {
            hostConfiguration(this@Shell.hostConfiguration)
        }
        updateEvaluationConfiguration {
            hostConfiguration(this@Shell.hostConfiguration)
        }
    }

    fun updateCompilationConfiguration(body: ScriptCompilationConfiguration.Builder.() -> Unit) {
        compilationConfiguration = compilationConfiguration.with(body)
    }

    fun updateEvaluationConfiguration(body: ScriptEvaluationConfiguration.Builder.() -> Unit) {
        evaluationConfiguration = evaluationConfiguration.with(body)
    }

    fun listCommands(): Iterable<Command> = commands.asIterable()

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

        readerBuilder =
                LineReaderBuilder.builder()
                        .terminal(term)
                        .highlighter(highlighter)
                        .parser(parser)
                        .completer(completer)
        reader = readerBuilder.build()

        replConfiguration.plugins().forEach { it.init(this, replConfiguration) }

        reader.setVariable(LineReader.HISTORY_FILE, replConfiguration.get(LineReader.HISTORY_FILE,
                System.getProperty("user.home") + File.separator + ".kshell_history"))
        reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "")
        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)

        // Created after the plugins have updated the configuration; the later changes reach the compiler per snippet
        replState?.let { Disposer.dispose(it.disposable) }
        replState = createReplState()
    }

    internal fun interrupt() {
        val thread = evalThread
        if (!thread.isAlive) return
        thread.interrupt()
        for (i in 1..5) {
            if (!thread.isAlive) break
            Thread.sleep(100)
        }
        // Thread.stop is gone from modern JDKs, so code that ignores interruption is left running in the background
        if (thread.isAlive) thread.abandoned = true
    }

    private fun isCommandMode(buffer: String): Boolean = incompleteLines.isEmpty()
                && buffer.startsWith(":")
                && !buffer.startsWith("::")

    fun doRun() {
        initEngine()

        if (settings.sayHello) sayHello()

        var blankLines = 0

        fun evalSnippet(line: String) {
            if (line.isBlank() && incompleteLines.isNotEmpty()) {
                if (blankLines == settings.blankLinesAllowed - 1) {
                    incompleteLines.clear()
                    println("You typed ${settings.blankLinesAllowed} blank lines. Starting a new command.")
                } else blankLines++
            } else {
                val source = (incompleteLines + line).joinToString(separator = "\n")
                val time = System.nanoTime()
                val result = eval(source)
                evaluationTimeMillis = (System.nanoTime() - time) / 1_000_000
                when (result.getStatus()) {
                    ResultWrapper.Status.INCOMPLETE -> incompleteLines.add(line)
                    ResultWrapper.Status.ERROR -> {
                        incompleteLines.clear()
                        handleError(result.result, result.isCompiled)
                    }
                    ResultWrapper.Status.SUCCESS -> {
                        incompleteLines.clear()
                        handleSuccess(result.result as ResultWithDiagnostics.Success<*>)
                    }
                }
            }
        }

        do {
            try {
                val line = reader.readLine(prompt())

                if (line == null || isQuitAction(line)) break

                if (isCommandMode(line)) {
                    try {
                        val action = commands.first { it.match(line) }
                        when (val result = action.execute(line)) {
                            is Command.Result.Success -> {
                                result.message?.let { println(it) }
                            }
                            is Command.Result.Failure -> {
                                println("Error in command $line: ${result.message}")
                            }
                            is Command.Result.RunSnippets -> {
                                result.snippetsToRun.forEach(::evalSnippet)
                            }
                        }
                    } catch (_: NoSuchElementException) {
                        println("Unknown command $line")
                    } catch (e: Exception) {
                        commandError(e)
                    }
                } else {
                    evalSnippet(line)
                }
            }
            catch (e: UserInterruptException) { if (!settings.overrideSignals) break }
            catch (ee: EndOfFileException) { break }
            catch (ex: Exception) { ex.printStackTrace() }
        } while (true)

        cleanUp()
    }

    fun registerCommand(command: Command) {
        commands.add(command)
    }

    private fun isQuitAction(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    private fun nextLine(code: String) = code.toScriptSource("Line_${currentSnippetNo.get()}.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}")

    private fun replState(): ReplState = replState ?: error("The engine is not initialized, call initEngine() first")

    private fun createReplState(): ReplState {
        val disposable = Disposer.newDisposable("KI REPL")
        val replHostConfiguration = ScriptingHostConfiguration(hostConfiguration) {
            repl {
                firReplHistoryProvider(FirReplHistoryProviderImpl())
                isReplSnippetSource { _, _ -> true }
            }
        }
        val compilationState = K2ReplCompiler.createCompilationState(
            ScriptDiagnosticsMessageCollector(null),
            disposable,
            compilationConfiguration,
            replHostConfiguration
        )
        return ReplState(K2ReplCompiler(compilationState), K2ReplEvaluator(), disposable)
    }

    private fun snippetCompilationConfiguration(): ScriptCompilationConfiguration =
        compilationConfiguration.with {
            repl {
                currentSnippetNo(this@Shell.currentSnippetNo.get())
            }
        }

    suspend fun compile(code: String) = compile(nextLine(code))

    // Incomplete input is compiled again with the next line added, so only complete snippets take a number
    fun compile(code: SourceCode): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>> =
        replLock.withLock {
            val result = runBlocking { replState().compiler.compile(listOf(code), snippetCompilationConfiguration()) }
            if (result.reports.none { it.code == ScriptDiagnostic.incompleteCode }) currentSnippetNo.incrementAndGet()
            result
        }

    fun compileProbe(code: String): ResultWithDiagnostics<LinkedSnippet<CompiledSnippet>>? {
        if (!replLock.tryLock(PROBE_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) return null
        return try {
            val source = code.toScriptSource("Line_${currentSnippetNo.get()}_probe.${compilationConfiguration[ScriptCompilationConfiguration.fileExtension]}")
            runBlocking { replState().compiler.compile(listOf(source), snippetCompilationConfiguration()) }
        } finally {
            replLock.unlock()
        }
    }

    fun eval(source: String): ResultWrapper {
        return if (settings.overrideSignals) {
            val thread = evalThread.apply {
                evalBlock = { compileAndEval(source) }
                start()
            }
            while (thread.isAlive && !thread.abandoned) thread.join(50)
            val result = thread.result
            evalThread = EvalThread()
            result
        } else {
            compileAndEval(source)
        }
    }

    fun compileAndEval(source: String): ResultWrapper =
        runBlocking {
            val compileResult = compile(source)
            val res = when {
                Thread.currentThread().isInterrupted ->
                    ResultWrapper(ResultWithDiagnostics.Failure("Interrupted".asErrorDiagnostics()), true)
                compileResult is ResultWithDiagnostics.Success -> {
                    eventManager.emitEvent(OnCompile(compileResult.value))
                    ResultWrapper(replState().evaluator.eval(compileResult.value, evaluationConfiguration), true)
                }
                else -> ResultWrapper(compileResult, false)
            }
            res
        }

    fun handleError(result: ResultWithDiagnostics<*>, isCompiled: Boolean) = printDiagnostics(result, isCompiled)

    private fun printDiagnostics(result: ResultWithDiagnostics<*>, isCompiled: Boolean) {
        result.reports.forEach {
            println(it.render(withStackTrace = isCompiled))
        }
    }

    fun handleSuccess(result: ResultWithDiagnostics.Success<*>) {
        printDiagnostics(result, true)
        // TODO: avoid unchecked cast
        val snippets = result.value as LinkedSnippet<KJvmEvaluatedSnippet>
        eventManager.emitEvent(OnEval(snippets))
        val evalResultValue = snippets.get().result
        when (evalResultValue) {
            is ResultValue.Value ->
                println(renderResult(evalResultValue).bound(settings.maxResultLength))
            is ResultValue.Error -> {
                evalResultValue.renderError(System.err)
            }
            else -> {}
        }
    }

    fun renderResultType(res: ResultValue.Value): String = ": " + renderKotlinType(res.type)

    fun renderResult(res: ResultValue.Value): String = "${res.name}${renderResultType(res)} = ${boxValueClass(res)}"

    private fun commandError(e: Exception) {
        e.printStackTrace()
    }

    fun cleanUp() {
        if (::reader.isInitialized) reader.history.save()
        replState?.let { Disposer.dispose(it.disposable) }
        replState = null
    }

    private fun sayHello() {
        printVersion()
        replConfiguration.plugins().forEach { it.sayHello() }
    }
}

private const val PROBE_LOCK_TIMEOUT_MS = 2000L

class OnCompile(private val data: LinkedSnippet<CompiledSnippet>) : Event<LinkedSnippet<CompiledSnippet>> {
    override fun data(): LinkedSnippet<CompiledSnippet> = data
}

class OnEval(private val data: LinkedSnippet<KJvmEvaluatedSnippet>) : Event<LinkedSnippet<KJvmEvaluatedSnippet>> {
    override fun data(): LinkedSnippet<KJvmEvaluatedSnippet> = data
}

private fun ResultValue.Error.renderError(stream: PrintStream) {
    val fullTrace = error.stackTrace
    if (wrappingException == null || fullTrace.size < wrappingException!!.stackTrace.size) {
        error.printStackTrace(stream)
    } else {
        // subtracting wrapping message stacktrace from error stacktrace to show only user-specific part of it
        // TODO: consider more reliable logic, e.g. comparing traces, fallback to full error printing in case of mismatch
        // TODO: write tests
        stream.println(error)
        val scriptTraceSize = fullTrace.size - wrappingException!!.stackTrace.size
        for (i in 0 until scriptTraceSize) {
            stream.println("\tat " + fullTrace[i])
        }
    }
}

// A result of a value class type, e.g. `ULong`, is stored unboxed, so its own `toString` is not used without boxing
private fun boxValueClass(res: ResultValue.Value): Any? {
    val value = res.value ?: return null
    val classLoader = res.scriptInstance?.javaClass?.classLoader ?: return value
    val valueClass = loadClassByKotlinName(res.type.substringBefore('<').removeSuffix("?"), classLoader) ?: return value
    if (valueClass.isInstance(value)) return value
    val box = valueClass.declaredMethods.singleOrNull {
        it.name == "box-impl" && Modifier.isStatic(it.modifiers) && it.parameterCount == 1
    } ?: return value
    return try {
        box.isAccessible = true
        box.invoke(null, value)
    } catch (_: Exception) {
        value
    }
}

private fun loadClassByKotlinName(name: String, classLoader: ClassLoader): Class<*>? {
    var binaryName = name
    while (true) {
        try {
            return Class.forName(binaryName, false, classLoader)
        } catch (_: ClassNotFoundException) {
        } catch (_: LinkageError) {
        }
        val lastDot = binaryName.lastIndexOf('.')
        if (lastDot < 0) return null
        binaryName = binaryName.substring(0, lastDot) + "$" + binaryName.substring(lastDot + 1)
    }
}

// TODO: put into the kotlin codebase, remove from here
private fun ScriptingHostConfiguration?.with(body: ScriptingHostConfiguration.Builder.() -> Unit): ScriptingHostConfiguration {
    val newConfiguration =
            if (this == null) ScriptingHostConfiguration(body = body)
            else ScriptingHostConfiguration(this, body = body)
    return if (newConfiguration == this) this else newConfiguration
}
