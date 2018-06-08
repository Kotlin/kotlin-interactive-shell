package com.github.khud.sparklin.kshell.configuration

import com.github.khud.sparklin.kshell.Plugin
import java.util.*

open class PropertyBasedConfiguration(protected val props: Properties, protected val defaultPlugins: List<String>) : Configuration() {
    private val plugins = linkedMapOf<String, CachedInstance<Plugin>>()

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
}