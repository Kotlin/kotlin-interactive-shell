package sparklin.repl

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import java.io.File
import kotlin.script.templates.standard.ScriptTemplateWithArgs

/**
 * Created by vitaly.khudobakhshov on 18/03/17.
 */

object TestRepl {
    @JvmStatic
    fun main(args: Array<String>) {
        val defs = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES), listOf("sparklin.repl.Shared.*"))
        val repl = AdvancedRepl(additionalClasspath=listOf(),
                sharedHostClassLoader = this.javaClass.classLoader,
                scriptDefinition = defs)
        repl.registerCommand(CommandHelp())

        repl.doRun()

    }
}