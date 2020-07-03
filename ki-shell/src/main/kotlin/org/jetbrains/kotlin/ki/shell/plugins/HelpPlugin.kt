package org.jetbrains.kotlin.ki.shell.plugins

import kotlinx.cli.HelpPrinter
import org.jetbrains.kotlin.ki.shell.BaseCommand
import org.jetbrains.kotlin.ki.shell.Shell
import org.jetbrains.kotlin.ki.shell.Plugin
import org.jetbrains.kotlin.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlin.ki.shell.match

class HelpPlugin: Plugin {
    inner class Help(conf: ReplConfiguration): BaseCommand() {
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

    lateinit var repl: Shell

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(Help(config))
    }

    override fun cleanUp() { }

    override fun sayHello() {
        println("type :h for help")
    }

    class StringHelpPrinter(private val syntaxWidth: Int = 24): HelpPrinter {
        private val sb = StringBuilder()

        override fun printText(text: String) {
            sb.appendln(text)
        }

        override fun printSeparator() {
            sb.appendln()
        }

        override fun printEntry(helpEntry: String, description: String) {
            if (helpEntry.length <= syntaxWidth) {
                sb.appendln("  ${helpEntry.padEnd(syntaxWidth)}  $description")
            }
            else {
                sb.appendln("  $helpEntry")
                sb.appendln("  ${"".padEnd(syntaxWidth)}  $description")
            }
        }

        override fun toString(): String = sb.toString()
    }
}