package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.concurrent.write
import kotlin.reflect.full.*

open class ReplEvaluator(
        val baseClasspath: Iterable<File>,
        val baseClassloader: ClassLoader? = Thread.currentThread().contextClassLoader,
        protected val fallbackScriptArgs: ScriptArgsWithTypes? = null
//        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT
) {

    val topClassLoader: ReplClassLoader = makeReplClassLoader(baseClassloader, baseClasspath)

    fun eval(state: State, compiledClasses: CompiledClasses,
             invokeWrapper: InvokeWrapper?): ReplEvalResult {
        state.lock.write {

            val (classLoader, scriptClass) = processClasses(compiledClasses)


            val scriptInstance =
                    try {
//                        if (invokeWrapper != null) invokeWrapper.invoke { scriptInstanceConstructor.newInstance(*constructorArgs) }
//                        else scriptInstanceConstructor.newInstance(*constructorArgs)
                        val scriptInstance = if (invokeWrapper != null) {
                            invokeWrapper.invoke { scriptClass.getDeclaredField("INSTANCE").get(null) }
                        } else {
                            scriptClass.getDeclaredField("INSTANCE").get(null)
                        }

//                        scriptInstance.javaClass.getDeclaredMethod("run").invoke(null)
//                        scriptInstance.javaClass.getDeclaredMethod("run").invoke(null)
                        scriptInstance
                    }
                    catch (e: InvocationTargetException) {
                        // ignore everything in the stack trace until this constructor call
                        return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e.targetException as? Exception)
                    }
                    catch (e: Throwable) {
                        //e.printStackTrace()
                        throw e
                        // ignore everything in the stack trace until this constructor call
//                        return@eval ReplEvalResult.Error.Runtime(renderReplStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"), e as? Exception)
                    }
                    finally {
//                        historyActor.removePlaceholder(compileResult.lineId)
                    }

//            scriptInstance.javaClass.kotlin.memberProperties.forEach { state.snippets.add(Snippet.PropertySnippet(compiledClasses.mainClassName, it)) }
//            scriptInstance.javaClass.kotlin.declaredFunctions.forEach {
//                if (it.name != "run") state.snippets.add(Snippet.FunctionSnippet(compiledClasses.mainClassName, it))
//            }

//            state.snippets.forEach {
//                println("**** " + it.toImportStatement())
//            }
//            state.history.last().instance = scriptInstance
//            historyActor.addFinal(compileResult.lineId, EvalClassWithInstanceAndLoader(scriptClass.kotlin, scriptInstance, classLoader, invokeWrapper))

//            val resultField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
            val resultValue: Any? = null //resultField.get(scriptInstance)

            return if (compiledClasses.hasResult) ReplEvalResult.ValueResult(resultValue, compiledClasses.type)
            else ReplEvalResult.UnitResult()
        }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }

    private fun processClasses(compileResult: CompiledClasses
    ): Pair<ClassLoader, Class<out Any>> {
        var mainLineClassName: String? = null
        val classLoader = topClassLoader//makeReplClassLoader(topClassLoader, compileResult.classpathAddendum)//makeReplClassLoader(effectiveHistory.lastOrNull()?.classLoader ?: state.topClassLoader, compileResult.classpathAddendum)
        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.removeSuffix(".class"))
        fun compiledClassesNames() = compileResult.classes.map { classNameFromPath(it.path).internalName.replace('/', '.') }
        val expectedClassName = compileResult.mainClassName
        compileResult.classes.filter { it.path.endsWith(".class") }
                .forEach {
                    val className = classNameFromPath(it.path)
                    if (className.internalName == expectedClassName || className.internalName.endsWith("/$expectedClassName")) {
                        mainLineClassName = className.internalName.replace('/', '.')
                    }
                    classLoader.addClass(className, it.bytes)
                }

        val scriptClass = try {
            classLoader.loadClass(mainLineClassName!!)
        }
        catch (t: Throwable) {
            throw Exception("Error loading class $mainLineClassName: known classes: ${compiledClassesNames()}", t)
        }
        return Pair(classLoader, scriptClass)
    }

    fun makeReplClassLoader(baseClassloader: ClassLoader?, baseClasspath: Iterable<File>) =
            ReplClassLoader(URLClassLoader(baseClasspath.map { it.toURI().toURL() }.toTypedArray(), baseClassloader))

}

//private open class HistoryActionsForNoRepeat(val state: GenericReplEvaluatorState) {
//
//    open val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = state.history.map { it.item }
//
//    open fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? = state.history.firstMismatch(other)
//
//    open fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) { state.history.push(lineId, value) }
//
//    open fun removePlaceholder(lineId: ILineId): Boolean = state.history.verifiedPop(lineId) != null
//
//    open fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) { state.history.push(lineId, value) }
//
//    open fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> = prependClassLoaderWithNewClasses(effectiveHistory, compileResult)
//
//    private fun prependClassLoaderWithNewClasses(effectiveHistory: List<EvalClassWithInstanceAndLoader>,
//                                                 compileResult: ReplCompileResult.CompiledClasses
//    ): Pair<ClassLoader, Class<out Any>> {
//        var mainLineClassName: String? = null
//        val classLoader = makeReplClassLoader(effectiveHistory.lastOrNull()?.classLoader ?: state.topClassLoader, compileResult.classpathAddendum)
//        fun classNameFromPath(path: String) = JvmClassName.byInternalName(path.removeSuffix(".class"))
//        fun compiledClassesNames() = compileResult.classes.map { classNameFromPath(it.path).internalName.replace('/', '.') }
//        val expectedClassName = compileResult.mainClassName
//        compileResult.classes.filter { it.path.endsWith(".class") }
//                .forEach {
//                    val className = classNameFromPath(it.path)
//                    if (className.internalName == expectedClassName || className.internalName.endsWith("/$expectedClassName")) {
//                        mainLineClassName = className.internalName.replace('/', '.')
//                    }
//                    classLoader.addClass(className, it.bytes)
//                }
//
//        val scriptClass = try {
//            classLoader.loadClass(mainLineClassName!!)
//        }
//        catch (t: Throwable) {
//            throw Exception("Error loading class $mainLineClassName: known classes: ${compiledClassesNames()}", t)
//        }
//        return Pair(classLoader, scriptClass)
//    }
//}

//private open class HistoryActionsForRepeatRecentOnly(state: GenericReplEvaluatorState) : HistoryActionsForNoRepeat(state) {
//
//    val currentLast = state.history.peek()!!
//
//    override val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = super.effectiveHistory.dropLast(1)
//
//    override fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? =
//            state.history.firstMismatchFiltered(other) { it.id != currentLast.id }
//
//    override fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {}
//
//    override fun removePlaceholder(lineId: ILineId): Boolean = true
//
//    override fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
//        state.history.pop()
//        state.history.push(lineId, value)
//    }
//
//    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> =
//            currentLast.item.classLoader to currentLast.item.klass.java
//}private open class HistoryActionsForRepeatRecentOnly(state: GenericReplEvaluatorState) : HistoryActionsForNoRepeat(state) {
//
//    val currentLast = state.history.peek()!!
//
//    override val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = super.effectiveHistory.dropLast(1)
//
//    override fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? =
//            state.history.firstMismatchFiltered(other) { it.id != currentLast.id }
//
//    override fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {}
//
//    override fun removePlaceholder(lineId: ILineId): Boolean = true
//
//    override fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
//        state.history.pop()
//        state.history.push(lineId, value)
//    }
//
//    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> =
//            currentLast.item.classLoader to currentLast.item.klass.java
//}

//private open class HistoryActionsForRepeatAny(state: GenericReplEvaluatorState, val matchingLine: ReplHistoryRecord<EvalClassWithInstanceAndLoader>): HistoryActionsForNoRepeat(state) {
//
//    override val effectiveHistory: List<EvalClassWithInstanceAndLoader> get() = state.history.takeWhile { it.id != matchingLine.id }.map { it.item }
//
//    override fun firstMismatch(other: Sequence<ILineId>): Pair<ReplHistoryRecord<EvalClassWithInstanceAndLoader>?, ILineId?>? =
//            state.history.firstMismatchWhile(other) { it.id != matchingLine.id }
//
//    override fun addPlaceholder(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {}
//
//    override fun removePlaceholder(lineId: ILineId): Boolean = true
//
//    override fun addFinal(lineId: ILineId, value: EvalClassWithInstanceAndLoader) {
//        val extraLines = state.history.takeLastWhile { it.id == matchingLine.id }
//        state.history.resetTo(lineId)
//        state.history.pop()
//        state.history.push(lineId, value)
//        extraLines.forEach {
//            state.history.push(it.id, it.item)
//        }
//    }
//
//    override fun processClasses(compileResult: ReplCompileResult.CompiledClasses): Pair<ClassLoader, Class<out Any>> =
//            matchingLine.item.classLoader to matchingLine.item.klass.java
//}
