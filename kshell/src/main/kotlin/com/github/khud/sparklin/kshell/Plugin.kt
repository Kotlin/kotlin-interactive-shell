package com.github.khud.sparklin.kshell

import com.github.khud.sparklin.kshell.configuration.ReplConfiguration

interface Plugin {
    fun init(repl: KShell, config: ReplConfiguration)

    fun cleanUp()

    fun sayHello() { }
}