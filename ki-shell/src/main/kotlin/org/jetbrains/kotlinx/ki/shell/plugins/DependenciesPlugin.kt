package org.jetbrains.kotlinx.ki.shell.plugins

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlinx.ki.shell.BaseCommand
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import java.io.File
import java.util.Properties
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency

data class RepoCredentials(val username: String, val password: String)

private const val CREDENTIALS_USERNAME_PROPERTY = "username"
private const val CREDENTIALS_PASSWORD_PROPERTY = "password"
private const val MANDATORY_REPO_COMMAND_ARGS = 2
private const val OPTIONAL_REPO_COMMAND_ARGS = 2

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

        private val emptyRepoCredentials = RepoCredentials("", "")

        override fun execute(line: String): Command.Result {
            val args = line.split(' ')

            return Command.Result.RunSnippets(buildSnippet(args))
        }

        private fun buildSnippet(args: List<String>): List<String> {
            val repo = if (args.size > 1) args[1].trim() else ""

            if (args.size <= MANDATORY_REPO_COMMAND_ARGS) {
                return listOf("@file:Repository(\"$repo\")")
            }

            val (username, password) = getCredentials(
                args.drop(MANDATORY_REPO_COMMAND_ARGS).take(OPTIONAL_REPO_COMMAND_ARGS)
            )

            return listOf("@file:Repository(\"$repo\", options = [\"username=$username\", \"password=$password\"])")
        }

        private fun getCredentials(args: List<String>): RepoCredentials {
            return when (args.size) {
                2 -> buildCredentialsFromUsernameAndPassword(args)
                1 -> buildCredentialsFromFile(args)
                else -> emptyRepoCredentials
            }
        }

        private fun buildCredentialsFromUsernameAndPassword(args: List<String>): RepoCredentials {
            val props = Properties()

            return args.joinToString("\n").reader().use { reader ->
                props.load(reader)

                RepoCredentials(
                    props.getProperty(CREDENTIALS_USERNAME_PROPERTY, ""),
                    props.getProperty(CREDENTIALS_PASSWORD_PROPERTY, "")
                )
            }
        }

        private fun buildCredentialsFromFile(args: List<String>): RepoCredentials {
            val path = args[0]
            val credFile = File(path)

            return when {
                !credFile.exists() -> {
                    println("File $path not found.")
                    emptyRepoCredentials
                }
                !credFile.canRead() -> {
                    println("Cannot open $path.")
                    emptyRepoCredentials
                }
                else -> {
                    try {
                        credFile.reader().use { reader ->
                            val props = Properties()
                            props.load(reader)

                            RepoCredentials(
                                props.getProperty(CREDENTIALS_USERNAME_PROPERTY, ""),
                                props.getProperty(CREDENTIALS_PASSWORD_PROPERTY, "")
                            )
                        }
                    } catch (e: Exception) {
                        println(e.message)
                        emptyRepoCredentials
                    }
                }
            }
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

//        val dependenciesClasspath = JvmDependency(
//                scriptCompilationClasspathFromContext(
//                        "kotlin-scripting-dependencies" // DependsOn and Repository annotations are taken from it
//                )
//        )
        repl.updateHostConfiguration {
//            configurationDependencies.append(dependenciesClasspath)
        }
        repl.updateCompilationConfiguration {
            defaultImports(DependsOn::class, Repository::class)
//            dependencies.append(dependenciesClasspath)
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
