package org.jetbrains.kotlinx.ki.shell.plugins

import kotlinx.cli.*
import org.jetbrains.kotlinx.ki.shell.*
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration

class ConfigPlugin: Plugin {
    inner class Set(val conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "set")
        override val short: String? by conf.getNullable()
        override val description: String = "set configuration parameter"

        override fun execute(line: String): Command.Result {
            val args = smartSplit(line)
            val (k, v) = args.drop(1)
            val params = conf.list().filter { it.endsWith(k) }
            if (params.size > 1) {
                return Command.Result.Failure(
                    "Please specify configuration parameter more precisely, found ${params.size}:\n${params.joinToString("\n")}"
                )
            } else {
                val key = params.first()
                try {
                    conf.set(key, v)
                    return Command.Result.Success("$key is set to $v")
                } catch (e: Exception) {
                    return Command.Result.Failure(e.toString())
                }
            }
        }
    }

    inner class Conf(val conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "conf")
        override val short: String? by conf.getNullable()
        override val description: String = "list configuration parameters"

        private val cli = CommandLineInterface("conf", printHelpByDefault = false)
        private val group by cli.flagArgument("-g", "Groups by class")
        private val withValues by cli.flagArgument("-v", "Prints with values")
        private val glob by cli.positionalArgument("GLOB", "Glob pattern to match parameter (i.e. *.name)", minArgs = 0)

        override fun execute(line: String): Command.Result {
            val args = line.split(' ').drop(1)
            try {
                cli.parse(args)
            } catch (e: HelpPrintedException) {
                return Command.Result.Success()
            } catch (e: Exception) {
                return Command.Result.Failure(e.toString())
            }
            val regex = glob?.let { globToRegex(it).toRegex() }
            val params = if (regex == null) conf.list() else conf.list().filter { regex.matches(it) }
            if (group) {
                groupByClass(params).forEach { (groupName, list) ->
                    if (groupName != null) println("[$groupName]")
                    list.forEach {
                        val realName = if (groupName != null) "$groupName.$it" else it
                        if (withValues) println("$it=${conf.getTouched(realName)}") else println(it)
                    }
                }
            } else {
                params.forEach { if (withValues) println("$it=${conf.getTouched(it)}") else println(it) }
            }
            return Command.Result.Success()
        }

        override fun help(): String {
            val printer = HelpPlugin.StringHelpPrinter()
            cli.printHelp(printer)
            return printer.toString()
        }
    }

    override fun init(repl: Shell, config: ReplConfiguration) {
        repl.registerCommand(Set(config))
        repl.registerCommand(Conf(config))
    }

    override fun cleanUp() {

    }

    companion object {
        fun groupByClass(iterable: Iterable<String>): Map<String?, List<String>> =
            iterable.groupBy {
                val p = it.lastIndexOf('.')
                if (p < 0) null else it.substring(0, p)
            }.mapValues {
                it.value.map { v ->
                    val p = v.lastIndexOf('.')
                    if (p < 0) v else v.substring(p + 1)
                }
            }
    }
}