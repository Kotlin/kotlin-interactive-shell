package org.jetbrains.kotlinx.ki.shell.configuration

import org.jetbrains.kotlinx.ki.shell.Plugin
import kotlin.reflect.KProperty

abstract class ReplConfiguration {
    data class ValueWithConverter<T>(val value: T?, val converter: Converter<T>) {
        fun valueToString() = value?.let { converter.toString(it) }
    }

    protected val data: MutableMap<String, ValueWithConverter<*>> = mutableMapOf()

    abstract fun <T : Any> get(key: String, converter: Converter<T>): T?

    fun <T : Any> getWithConverter(key: String, converter: Converter<T>, default: () -> T): T {
        val v = get(key, converter) ?: default()
        data[key] = ValueWithConverter(v, converter)
        return v
    }

    fun <T : Any> getWithConverterNullable(key: String, converter: Converter<T>): T? {
        val v = get(key, converter)
        data[key] = ValueWithConverter(v, converter)
        return v
    }

    fun check(key: String, value: String) {
        if (data.containsKey(key)) {
            data[key]!!.converter.convert(value)
        } else {
            throw IllegalArgumentException("Converter not found for $key")
        }
    }

    fun getTouched(key: String): String? = data[key]!!.valueToString()

    abstract fun list(): Iterable<String>

    // should be renamed back to get() in the future (see https://youtrack.jetbrains.com/issue/KT-24900)
    fun <T : Any> get1(key: String, converter: Converter<T>, default: () -> T): T = getWithConverter(key, converter, default)

    fun <T : Any> get(key: String, converter: Converter<T>, default: T): T = get1(key, converter, { default })

    fun get(key: String, default: () -> String) = get1(key, IdentityConverter, default)

    fun get(key: String, default: String) = get(key, IdentityConverter, default)

    abstract fun set(key: String, value: String)

    abstract fun load()

    abstract fun getPlugin(klassName: String): Plugin?

    abstract fun plugins(): Iterator<Plugin>

    inner class DelegateProvider<out T : Any>(private val converter: Converter<T>, val default: () -> T) {
        operator fun <R : Any> getValue(thisRef: R, property: KProperty<*>): T {
            val p = "${thisRef.javaClass.kotlin.qualifiedName}.${property.name}"
            return this@ReplConfiguration.get1(p, converter, default)
        }
    }

    inner class DelegateProviderForNullables<out T : Any>(private val converter: Converter<T>) {
        operator fun <R : Any> getValue(thisRef: R, property: KProperty<*>): T? {
            val p = "${thisRef.javaClass.kotlin.qualifiedName}.${property.name}"
            return this@ReplConfiguration.getWithConverterNullable(p, converter)
        }
    }

    fun <T : Any> get(converter: Converter<T>, default: () -> T): DelegateProvider<T> = DelegateProvider(converter, default)

    fun <T : Any> get(converter: Converter<T>, default: T): DelegateProvider<T> = DelegateProvider(converter, { default })

    fun get(default: () -> String) = get(IdentityConverter, default)

    fun get(default: String) = get({ default })

    fun <T : Any> getNullable(converter: Converter<T>): DelegateProviderForNullables<T> = DelegateProviderForNullables(converter)

    fun getNullable() = getNullable(IdentityConverter)
}