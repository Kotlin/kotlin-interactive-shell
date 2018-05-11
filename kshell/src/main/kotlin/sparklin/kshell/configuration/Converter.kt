package sparklin.kshell.configuration

interface Converter<out T> {
    fun convert(s: String): T
}

class ListConverter<out T>(private val itemConverter: Converter<T>):  Converter<List<T>> {
    override fun convert(s: String): List<T> {
        return s.split(",").map { itemConverter.convert(it) }
    }
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