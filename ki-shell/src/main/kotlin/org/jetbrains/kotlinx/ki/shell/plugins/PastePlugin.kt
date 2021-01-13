package org.jetbrains.kotlinx.ki.shell.plugins

import org.jetbrains.kotlinx.ki.shell.BaseCommand
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader

class PastePlugin : Plugin {
    inner class Paste(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "paste")
        override val short: String by conf.get(default = "p")
        override val description: String = "enter paste mode"

        override fun execute(line: String): Command.Result {
            println("// Entering paste mode (ctrl-D to finish)")
            val buf = StringBuilder()
            var lineCount = 0
            try {
                while (true) {
                    val s = pasteConsole.readLine("")
                    buf.appendLine(s)
                    lineCount++
                }
            } catch (e: EndOfFileException) { }
            val code = buf.toString()
            println("// Exiting paste mode, now interpreting.")
            return Command.Result.RunSnippets(listOf(code))
        }
    }

    lateinit var repl: Shell
    lateinit var console: LineReader
    lateinit var pasteConsole: LineReader

    override fun init(repl: Shell, config: ReplConfiguration) {
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