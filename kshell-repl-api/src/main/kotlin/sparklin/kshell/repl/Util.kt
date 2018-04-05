package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompiler
import sparklin.kshell.*
import java.io.File
import kotlin.reflect.KClass

fun findRequiredJarFiles(includeScriptEngine: Boolean = false,
                                  includeKotlinCompiler: Boolean = false,
                                  useEmbeddableCompiler: Boolean = true,
                                  includeStdLib: Boolean = true,
                                  includeRuntime: Boolean = true,
                                  additionalClasses: List<KClass<out Any>> = emptyList()): List<File> {
    val additionalClassJars = additionalClasses.map { findClassJarsOrEmpty(it).assertNotEmpty("Missing JAR for additional class $it") }.flatten()
    val scriptEngineJars = if (includeScriptEngine) findClassJarsOrEmpty(GenericReplCompiler::class).assertNotEmpty("Cannot find repl engine classpath, which is required")
    else emptyList()
    val kotlinJars = (if (includeKotlinCompiler) findKotlinCompilerJars(useEmbeddableCompiler) else emptyList()) +
            (if (includeStdLib) findKotlinStdLibJars() else emptyList()) +
            (if (includeRuntime) findKotlinRuntimeJars() else emptyList())
    return (additionalClassJars + scriptEngineJars + kotlinJars).toSet().toList()
}

fun getJavaVersion(): Int {
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

fun makeFileBaseName(codeLine: CodeLine) =
        "Line_${codeLine.no}" + if (codeLine.part != 0) "_${codeLine.part}" else ""

fun List<Snippet>.containsWithName(name: String): Boolean =
        this.any { it is NamedSnippet && it.name == name }

fun List<Snippet>.filterNamed(): List<NamedSnippet> =
        this.filter { it is NamedSnippet }.map { it as NamedSnippet }

fun List<Snippet>.filterDeclarations(): List<DeclarationSnippet> =
        this.filter { it is DeclarationSnippet }.map { it as DeclarationSnippet }

fun List<Snippet>.shadow(snippets: List<Snippet>) {
    filterDeclarations().forEach {
        val historyItem = it
        if (snippets.filterDeclarations().any { historyItem.signature() == it.signature()  }) historyItem.shadowed = true
    }
}

fun List<DeclarationSnippet>.printAll(prefix: String) {
    forEach { println("$prefix>> ${it.klass}.${it.signature()}-> ${it.shadowed}") }
}
//fun renderReplStackTrace(cause: Throwable): String {
//    val newTrace = arrayListOf<StackTraceElement>()
//    var skip = true
//    for (element in cause.stackTrace.reversed()) {
//        val method = "${element.className}.${element.methodName}"
//        if (method.startsWith("Line_") && method.endsWith("<clinit>")) {
//            skip = false
//        }
//        if (!skip) {
//            newTrace.add(element)
//        }
//    }
//
//    val resultingTrace = newTrace.reversed().dropLast(1)
//
//    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
//    (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())
//
//    return Throwables.getStackTraceAsString(cause).trimEnd()
//}