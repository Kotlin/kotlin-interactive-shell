package org.jetbrains.kotlin.ki.shell

import org.jetbrains.kotlin.ki.shell.configuration.ReplConfiguration

interface Plugin {
    fun init(repl: Shell, config: ReplConfiguration)

    fun cleanUp()

    fun sayHello() { }
}