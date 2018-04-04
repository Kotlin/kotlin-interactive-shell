package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.net.URLClassLoader
import kotlin.concurrent.write

open class ReplEvaluator(
        val baseClasspath: Iterable<File>,
        val baseClassloader: ClassLoader? = Thread.currentThread().contextClassLoader,
        protected val fallbackScriptArgs: ScriptArgsWithTypes? = null
//        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT
) {

    val topClassLoader: ReplClassLoader = makeReplClassLoader(baseClassloader, baseClasspath)

    fun eval(state: State, snippets: List<Snippet>, compiledClasses: CompiledClasses,
             invokeWrapper: InvokeWrapper?): ReplEvalResult {
        state.lock.write {
            val scriptClass = processClasses(compiledClasses)
            val scriptInstance =
                    try {
                        val scriptInstance = if (invokeWrapper != null) {
                            invokeWrapper.invoke { scriptClass.getDeclaredField("INSTANCE").get(null) }
                        } else {
                            scriptClass.getDeclaredField("INSTANCE").get(null)
                        }
                        scriptInstance
                    }
                    catch (e: Throwable) {
                        throw e
                        // ignore everything in the stack trace until this constructor call
//                        return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                    }

            commitSnippets(state, snippets)

            val resultField = scriptClass.getDeclaredField(ReplCompiler.RESULT_FIELD_NAME).apply { isAccessible = true }
            val resultValue: Any? = resultField.get(scriptInstance)

            return if (compiledClasses.hasResult) ReplEvalResult.ValueResult(resultValue, compiledClasses.type)
            else ReplEvalResult.UnitResult()
        }
    }

    private fun commitSnippets(state: State, snippets: List<Snippet>) {
        state.snippets.shadow(snippets)
        state.snippets.addAll(snippets)
    }

    private fun processClasses(compileResult: CompiledClasses): Class<out Any> {
        var mainLineClassName: String? = null
        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.removeSuffix(".class"))
        fun compiledClassesNames() = compileResult.classes.map { classNameFromPath(it.path).internalName.replace('/', '.') }
        val expectedClassName = compileResult.mainClassName
        compileResult.classes.filter { it.path.endsWith(".class") }
                .forEach {
                    val className = classNameFromPath(it.path)
                    if (className.internalName == expectedClassName || className.internalName.endsWith("/$expectedClassName")) {
                        mainLineClassName = className.internalName.replace('/', '.')
                    }
                    topClassLoader.addClass(className, it.bytes)
                }

        return try {
            topClassLoader.loadClass(mainLineClassName!!)
        }
        catch (t: Throwable) {
            throw Exception("Error loading class $mainLineClassName: known classes: ${compiledClassesNames()}", t)
        }
    }

    fun makeReplClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
            ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))

}