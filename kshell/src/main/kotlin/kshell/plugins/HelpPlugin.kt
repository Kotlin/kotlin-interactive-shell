package kshell.plugins

import kshell.console.ConsoleReader
import kshell.BaseCommand
import kshell.Plugin
import kshell.Repl
import kshell.configuration.Configuration

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
                    val res = commands.filter { it.match(":$command") }
                    if (res.isEmpty()) {
                        console.println("$command: no such command.  Type :help for help.")
                    } else {
                        console.println(res[0].help())
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