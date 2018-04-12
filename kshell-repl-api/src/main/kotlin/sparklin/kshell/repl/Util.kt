package sparklin.kshell.repl

import org.jetbrains.kotlin.utils.PathUtil

fun PathUtil.stdlibPathForJar() = PathUtil.getResourcePathForClass(Pair::class.java)

internal fun getJavaVersion(): Int {
    val default = 0x10006
    val version = System.getProperty("java.specification.version") ?: return default
    val components = version.split('.')
    return try {
        when (components.size) {
            0 -> default
            1 -> components[0].toInt() * 0x10000
            else -> components[0].toInt() * 0x10000 + components[1].toInt()
        }
    } catch (e: NumberFormatException) {
        default
    }
}

internal fun List<Snippet>.containsWithName(name: String): Boolean =
        this.any { it is NamedSnippet && it.name == name }

internal fun List<Snippet>.shadow(snippets: List<Snippet>) {
    filterIsInstance<DeclarationSnippet>().forEach {
        val historyItem = it
        if (snippets.filterIsInstance<DeclarationSnippet>().any { historyItem.signature() == it.signature()  }) historyItem.shadowed = true
    }
}