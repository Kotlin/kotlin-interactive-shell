package org.jetbrains.kotlinx.ki.shell

import java.io.File
import java.util.*
import java.util.regex.Pattern

fun printVersion() {
    val version = ApplicationProperties.version
    println("ki-shell $version/${KotlinVersion.CURRENT}")
}

fun calcHumanReadableSize(bytes: Long, si: Boolean = false): String {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

fun String.bound(maxLength: Int): String {
    return if (length > maxLength) substring(0, maxLength - 1) + " ..." else this
}

fun replJars(jars: List<String> = listOf()): List<File> {
    // to get Spark related libraries and so on
    val jvmClasspath = System.getProperty("java.class.path")
    val systemJars = jvmClasspath.split(File.pathSeparatorChar).map(::File)

    return jars.filter {
        // exclude all Kotlin stuff from REPL classpath
        it -> !it.contains(Regex("/kotlin-(runtime|stdlib|compiler|reflect)(-.*)?\\.jar"))
    }.map {
        // remove "file:"
        it ->
        val p = it.indexOf(':')
        File(it.substring(p + 1))
    } + systemJars
}

fun globToRegex(line: String): String {
    val sb = StringBuilder()
    line.forEach {
        when (it) {
            '*' -> sb.append("[a-zA-Z_0-9.]*")
            '?' -> sb.append("[a-zA-Z_0-9.]")
            '.' -> sb.append("\\.")
            else -> sb.append(it)
        }
    }
    return sb.toString()
}

fun smartSplit(line: String): List<String> {
    val list = ArrayList<String>()
    val m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line)
    while (m.find())
        list.add(m.group(1).unquote())
    return list
}

fun String.unquote(): String = if (startsWith("\"") && endsWith("\"")) substring(1, length - 1) else this

private val builtinPackages = listOf(
        "kotlin",
        "kotlin.collections",
        "java.lang"
)
private val typeDelimiters = "<>, "
private val builtinsNamesRE = Regex("(?<=^|[$typeDelimiters])(?:(?:${builtinPackages.joinToString("|") { Regex.escape(it) }})\\.){1}([^\\.$typeDelimiters]+)(?=[$typeDelimiters]|\$)")

fun renderKotlinType(typeName: String): String = typeName.replace(builtinsNamesRE, "\$1")
