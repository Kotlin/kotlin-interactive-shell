package sparklin.kshell.plugins

import sparklin.kshell.console.ConsoleReader
import sparklin.kshell.BaseCommand
import sparklin.kshell.Plugin
import sparklin.kshell.Repl
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.match

class HelpPlugin: Plugin {
    inner class Help(fullName: String, shortName: String, description: String):
            BaseCommand(fullName, shortName, description) {

        override val params = "[command]"

        override fun execute(line: String) {
            val args = line.split(' ')
            val commands = repl.listCommands()

            repl.apply {
                if (args.size == 1) {
                    val help = commands.joinToString(separator = "\n") { it.desc() }
                    console.println(help)
                } else {
                    val command = args[1]
                    try {
                        val res = commands.first { it.match(":$command") }
                        console.println(res.help())
                    } catch (_: NoSuchElementException) {
                        console.println("$command: no such command. Type :help for help.")
                    }
                }
            }
        }
    }

    lateinit var repl: Repl
    lateinit var console: ConsoleReader

    override fun init(repl: Repl, config: Configuration) {
        this.repl = repl
        this.console = config.getConsoleReader()

        repl.registerCommand(Help("help", "h", "print this summary or command-specific help"))
    }

    override fun cleanUp() { }
}