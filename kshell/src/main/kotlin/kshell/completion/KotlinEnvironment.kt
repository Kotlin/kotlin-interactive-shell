package kshell.completion

import kshell.KShell
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment

fun getReplEnvironment(repl: KShell): KotlinCoreEnvironment {
    // FIXME: reuse existing environment if possible
    return  KotlinCoreEnvironment.createForProduction(repl.disposable,
            repl.compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
}
