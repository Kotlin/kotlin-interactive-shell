package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.Plugin
import sparklin.kshell.Repl
import sparklin.kshell.configuration.CachedInstance
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.console.ConsoleReader

class PastePlugin : Plugin {
    inner class Paste(name: String, short: String, description: String):
            BaseCommand(name, short, description) {
        override fun execute(line: String) {
            println("// Entering paste mode (ctrl-D to finish)")
            val buf = StringBuilder()
            var lineCount = 0
            while(true) {
                val s = pasteConsole.readLine()
                if (s == null) {
                    break
                } else {
                    buf.append(s)
                    buf.append('\n')
                    lineCount ++
                }
            }
            val code = buf.toString()
            console.addHistoryItem(code)
            println("// Exiting paste mode, now interpreting.")
            repl.compileAndEval(code)
        }
    }

    lateinit var repl: Repl
    lateinit var console: ConsoleReader
    lateinit var pasteConsole: ConsoleReader

    override fun init(repl: Repl, config: Configuration) {
        this.repl = repl
        this.console = config.getConsoleReader()
        this.pasteConsole = getPasteConsoleReader(config)

        repl.registerCommand(Paste("paste", "p", "enter paste mode"))
    }

    override fun cleanUp() { }

    private fun getPasteConsoleReader(config: Configuration): ConsoleReader {
        val klassName = config.get("console.class","sparklin.kshell.console.jline2.ConsoleReaderImpl")
        val reader = CachedInstance<ConsoleReader>().
                load(klassName, ConsoleReader::class)
        reader.init(config)
        return reader
    }
}