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
            console.println("// Entering paste mode (ctrl-D to finish)")
            val buf = StringBuilder()
            while(true) {
                val s = console.readLine()
                if (s == null) {
                    break
                } else {
                    buf.append(s)
                    buf.append('\n')
                }
            }
            println(buf.toString())
            console.println("// Exiting paste mode, now interpreting.")
        }
    }

    lateinit var repl: Repl
    lateinit var console: ConsoleReader

    override fun init(repl: Repl, config: Configuration) {
        this.repl = repl
        this.console = getConsoleReader(config)

        repl.registerCommand(Paste("paste", "p", "enter paste mode"))
    }

    override fun cleanUp() { }

    private fun getConsoleReader(config: Configuration): ConsoleReader {
        val klassName = config.get("console.class","sparklin.kshell.console.jline2.ConsoleReaderImpl")
        val reader = CachedInstance<ConsoleReader>().
                load(klassName, ConsoleReader::class)
        reader.init(config)
        return reader
    }
}