package sparklin.kshell

interface Command {
    fun execute(line: String)

    val name: String

    val short: String?

    fun help(): String

    fun desc(): String
}

fun Command.match(line: String): Boolean {
    val ind = line.indexOf(' ')
    val command = if (ind < 0) line else line.substring(0, ind)
    return (short != null && command.equals(":$short", ignoreCase = true)) ||
            command.equals(":$name", ignoreCase = true)
}