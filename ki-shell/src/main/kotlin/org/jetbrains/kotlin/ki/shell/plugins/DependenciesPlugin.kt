package org.jetbrains.kotlin.ki.shell.plugins

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.ki.shell.BaseCommand
import org.jetbrains.kotlin.ki.shell.Command
import org.jetbrains.kotlin.ki.shell.Plugin
import org.jetbrains.kotlin.ki.shell.Shell
import org.jetbrains.kotlin.ki.shell.configuration.ReplConfiguration
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

class DependenciesPlugin : Plugin {

    inner class DependsOnCommand(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "dependsOn")
        override val short: String? by conf.getNullable()
        override val description: String = "Load dependency from the current set of repositories"

        override val params = "<artifact coordinates>"

        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            val coords = line.substring(p + 1).trim()
            return Command.Result.RunSnippets(listOf("@file:DependsOn(\"$coords\")"))
        }
    }

    inner class RepositoryCommand(conf: ReplConfiguration): BaseCommand() {
        override val name: String by conf.get(default = "repository")
        override val short: String? by conf.getNullable()
        override val description: String = "Add repository for the artifacts lookup"

        override val params = "<repository coordinates>"

        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            val repo = line.substring(p + 1).trim()
            return Command.Result.RunSnippets(listOf("@file:Repository(\"$repo\")"))
        }
    }

    lateinit var repl: Shell

    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

    fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
                ?: return context.compilationConfiguration.asSuccess()
        return runBlocking {
            resolver.resolveFromScriptSourceAnnotations(annotations)
        }.onSuccess {
            context.compilationConfiguration.with {
                dependencies.append(JvmDependency(it))
            }.asSuccess()
        }
    }

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(DependsOnCommand(config))
        repl.registerCommand(RepositoryCommand(config))

        val dependenciesClasspath = JvmDependency(
                scriptCompilationClasspathFromContext(
                        "kotlin-scripting-dependencies" // DependsOn and Repository annotations are taken from it
                )
        )
        repl.updateHostConfiguration {
            configurationDependencies.append(dependenciesClasspath)
        }
        repl.updateCompilationConfiguration {
            defaultImports(DependsOn::class, Repository::class)
            dependencies.append(dependenciesClasspath)
//            jvm {
//                dependenciesFromCurrentContext(
//                        "kotlin-scripting-dependencies" // DependsOn and Repository annotations are taken from it
//                )
//            }
            refineConfiguration {
                onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
            }
        }
    }

    override fun cleanUp() {}
}
