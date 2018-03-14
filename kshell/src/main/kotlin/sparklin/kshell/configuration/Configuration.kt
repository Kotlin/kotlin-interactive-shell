package sparklin.kshell.configuration

import sparklin.kshell.Plugin
import sparklin.kshell.console.ConsoleReader
import java.io.*

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