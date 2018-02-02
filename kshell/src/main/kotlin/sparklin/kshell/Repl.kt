package sparklin.kshell

import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import java.io.File
import kotlin.reflect.KClass

interface Repl : EventManager {
    fun addClasspathRoots(files: List<File>)

    fun addImports(imports: List<String>)

    fun compile(code: String): ReplCompileResult

    fun compileAndEval(line: String)

    fun registerCommand(command: sparklin.kshell.Command)

    fun extensionPoint(body: () -> Unit)

    fun compileError(result: ReplCompileResult.Error)

    var wrapper: InvokeWrapper

    fun listCommands(): Iterable<sparklin.kshell.Command>
}

interface EventManager {
    fun <E: Any> registerEventHandler(eventType: KClass<E>, handler: EventHandler<E>)

    fun <T> emitEvent(event: Event<T>)
}

interface EventHandler<E> {
    fun handle(event: E)
}

interface Event<T> {
    fun data(): T
}

class OnCompile(private val compiledClasses: ReplCompileResult.CompiledClasses) : Event<ReplCompileResult.CompiledClasses> {
    override fun data(): ReplCompileResult.CompiledClasses = compiledClasses
}
