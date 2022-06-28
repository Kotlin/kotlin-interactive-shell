package org.jetbrains.kotlinx.ki.shell

import kotlinx.cli.CommandLineInterface
import kotlinx.cli.HelpPrintedException
import kotlinx.cli.flagArgument
import kotlinx.cli.parse
import org.jetbrains.kotlinx.ki.shell.configuration.CachedInstance
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm

object KotlinShell {
    private val cli = CommandLineInterface("ki", printHelpByDefault = false)
    private val version by cli.flagArgument("--version", "Print version")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            cli.parse(args)
        }catch (e:HelpPrintedException){
            return
        }

        if (version) {
            printVersion()
            return
        }

        val repl = Shell(
            configuration(),
            defaultJvmScriptingHostConfiguration,
            ScriptCompilationConfiguration {
                jvm {
                    dependenciesFromClassloader(
                        classLoader = KotlinShell::class.java.classLoader,
                        wholeClasspath = true
                    )
                }
            },
            ScriptEvaluationConfiguration {
                jvm {
                    baseClassLoader(Shell::class.java.classLoader)
                }
            }
        )

        Runtime.getRuntime().addShutdownHook(Thread {
            println("\nBye!")
            repl.cleanUp()
        })

        repl.doRun()
    }

    fun configuration(): ReplConfiguration {
        val instance = CachedInstance<ReplConfiguration>()
        val klassName: String? = System.getProperty("config.class")

        return if (klassName != null) {
            instance.load(klassName, ReplConfiguration::class)
        } else {
            instance.get { object : ReplConfigurationBase() {}  }
        }
    }
}