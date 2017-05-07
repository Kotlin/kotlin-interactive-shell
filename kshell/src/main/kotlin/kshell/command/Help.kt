package kshell.command

import kshell.Command

/**
 * Created by vitaly.khudobakhshov on 05/05/17.
 */
open class Help : Command("help", "h", "print this summary or command-specific help", params = "[command]") {
    override fun execute(line: String) {
        val repl = this.repl!!
        val args = line.split(' ')
        if (args.size == 1) {
            val help = repl.commands.map(Command::toString).joinToString(separator = "\n")
            repl.reader.println(help)
        } else {
            val command = args[1]
            val res = repl.commands.filter { it.match(":$command") }
            if (res.isEmpty()) {
                repl.reader.println("$command: no such command.  Type :help for help.")
            } else {
                repl.reader.println(res[0].signature() + res[0].help())
            }
        }
    }
}