package org.jetbrains.kotlinx.ki.shell.plugins

import org.jetbrains.kotlinx.ki.shell.BaseCommand
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import java.io.File

class LoadFilePlugin: Plugin {
    inner class Load(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "load")
        override val short: String by conf.get(default = "l")
        override val description: String = "load file and evaluate"

        override val params = "<path>"

        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            val path = line.substring(p + 1).trim()
            val content = File(path).readText()
            return Command.Result.RunSnippets(listOf(content))
        }

    }

    lateinit var repl: Shell

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(Load(config))
    }

    override fun cleanUp() { }
}