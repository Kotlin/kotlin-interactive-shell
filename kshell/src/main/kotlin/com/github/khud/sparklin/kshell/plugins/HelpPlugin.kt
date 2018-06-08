package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.BaseCommand
import com.github.khud.sparklin.kshell.Plugin
import com.github.khud.sparklin.kshell.KShell
import com.github.khud.sparklin.kshell.configuration.Configuration
import com.github.khud.sparklin.kshell.match

class HelpPlugin: Plugin {
    inner class Help(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "help")
        override val short: String by conf.get(default = "h")
        override val description: String = "print this summary or command-specific help"

        override val params = "[command]"

        override fun execute(line: String) {
            val args = line.split(' ')
            val commands = repl.listCommands()

            repl.apply {
                if (args.size == 1) {
                    val help = commands.joinToString(separator = "\n") { it.desc() }
                    println(help)
                } else {
                    val command = args[1]
                    try {
                        val res = commands.first { it.match(":$command") }
                        println(res.help())
                    } catch (_: NoSuchElementException) {
                        println("$command: no such command. Type :help for help.")
                    }
                }
            }
        }
    }

    lateinit var repl: KShell

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl

        repl.registerCommand(Help(config))
    }

    override fun cleanUp() { }
}