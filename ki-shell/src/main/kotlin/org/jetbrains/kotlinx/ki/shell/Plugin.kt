package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration

interface Plugin {
    fun init(repl: Shell, config: ReplConfiguration)

    fun cleanUp()

    fun sayHello() { }
}