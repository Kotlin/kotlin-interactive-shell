package org.jetbrains.kotlinx.ki.shell.configuration

interface Converter<T> {
    fun convert(s: String): T
    fun toString(t: T): String = t.toString()
}

class ListConverter<T>(private val itemConverter: Converter<T>):  Converter<List<T>> {
    override fun convert(s: String): List<T> {
        return s.split(",").map { itemConverter.convert(it) }
    }

    override fun toString(t: List<T>): String = t.joinToString(separator = ",")
}

object IdentityConverter: Converter<String> {
    override fun convert(s: String): String = s
}

object TrimConverter: Converter<String> {
    override fun convert(s: String): String = s.trim()
}

object BooleanConverter: Converter<Boolean> {
    override fun convert(s: String): Boolean = s.toBoolean()
}

object IntConverter: Converter<Int> {
    override fun convert(s: String): Int = s.toInt()
}