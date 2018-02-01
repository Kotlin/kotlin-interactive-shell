package kshell.configuration

import kshell.Plugin
import kshell.console.ConsoleReader
import kotlin.reflect.KClass

interface Configuration {
    fun <T : Any> get(key: String, converter: Converter<T>, default: () -> T): T

    fun <T : Any> get(key: String, converter: Converter<T>, default: T): T = get(key, converter, { default })

    fun get(key: String, default: () -> String) = get(key, IdentityConverter, default)

    fun get(key: String, default: String) = get(key, IdentityConverter, default)

    fun load()

    fun getPlugin(klassName: String): Plugin?

    fun plugins(): Iterator<Plugin>

    fun getConsoleReader(): ConsoleReader
}