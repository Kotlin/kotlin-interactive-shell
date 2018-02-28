package sparklin.kshell

import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import java.io.File
import java.util.*
import kotlin.reflect.KClass

interface Repl {
    fun addClasspathRoots(files: List<File>)

    fun addImports(imports: List<String>)

    fun compile(code: String): CompileResult

    fun compileAndEval(line: String)

    fun registerCommand(command: sparklin.kshell.Command)

    fun extensionPoint(body: () -> Unit)

    fun compileError(result: CompileResult.Error)

    var wrapper: InvokeWrapper

    fun listCommands(): Iterable<sparklin.kshell.Command>
}


interface CompileResult {
    interface ClassData {
        val path: String
        val bytes: ByteArray
    }

    interface CompiledClasses : CompileResult {
        val mainClassName: String
        val classes: List<ClassData>
        val hasResult: Boolean
        val type: String?
    }

    object Incomplete : CompileResult

    interface Error : CompileResult {
        val message: String
    }
}

class OnCompile(private val compiledClasses: CompileResult.CompiledClasses) : Event<CompileResult.CompiledClasses> {
    override fun data(): CompileResult.CompiledClasses = compiledClasses
}
