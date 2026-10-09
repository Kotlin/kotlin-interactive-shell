package org.jetbrains.kotlinx.ki.completion

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class KiCompletionPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String get() = "org.jetbrains.kotlinx.ki.completion"

    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(KiFirExtensionRegistrar())
    }
}
