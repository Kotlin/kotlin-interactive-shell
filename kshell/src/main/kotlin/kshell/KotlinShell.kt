package kshell

import kshell.command.Help
import kshell.command.Load
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
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

        val repl = KShell(additionalClasspath=findClassJarsOrEmpty(Shared::class),
                sharedHostClassLoader = this.javaClass.classLoader,
                scriptDefinition = defs)
        repl.registerCommand(Help())
        repl.registerCommand(Load())

        repl.doRun()
    }
}