package kshell

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import kotlin.reflect.KClass

/**
 * Eventually adopted from https://github.com/kohesive/keplin
 */
open class KotlinScriptDefinitionEx(template: KClass<out Any>,
                                    override val defaultEmptyArgs: ScriptArgsWithTypes?,
                                    val defaultImports: List<String> = emptyList())
    : KotlinScriptDefinition(template), ScriptTemplateEmptyArgsProvider

interface ScriptTemplateEmptyArgsProvider {
    val defaultEmptyArgs: org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes?
}