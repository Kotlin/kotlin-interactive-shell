package kshell

import kshell.command.Help
import kshell.command.Load
import kshell.command.Type
import lib.jline.console.history.FileHistory
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import java.io.File
import kotlin.script.templates.standard.ScriptTemplateWithArgs

/**
 * Created by vitaly.khudobakhshov on 18/03/17.
 */

object KotlinShell {
    @JvmStatic
    fun main(args: Array<String>) {
        val defs = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                listOf("kshell.Shared.*"))

        val historyPath = System.getProperty("user.home") + File.separator + ".kshell.history"

        val hist = FileHistory(File(historyPath))

        val repl = KShell(additionalClasspath=findClassJarsOrEmpty(Shared::class),
                sharedHostClassLoader = this.javaClass.classLoader,
                scriptDefinition = defs,
                shellHistory = hist)

        Runtime.getRuntime().addShutdownHook(Thread({
            println("\nBye!")
            repl.cleanUp()
        }))

        repl.apply {
            registerCommand(Help())
            registerCommand(Load())
            registerCommand(Type())
            doRun()
        }

    }
}