package org.jetbrains.kotlinx.ki.shell

abstract class BaseCommand: Command {
    abstract val description: String

    open val params = ""

    override fun help(): String = "(no extended help available)"

    override fun desc(): String {
        val shortPhrase = if (short != null) "or $short " else ""
        return String.format("%-30s %s",  ":$name $shortPhrase$params", description)
    }
}