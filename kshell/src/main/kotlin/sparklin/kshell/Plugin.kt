package sparklin.kshell

import sparklin.kshell.configuration.Configuration

interface Plugin {
    fun init(repl: KShell, config: Configuration)

    fun cleanUp()
}