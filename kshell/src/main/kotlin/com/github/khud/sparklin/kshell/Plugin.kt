package com.github.khud.sparklin.kshell

import com.github.khud.sparklin.kshell.configuration.Configuration

interface Plugin {
    fun init(repl: KShell, config: Configuration)

    fun cleanUp()

    fun sayHello() { }
}