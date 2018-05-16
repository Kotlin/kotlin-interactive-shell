package sparklin.kshell

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