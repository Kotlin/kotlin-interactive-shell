package kshell.command

import kshell.Command

open class Help : Command("help", "h", "print this summary or command-specific help", params = "[command]") {
    override fun execute(line: String) {
        val args = line.split(' ')

        repl!!.apply {
            if (args.size == 1) {
                val help = commands.map(Command::toString).joinToString(separator = "\n")
                reader.println(help)
            } else {
                val command = args[1]
                val res = commands.filter { it.match(":$command") }
                if (res.isEmpty()) {
                    reader.println("$command: no such command.  Type :help for help.")
                } else {
                    reader.println(res[0].signature() + res[0].help())
                }
            }
        }
    }
}