package sparklin.kshell

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import sparklin.kshell.configuration.CachedInstance
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.ConfigurationImpl
import java.io.File
import kotlin.script.templates.standard.ScriptTemplateWithArgs

object KotlinShell {
    @JvmStatic
    fun main(args: Array<String>) {
        val defs = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                listOf(Shared::class.qualifiedName + ".*"))

        val repl = KShell(configuration(), additionalClasspath=findClassJarsOrEmpty(sparklin.kshell.Shared::class),
                sharedHostClassLoader = this.javaClass.classLoader,
                scriptDefinition = defs)

        Runtime.getRuntime().addShutdownHook(Thread({
            println("\nBye!")
            repl.cleanUp()
        }))

        repl.doRun()
    }

    fun configuration(): Configuration {
        val instance = CachedInstance<Configuration>()
        val klassName: String? = System.getProperty("config.class")

        return if (klassName != null) {
            instance.load(klassName, Configuration::class)
        } else {
            instance.get { ConfigurationImpl() }
        }
    }
}