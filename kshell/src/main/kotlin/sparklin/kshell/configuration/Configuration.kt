package sparklin.kshell.configuration

import sparklin.kshell.Plugin
import kotlin.reflect.KProperty

abstract class Configuration {
    abstract fun <T : Any> get(key: String, converter: Converter<T>): T?

    fun <T : Any> get(key: String, converter: Converter<T>, default: () -> T): T = get(key, converter) ?: default()

    fun <T : Any> get(key: String, converter: Converter<T>, default: T): T = get(key, converter, { default })

    fun get(key: String, default: () -> String) = get(key, IdentityConverter, default)

    fun get(key: String, default: String) = get(key, IdentityConverter, default)

    abstract fun load()

    abstract fun getPlugin(klassName: String): Plugin?

    abstract fun plugins(): Iterator<Plugin>

    inner class DelegateProvider<out T : Any>(private val converter: Converter<T>, val default: () -> T) {
        operator fun <R : Any> getValue(thisRef: R, property: KProperty<*>): T {
            val p = "${thisRef.javaClass.kotlin.qualifiedName}.${property.name}"
            return this@Configuration.get(p, converter, default)
        }
    }

    inner class DelegateProviderForNullables<out T : Any>(private val converter: Converter<T>) {
        operator fun <R : Any> getValue(thisRef: R, property: KProperty<*>): T? {
            val p = "${thisRef.javaClass.kotlin.qualifiedName}.${property.name}"
            return this@Configuration.get(p, converter)
        }
    }

    fun <T : Any> get(converter: Converter<T>, default: () -> T): DelegateProvider<T> = DelegateProvider(converter, default)

    fun <T : Any> get(converter: Converter<T>, default: T): DelegateProvider<T> = DelegateProvider(converter, { default })

    fun get(default: () -> String) = get(IdentityConverter, default)

    fun get(default: String) = get({ default })

    fun <T : Any> getNullable(converter: Converter<T>): DelegateProviderForNullables<T> = DelegateProviderForNullables(converter)

    fun getNullable() = getNullable(IdentityConverter)
}