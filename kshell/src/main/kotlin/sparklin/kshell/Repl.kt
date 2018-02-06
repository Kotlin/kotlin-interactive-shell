package sparklin.kshell

import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import java.io.File
import kotlin.reflect.KClass

interface Repl {
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

class OnCompile(private val compiledClasses: ReplCompileResult.CompiledClasses) : Event<ReplCompileResult.CompiledClasses> {
    override fun data(): ReplCompileResult.CompiledClasses = compiledClasses
}
