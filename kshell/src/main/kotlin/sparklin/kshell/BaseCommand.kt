package sparklin.kshell

import sparklin.kshell.console.Completer

abstract class BaseCommand(override val name: String,
                           override val short: String?,
                           private val description: String): sparklin.kshell.Command {

    open val params = ""

    override fun help(): String = "(no extended help available)"

    override fun desc(): String {
        val shortPhrase = if (short != null) "or $short " else ""
        return String.format("%-30s %s",  ":$name $shortPhrase$params", description)
    }

    override fun completer(): Completer = Completer.DEFAULT_COMPLETER
}