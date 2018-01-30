package kshell

import org.jetbrains.kotlin.cli.common.repl.InvokeWrapper
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult

interface Repl {
    fun compile(code: String): ReplCompileResult

    fun compileAndEval(line: String)

    fun registerCommand(command: Command)

    fun extensionPoint(body: () -> Unit)

    fun compileError(result: ReplCompileResult.Error)

    var wrapper: InvokeWrapper

    fun getLastCompiledClasses(): ReplCompileResult.CompiledClasses?

    fun listCommands(): Iterable<Command>
}
