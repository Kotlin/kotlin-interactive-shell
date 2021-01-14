package org.jetbrains.kotlinx.ki.shell.configuration

import org.jetbrains.kotlinx.ki.shell.plugins.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

abstract class ReplConfigurationBase(defaultPlugins: List<String> = DEFAULT_PLUGINS) :
    PropertyBasedReplConfiguration(Properties(), defaultPlugins) {

    override fun load() {
        val path = configPath()

        if (File(path).exists()) {
            props.load(BufferedReader(FileReader(path)))
        }

        super.load()
    }

    private fun configPath() =
        System.getProperty("config.path") ?: System.getenv("KI_CONFIG") ?: (System.getProperty("user.home")
            ?: "") + File.separator + ".ki-shell"

    companion object {
        val DEFAULT_PLUGINS = listOf(
            LoadFilePlugin::class.qualifiedName!!,
            RuntimePlugin::class.qualifiedName!!,
            HelpPlugin::class.qualifiedName!!,
            PastePlugin::class.qualifiedName!!,
            SyntaxPlugin::class.qualifiedName!!,
            PromptPlugin::class.qualifiedName!!,
            ConfigPlugin::class.qualifiedName!!,
            DependenciesPlugin::class.qualifiedName!!,
            ExecutionEnvironmentPlugin::class.qualifiedName!!
        )
    }
}