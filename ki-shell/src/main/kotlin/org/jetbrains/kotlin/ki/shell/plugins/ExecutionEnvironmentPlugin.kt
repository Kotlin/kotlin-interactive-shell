package org.jetbrains.kotlin.ki.shell.plugins

import org.jetbrains.kotlin.ki.shell.BaseCommand
import org.jetbrains.kotlin.ki.shell.Command
import org.jetbrains.kotlin.ki.shell.Plugin
import org.jetbrains.kotlin.ki.shell.Shell
import org.jetbrains.kotlin.ki.shell.configuration.ReplConfiguration
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.jvm.JvmDependency

class ExecutionEnvironmentPlugin : Plugin {
    inner class ClassPath(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "classpath")
        override val short: String by conf.get(default = "cp")
        override val description: String = "Show current script compilation classpath"

        override fun execute(line: String): Command.Result {
            val cp = repl.compilationConfiguration[ScriptCompilationConfiguration.dependencies]?.flatMap {
                if (it is JvmDependency) it.classpath else emptyList()
            }
            return Command.Result.Success(cp?.joinToString("\n"))
        }

    }

    lateinit var repl: Shell

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(ClassPath(config))
    }

    override fun cleanUp() { }
}