package sparklin.kshell

import com.intellij.openapi.Disposable
import sparklin.kshell.console.Completer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.repl.*
import java.io.Closeable
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

open class KShell(val disposable: Disposable,
                  val configuration: Configuration,
                  val compilerConfiguration: CompilerConfiguration,
                  val messageCollector: MessageCollector,
                  val baseClasspath: Iterable<File>,
                  val baseClassloader: ClassLoader?) : Closeable {

    private lateinit var compiler: ReplCompiler
    private lateinit var evaluator: ReplEvaluator
    val state = ReplState(ReentrantReadWriteLock())

    val incompleteLines = arrayListOf<String>()
    val reader = configuration.getConsoleReader()
    val commands = mutableListOf<sparklin.kshell.Command>(FakeQuit())
    val additionalImports = mutableListOf<String>()
    val eventManager = EventManager()

    private class FakeQuit: sparklin.kshell.BaseCommand("quit", "q", "exit the interpreter") {
        override fun execute(line: String) {}
    }

    var wrapper: InvokeWrapper = object: InvokeWrapper {
        override fun <T> invoke(body: () -> T): T {
            return body()
        }
    }

    open fun buildDefaultCompleter() = Completer.DEFAULT_COMPLETER

//    fun getReplEnvironment() = engine.compiler.getReplEnvironment()


    fun listCommands(): Iterable<sparklin.kshell.Command> = commands.asIterable()

    fun addClasspathRoots(files: List<File>) = compilerConfiguration.addJvmClasspathRoots(files)

    fun initEngine() {
        reader.apply {
            addCompleter(ContextDependentCompleter(commands, incompleteLines::isEmpty, buildDefaultCompleter()))
        }

        configuration.load()
        configuration.plugins().forEach { it.init(this, configuration) }

        compiler = ReplCompiler(disposable, compilerConfiguration, messageCollector)
        evaluator = ReplEvaluator(baseClasspath, baseClassloader)
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
                    eval(line)
                }
            }
        } while (true)

        cleanUp()
    }

    private fun initImports() {
//        scriptDefinition.defaultImports.forEach { compileAndEval("import $it") }
//        additionalImports.forEach { compileAndEval("import $it") }
    }

    fun registerCommand(command: sparklin.kshell.Command) {
        commands.add(command)
    }

    private fun isQuitAction(line: String): Boolean {
        return incompleteLines.isEmpty() && (line.equals(":quit", ignoreCase = true) || line.equals(":q", ignoreCase = true))
    }

    private fun nextLine(code: String) = CodeLine(state.lineIndex.getAndIncrement(), code)

    fun compile(code: String) = compiler.compile(state, nextLine(code))

    fun eval(line: String) {
        return state.lock.write {
            val source = (incompleteLines + line).joinToString(separator = "\n")
            val compileResult = compile(source)
            when (compileResult) {
                is Result.Incomplete -> {
                    incompleteLines.add(line)
                }
                is Result.Error -> {
                    compilationError(compileResult)
                    incompleteLines.clear()
                }
                is Result.Success -> {
                    eventManager.emitEvent(OnCompile(compileResult.data))
                    val evalResult = evaluator.eval(state, compileResult.data, null)
                    when (evalResult) {
                        is Result.Error -> {
                            reader.println("Message: ${evalResult.error.message}")
                        }
                        is Result.Success -> if (evalResult.data is EvalResult.ValueResult) {
                            incompleteLines.clear()
                            reader.println(evalResult.data.toString())
                        }
                    }
                }
            }
        }
    }

    fun compilationError(compileResult: Result.Error<CompilationData, EvalError.CompileError>) {
        reader.println("Message: ${compileResult.error.message}")
    }

    fun printPrompt() {
        if (incompleteLines.isEmpty())
            reader.setPrompt("kotlin [${state.lineIndex.get()}]> ")
        else
            reader.setPrompt("... ")
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

class OnCompile(private val data: CompilationData) : Event<CompilationData> {
    override fun data(): CompilationData = data
}