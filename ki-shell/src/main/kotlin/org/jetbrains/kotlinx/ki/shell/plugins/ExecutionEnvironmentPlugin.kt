package org.jetbrains.kotlinx.ki.shell.plugins

import org.jetbrains.kotlinx.ki.shell.BaseCommand
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import java.io.File
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.RefineConfigurationOnAnnotationsData
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.refineConfigurationOnAnnotations
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.JvmDependency

/**
 * Compiler options for the snippet, e.g. `@file:CompilerOptions("-opt-in=kotlin.RequiresOptIn")`, as in `.main.kts` scripts.
 */
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class CompilerOptions(vararg val options: String)

class ExecutionEnvironmentPlugin : Plugin {
    inner class ClassPath(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "classpath")
        override val short: String by conf.get(default = "cp")
        override val description: String = "Show current script compilation classpath, or add jars and directories to it"

        override val params = "[add <path>]"

        override fun execute(line: String): Command.Result {
            val args = line.trim().split(Regex("\\s+"), limit = 3).drop(1)
            return when {
                args.isEmpty() -> show()
                args[0] == "add" && args.size == 2 -> add(args[1])
                else -> Command.Result.Failure("expected no arguments or 'add <path>'")
            }
        }

        private fun show(): Command.Result {
            val cp = repl.compilationConfiguration[ScriptCompilationConfiguration.dependencies]?.flatMap {
                if (it is JvmDependency) it.classpath else emptyList()
            }
            return Command.Result.Success(cp?.joinToString("\n"))
        }

        private fun add(paths: String): Command.Result {
            val files = paths.split(File.pathSeparatorChar).filter(String::isNotBlank).map { File(it.trim()) }
            val missing = files.filterNot(File::exists)
            if (missing.isNotEmpty()) return Command.Result.Failure("not found: ${missing.joinToString()}")
            repl.addClasspath(files)
            return Command.Result.Success()
        }
    }

    lateinit var repl: Shell

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(ClassPath(config))

        repl.updateCompilationConfiguration {
            defaultImports.append(CompilerOptions::class.qualifiedName!!)
            // refineConfiguration { onAnnotations(...) } would replace the handlers registered by the other plugins
            refineConfigurationOnAnnotations.append(
                RefineConfigurationOnAnnotationsData(listOf(KotlinType(CompilerOptions::class)), ::configureCompilerOptions)
            )
        }
    }

    override fun cleanUp() { }
}

// The compiler applies the options of the refined configuration to this snippet only
private fun configureCompilerOptions(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val options = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)
        ?.mapNotNull { it.annotation as? CompilerOptions }
        ?.flatMap { it.options.asList() }
        ?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return context.compilationConfiguration.with {
        compilerOptions.append(options)
    }.asSuccess()
}
