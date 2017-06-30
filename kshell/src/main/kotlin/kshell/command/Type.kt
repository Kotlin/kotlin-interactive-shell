package kshell.command

import kshell.Command
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import kotlin.concurrent.write

class Type : Command("type", "t", "display the type of an expression without evaluating it", params = "<expr>") {
    override fun execute(line: String) {
        val p = line.indexOf(' ')
        val expr = line.substring(p + 1).trim()

        repl!!.apply {
            state.lock.write {
                val replCodeLine = ReplCodeLine(nextLine.incrementAndGet(), state.currentGeneration, expr)
                val compileResult = engine.compile(state, replCodeLine)
                when (compileResult) {
                    is ReplCompileResult.Incomplete ->
                        reader.print("Incomplete line")
                    is ReplCompileResult.Error ->
                        compileError(compileResult)
                    is ReplCompileResult.CompiledClasses -> {
                        reader.println(clarifyType(compileResult.type))
                    }
                }
                alterState(state)
            }
        }
    }
}