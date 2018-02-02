package sparklin.kshell

import sparklin.kshell.configuration.Configuration

interface Plugin {
    fun init(repl: Repl, config: Configuration)

    fun cleanUp()
}