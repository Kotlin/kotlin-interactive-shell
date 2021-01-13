package org.jetbrains.kotlinx.ki.shell.configuration

import org.jetbrains.kotlinx.ki.shell.Plugin
import java.util.*

open class PropertyBasedReplConfiguration(protected val props: Properties, protected val defaultPlugins: List<String>) : ReplConfiguration() {
    private val plugins = linkedMapOf<String, CachedInstance<Plugin>>()

    override fun <T : Any> get(key: String, converter: Converter<T>): T? {
        val strValue = props.getProperty(key)
        return strValue?.let { converter.convert(strValue) }
    }

    override fun set(key: String, value: String) {
        check(key, value)
        props.setProperty(key, value)
    }

    override fun list(): List<String> = data.keys.toList()

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
}