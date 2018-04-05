package sparklin.kshell.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

class Repl(disposable: Disposable,
           compilerConfiguration: CompilerConfiguration,
           messageCollector: MessageCollector,
           baseClasspath: Iterable<File>,
           baseClassloader: ClassLoader?) {

    constructor(compilerConfiguration: CompilerConfiguration,
                messageCollector: MessageCollector,
                baseClasspath: Iterable<File>,
                baseClassloader: ClassLoader?  = Thread.currentThread().contextClassLoader) :
            this(Disposer.newDisposable(), compilerConfiguration, messageCollector, baseClasspath, baseClassloader)

    private val compiler = ReplCompiler(disposable, compilerConfiguration, messageCollector)
    private val evaluator = ReplEvaluator(baseClasspath, baseClassloader)
    internal val state = State(ReentrantReadWriteLock())

    fun eval(code: String): Result<EvalResult, EvalError> {
        val res = compiler.compile(state, CodeLine(state.lineIndex.getAndIncrement(), code))
        return when (res) {
            is Result.Error -> Result.Error(res.error)
            is Result.Incomplete -> Result.Incomplete()
            is Result.Success -> evaluator.eval(state, res.data.first, res.data.second, null)
        }
    }
}