package sparklin.kshell.console

import sparklin.kshell.configuration.Configuration

interface ConsoleReader {
    fun init(config: Configuration)

    fun addCompleter(completer: Completer)

    fun setPrompt(prompt: String)

    fun readLine(): String?

    fun println(s: String)

    fun cleanUp()
}