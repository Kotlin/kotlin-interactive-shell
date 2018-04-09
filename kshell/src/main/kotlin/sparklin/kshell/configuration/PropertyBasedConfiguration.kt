package sparklin.kshell.configuration

import sparklin.kshell.Plugin
import sparklin.kshell.console.ConsoleReader
import sparklin.kshell.plugins.HelpPlugin
import sparklin.kshell.plugins.LoadFilePlugin
import sparklin.kshell.plugins.PastePlugin
import sparklin.kshell.plugins.RuntimePlugin
import java.io.*
import java.util.*

open class PropertyBasedConfiguration(protected val props: Properties, protected val defaultPlugins: List<String>) : Configuration() {
    private val plugins = linkedMapOf<String, CachedInstance<Plugin>>()
    private val consoleReader = CachedInstance<ConsoleReader>()

    override fun <T : Any> get(key: String, converter: Converter<T>): T? {
        val strValue = props.getProperty(key)
        return strValue?.let { converter.convert(strValue) }
    }

    override fun load() {
        val pluginClasses = get("plugins", ListConverter(TrimConverter), defaultPlugins)

        pluginClasses.forEach { klassName ->
            val instance = CachedInstance<Plugin>()
            instance.load(klassName, Plugin::class)
            plugins[klassName] = instance
        }
    }

    override fun getPlugin(klassName: String): Plugin? = plugins[klassName]?.get()

    override fun plugins(): Iterator<Plugin> = plugins.values.map { it.get()!! }.iterator()

    override fun getConsoleReader(): ConsoleReader {
        val klassName = get("console.class","sparklin.kshell.console.jline2.ConsoleReaderImpl")
        val reader = consoleReader.load(klassName, ConsoleReader::class)
        reader.init(this)
        return reader
    }
}