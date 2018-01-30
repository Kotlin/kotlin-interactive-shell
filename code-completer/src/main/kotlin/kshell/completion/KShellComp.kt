package kshell.completion

import kshell.KShell
import kshell.KotlinScriptDefinitionEx
import lib.jline.console.history.PersistentHistory
import java.io.File

class KShellComp(additionalClasspath: List<File>,
                 scriptDefinition: KotlinScriptDefinitionEx,
                 sharedHostClassLoader: ClassLoader?,
                 shellHistory: PersistentHistory): KShell(
        additionalClasspath = additionalClasspath,
        sharedHostClassLoader = sharedHostClassLoader,
        scriptDefinition = scriptDefinition,
        shellHistory = shellHistory) {

    override fun buildDefaultCompleter() = CodeCompleter(KotlinCompletion(this))
}