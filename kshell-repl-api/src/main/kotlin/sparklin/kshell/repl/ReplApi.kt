package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.psi.*
import java.io.Serializable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Named
import kotlin.reflect.KProperty1
import kotlin.reflect.KCallable

sealed class Result<T> {
    class Incomplete<T>: Result<T>()
    data class Error<T>(val message: String, val location: CompilerMessageLocation? = null): Result<T>()
    data class Success<T>(val data: T): Result<T>()
}

abstract class Snippet(val klass: String) {
    abstract fun code(): String
}

abstract class NamedSnippet(klass: String, val name: String): Snippet(klass) {
    open fun toImportStatement(): String = "import $klass.$name"
}

abstract class DeclarationSnippet(klass: String, name: String, var shadowed: Boolean = false): NamedSnippet(klass, name)

class PropertySnippet(klass: String, name: String, val psi: KtProperty): DeclarationSnippet(klass, name) {
    override fun code(): String = psi.text
}

class FunctionSnippet(klass: String, name: String, val psi: KtFunction, var signature: String? = null): DeclarationSnippet(klass, name) {
    override fun code(): String = psi.text
}

class InitializerSnippet(klass: String, val psi: KtScriptInitializer): Snippet(klass) {
    override fun code(): String = psi.text
}

class ImportSnippet(klass: String, val psi: KtImportDirective): Snippet(klass) {
    override fun code(): String = psi.text
}

class SyntheticImportSnippet(klass: String, name: String, val alias: String): NamedSnippet(klass, name) {
    override fun toImportStatement(): String = code()
    override fun code(): String = "import $klass.$name as $alias"
}

class ObjectSnippet(klass: String, name: String, val psi: KtObjectDeclaration): DeclarationSnippet(klass, name) {
    override fun code(): String = psi.text
}

class ClassSnippet(klass: String, name: String, val psi: KtClass): DeclarationSnippet(klass, name) {
    override fun code(): String = psi.text
}


open class State(val lock: ReentrantReadWriteLock) {
    val resultIndex: AtomicInteger = AtomicInteger(1)
    val snippets  = mutableListOf<Snippet>()
}

data class CodeLine(val no: Int, val code: String, val part: Int = 0)

class CheckedCode(
        val codeLine: CodeLine,
        val psiFile: KtFile,
        val errorHolder: DiagnosticMessageHolder)

data class CompiledClasses(
        val mainClassName: String,
        val classes: List<CompiledClassData>,
        val hasResult: Boolean,
        val type: String?)

data class CompiledClassData(val path: String, val bytes: ByteArray) : Serializable {
    override fun equals(other: Any?): Boolean = (other as? CompiledClassData)?.let { path == it.path && Arrays.equals(bytes, it.bytes) } ?: false
    override fun hashCode(): Int = path.hashCode() + Arrays.hashCode(bytes)
}

data class CompiledReplCodeLine(val className: String, val source: CodeLine)

