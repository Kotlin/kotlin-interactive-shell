package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.BaseCommand
import com.github.khud.sparklin.kshell.KShell
import com.github.khud.sparklin.kshell.Plugin
import com.github.khud.sparklin.kshell.configuration.Configuration
import com.github.khud.sparklin.kshell.globToRegex
import kotlinx.cli.CommandLineInterface
import kotlinx.cli.flagArgument
import kotlinx.cli.parse
import kotlinx.cli.positionalArgument

class ConfigPlugin: Plugin {
    inner class Set(val conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "set")
        override val short: String? by conf.getNullable()
        override val description: String = "set configuration parameter"

        override fun execute(line: String) {
            val args = line.split(' ')
            val (k, v) = args[1].split('=')
            val params = conf.list().filter { it.endsWith(k) }
            if (params.size > 1) {
                println("Please specify configuration parameter more precisely, found ${params.size}:")
                params.forEach { println(it) }
            } else {
                val key = params.first()
                try {
                    conf.set(key, v)
                } catch (e: Exception) {
                    println(e)
                }
            }
        }
    }

    inner class Conf(val conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "conf")
        override val short: String? by conf.getNullable()
        override val description: String = "list configuration parameters"

        private val cli = CommandLineInterface("conf", printHelpByDefault = false)
        private val group by cli.flagArgument("-g", "Groups parameters")
        private val withValues by cli.flagArgument("-v", "Prints with values")
        private val glob by cli.positionalArgument("GLOB", "Glob pattern to match parameter (i.e. *.name)", minArgs = 0)

        override fun execute(line: String) {
            val args = line.split(' ').drop(1)
            try {
                cli.parse(args)
            } catch (e: Exception) {
                println(e)
                return
            }
            val regex = glob?.let { globToRegex(it).toRegex() }
            val params = if (regex == null) conf.list() else conf.list().filter { regex.matches(it) }
            params.forEach { if (withValues) println("$it=${conf.getTouched(it)}") else println(it) }
        }

        override fun help(): String {
            val printer = HelpPlugin.StringHelpPrinter()
            cli.printHelp(printer)
            return printer.toString()
        }
    }

    override fun init(repl: KShell, config: Configuration) {
        repl.registerCommand(Set(config))
        repl.registerCommand(Conf(config))
    }

    override fun cleanUp() {

    }
}