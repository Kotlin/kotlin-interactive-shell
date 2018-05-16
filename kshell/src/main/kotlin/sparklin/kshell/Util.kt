package sparklin.kshell

import java.io.File

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