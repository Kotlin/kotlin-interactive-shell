package com.github.khud.sparklin.kshell.plugins

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import com.github.khud.sparklin.kshell.BaseCommand
import com.github.khud.sparklin.kshell.KShell
import com.github.khud.sparklin.kshell.Plugin
import com.github.khud.sparklin.kshell.configuration.ReplConfiguration
import com.github.khud.sparklin.kshell.wrappers.ResultWrapper
import kotlin.script.experimental.api.ResultWithDiagnostics

class PastePlugin : Plugin {
    inner class Paste(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "paste")
        override val short: String by conf.get(default = "p")
        override val description: String = "enter paste mode"

        override fun execute(line: String) {
            println("// Entering paste mode (ctrl-D to finish)")
            val buf = StringBuilder()
            var lineCount = 0
            try {
                while (true) {
                    val s = pasteConsole.readLine("")
                    buf.appendln(s)
                    lineCount++
                }
            } catch (e: EndOfFileException) { }
            val code = buf.toString()
            println("// Exiting paste mode, now interpreting.")
            val time = System.nanoTime()
            val result = repl.eval(code)
            repl.evaluationTimeMillis = (System.nanoTime() - time) / 1_000_000
            when (result.getStatus()) {
                ResultWrapper.Status.ERROR -> repl.handleError(result.result)
                ResultWrapper.Status.SUCCESS -> repl.handleSuccess(result.result as ResultWithDiagnostics.Success<*>)
            }
        }
    }

    lateinit var repl: KShell
    lateinit var console: LineReader
    lateinit var pasteConsole: LineReader

    override fun init(repl: KShell, config: ReplConfiguration) {
        this.repl = repl
        this.console = repl.reader
        this.pasteConsole = repl.readerBuilder.highlighter(console.highlighter).build()

        pasteConsole.option(LineReader.Option.DISABLE_HIGHLIGHTER, console.isSet(LineReader.Option.DISABLE_HIGHLIGHTER))
        pasteConsole.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
        pasteConsole.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "")

        repl.registerCommand(Paste(config))
    }

    override fun cleanUp() { }
}