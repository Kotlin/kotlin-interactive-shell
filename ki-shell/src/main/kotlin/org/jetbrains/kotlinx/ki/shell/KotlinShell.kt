package org.jetbrains.kotlinx.ki.shell

import kotlinx.cli.CommandLineException
import kotlinx.cli.CommandLineInterface
import kotlinx.cli.HelpPrintedException
import kotlinx.cli.flagArgument
import kotlinx.cli.flagValueAction
import kotlinx.cli.flagValueArgument
import kotlinx.cli.parse
import org.jetbrains.kotlinx.ki.completion.KiCompletionPluginRegistrar
import org.jetbrains.kotlinx.ki.shell.configuration.CachedInstance
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.system.exitProcess

class KotlinShellOptions(args: Array<out String>) {
    private val cli = CommandLineInterface(
        "ki",
        printHelpByDefault = false,
        longTagPrefixes = listOf("--"),
        longTagValueDelimiter = "="
    )

    val version by cli.flagArgument("--version", "Print version")

    val script: String? by cli.flagValueArgument(listOf("-f", "--file"), "<path>", "Run the script file without a terminal and exit")

    val classpath = mutableListOf<File>()

    val compilerOptions = mutableListOf<String>()

    init {
        cli.flagValueAction(listOf("-cp", "-classpath", "--classpath"), "<path>", "Add jars and directories, separated by '${File.pathSeparator}', to the classpath") {
            it.split(File.pathSeparatorChar).filter(String::isNotBlank).mapTo(classpath, ::File)
        }
        cli.flagValueAction("--compiler-option", "<option>", "Pass the option to the compiler, e.g. -opt-in=kotlin.RequiresOptIn; repeatable") {
            compilerOptions.add(it)
        }
        cli.parse(args)
    }
}

object KotlinShell {
    @JvmStatic
    fun main(args: Array<String>) {
        val exitCode = run(args)
        if (exitCode != 0) exitProcess(exitCode)
    }

    fun run(args: Array<out String>): Int {
        val options = try {
            KotlinShellOptions(args)
        } catch (_: HelpPrintedException) {
            return 0
        } catch (_: CommandLineException) {
            // the parser has printed the error and the usage
            return 2
        }

        if (options.version) {
            printVersion()
            return 0
        }

        val missingClasspath = options.classpath.filterNot(File::exists)
        if (missingClasspath.isNotEmpty()) {
            System.err.println("Classpath entries not found: ${missingClasspath.joinToString()}")
            return 2
        }

        val script = options.script?.let(::File)
        if (script != null && !script.isFile) {
            System.err.println("Script file not found: ${options.script}")
            return 2
        }

        val repl = createShell(configuration(), options.classpath, options.compilerOptions)

        if (script != null) {
            return try {
                repl.runScript(script)
            } finally {
                repl.cleanUp()
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            println("\nBye!")
            repl.cleanUp()
        })

        repl.doRun()
        return 0
    }

    fun createShell(
        configuration: ReplConfiguration,
        classpath: List<File> = emptyList(),
        compilerOptions: List<String> = emptyList(),
    ): Shell =
        Shell(
            configuration,
            defaultJvmScriptingHostConfiguration,
            ScriptCompilationConfiguration {
                jvm {
                    dependenciesFromClassloader(
                        classLoader = KotlinShell::class.java.classLoader,
                        wholeClasspath = true
                    )
                }
                if (classpath.isNotEmpty()) {
                    dependencies.append(JvmDependency(classpath))
                }
                if (compilerOptions.isNotEmpty()) {
                    this.compilerOptions.append(withCompletionPlugin(compilerOptions))
                }
            },
            ScriptEvaluationConfiguration {
                jvm {
                    baseClassLoader(Shell::class.java.classLoader)
                }
            }
        )

    // The compiler loads the plugins found on its classpath, including the KI one, only if no plugin is passed explicitly
    internal fun withCompletionPlugin(compilerOptions: List<String>): List<String> {
        val hasPlugins = compilerOptions.any { it.startsWith("-Xplugin") || it.startsWith("-Xcompiler-plugin") }
        if (!hasPlugins) return compilerOptions
        return compilerOptions + "-Xplugin=${completionPluginLocation().path}"
    }

    // The shaded ki-shell.jar also registers the scripting compiler plugin, so the plugin jar next to it is passed instead
    internal fun completionPluginLocation(
        classesLocation: File = File(KiCompletionPluginRegistrar::class.java.protectionDomain.codeSource.location.toURI())
    ): File {
        if (classesLocation.isDirectory || classesLocation.name.startsWith(COMPLETION_PLUGIN_NAME)) return classesLocation
        return File(classesLocation.parentFile, "$COMPLETION_PLUGIN_NAME.jar").takeIf { it.isFile } ?: classesLocation
    }

    private const val COMPLETION_PLUGIN_NAME = "ki-completion-plugin"

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
