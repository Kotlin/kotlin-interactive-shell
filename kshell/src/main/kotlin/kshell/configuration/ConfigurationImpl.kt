package kshell.configuration

import kshell.Plugin
import kshell.console.ConsoleReader
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*

class ConfigurationImpl: Configuration {
    private val props = Properties()
    private val plugins = linkedMapOf<String, CachedInstance<Plugin>>()
    private val consoleReader = CachedInstance<ConsoleReader>()

    override fun <T : Any> get(key: String, converter: Converter<T>, default: () -> T): T {
        val strValue = props.getProperty(key)
        return if (strValue != null) converter.convert(strValue) else default()
    }

    override fun load() {
        val path = configPath()

        if (File(path).exists()) {
            props.load(BufferedReader(FileReader(path)))
        }

        val pluginClasses = get("plugins", ListConverter(IdentityConverter),
                listOf("kshell.plugins.LoadFilePlugin", "kshell.plugins.RuntimePlugin", "kshell.plugins.HelpPlugin"))

        pluginClasses.forEach { klassName ->
            val instance = CachedInstance<Plugin>()
            instance.load(klassName, Plugin::class)
            plugins[klassName] = instance
        }
    }

    private fun configPath() = System.getProperty("config.path") ?:
        (System.getProperty("user.home") ?: "") + File.separator + ".kshell"

    override fun getPlugin(klassName: String): Plugin? = plugins[klassName]?.get()

    override fun plugins(): Iterator<Plugin> = plugins.values.map { it.get()!! }.iterator()

    override fun getConsoleReader(): ConsoleReader {
        val klassName = get("console.class","kshell.console.jline2.ConsoleReaderImpl")
        val reader = consoleReader.load(klassName, ConsoleReader::class)
        reader.init(this)
        return reader
    }
}