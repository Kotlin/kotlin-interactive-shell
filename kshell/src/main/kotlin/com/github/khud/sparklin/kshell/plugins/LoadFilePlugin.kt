package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.BaseCommand
import com.github.khud.sparklin.kshell.KShell
import com.github.khud.sparklin.kshell.configuration.ReplConfiguration
import com.github.khud.sparklin.kshell.Plugin
import java.io.File

class LoadFilePlugin: Plugin {
    inner class Load(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "load")
        override val short: String by conf.get(default = "l")
        override val description: String = "load file and evaluate"

        override val params = "<path>"

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val path = line.substring(p + 1).trim()
            val content = File(path).readText()
            repl.eval(content)
        }

    }

    lateinit var repl: KShell

    override fun init(repl: KShell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(Load(config))
    }

    override fun cleanUp() { }
}