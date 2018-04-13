package sparklin.kshell.configuration

import sparklin.kshell.plugins.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

class ConfigurationImpl : PropertyBasedConfiguration(Properties(),
        listOf(LoadFilePlugin::class.qualifiedName!!,
        RuntimePlugin::class.qualifiedName!!,
        HelpPlugin::class.qualifiedName!!,
        PastePlugin::class.qualifiedName!!, SyntaxPlugin::class.qualifiedName!!))  {
    override fun load() {
        val path = configPath()

        if (File(path).exists()) {
            props.load(BufferedReader(FileReader(path)))
        }

        super.load()
    }

    private fun configPath() = System.getProperty("config.path") ?:
        (System.getProperty("user.home") ?: "") + File.separator + ".kshell"
}