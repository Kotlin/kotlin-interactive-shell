package sparklin.kshell.plugins

import sparklin.kshell.org.jline.reader.EndOfFileException
import sparklin.kshell.org.jline.reader.LineReader
import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.repl.EvalError
import sparklin.kshell.repl.Result

class PastePlugin : Plugin {
    inner class Paste(conf: Configuration): BaseCommand() {
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
                    buf.append(s)
                    buf.append('\n')
                    lineCount++
                }
            } catch (e: EndOfFileException) { }
            val code = buf.toString()
//            console.addHistoryItem(source)
            println("// Exiting paste mode, now interpreting.")
            val result = repl.eval(code).result
            when (result) {
                is Result.Error -> repl.handleError(result.error)
                is Result.Success -> repl.handleSuccess(result.data)
            }
        }
    }

    lateinit var repl: KShell
    lateinit var console: LineReader
    lateinit var pasteConsole: LineReader

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl
        this.console = repl.reader
        this.pasteConsole = repl.readerBuilder.build()

        repl.registerCommand(Paste(config))
    }

    override fun cleanUp() { }
}