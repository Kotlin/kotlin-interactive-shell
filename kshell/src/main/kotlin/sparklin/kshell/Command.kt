package sparklin.kshell

import sparklin.kshell.org.jline.reader.Highlighter

interface Command {
    fun execute(line: String)

    val name: String

    val short: String?

    fun help(): String

    fun desc(): String

    fun highlighter(): Highlighter? = null
}

fun Command.match(line: String): Boolean {
    val ind = line.indexOf(' ')
    val command = if (ind < 0) line else line.substring(0, ind)
    return (short != null && command.equals(":$short", ignoreCase = true)) ||
            command.equals(":$name", ignoreCase = true)
}

fun Command.weakMatch(line: String): Boolean {
    val ind = line.indexOf(' ')
    val command = if (ind < 0) line else line.substring(0, ind)
    return (short != null && command.startsWith(":$short", ignoreCase = true)) ||
            command.startsWith(":$name", ignoreCase = true)
}