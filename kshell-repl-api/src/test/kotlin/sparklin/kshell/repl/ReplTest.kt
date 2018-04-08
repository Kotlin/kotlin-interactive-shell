package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.URLClassLoader

class ReplTest {
    private lateinit var repl: Repl

    @Before
    fun setup() {
        val messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false)
        val moduleName = "my-module"

        val classpath = listOf(PathUtil.stdlibPathForJar())

        val conf = CompilerConfiguration().apply {
            addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
            addJvmClasspathRoots(classpath)
            put(CommonConfigurationKeys.MODULE_NAME, moduleName)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        }

        val baseClassloader = URLClassLoader(conf.jvmClasspathRoots.map { it.toURI().toURL() }
                .toTypedArray(), javaClass.classLoader)

        repl = Repl(conf, messageCollector, classpath, baseClassloader)
    }

    @Test
    fun testSimpleValue() {
        assertValue(2, repl.eval("1 + 1"))
    }

    @Test
    fun testMultipleLinesWithShadowing() {
        assertValue(11, repl.eval("val x = 10\nfun f(x: Int)=x\nx + 1"))
        assertUnit(repl.eval("class A() { val x = 10 }"))
        assertUnit(repl.eval("val a = A()"))
        assertUnit(repl.eval("fun f(x: A)=1"))
        assertUnit(repl.eval("fun f(x: A)=2"))
        assertValue(2, repl.eval("f(a)"))
    }

    @Test
    fun testOverloadWithShadowing() {
        assertSuccess(repl.eval("fun f(x: Int) = 1"))
        assertValue(2, repl.eval("fun f(x: Int) = 2\nfun f(x: Int, y: Int) = x + y\nf(10)"))
    }

    @Test
    fun testCommitRollback() {
        assertError(repl.eval( "fun f(x: Int) = x + 1\nfun f(x: Int, y: Int) = x + y\nval x: Int = TODO()"))
        assertEquals(0, repl.state.history.size)
    }

    @Test
    fun testInitializers() {
        assertValue(6, repl.eval("var x = 5\nx = x + 1\nx"))
    }

    @Test
    fun testShadowing() {
        assertSuccess(repl.eval("val x = 10\nval y = 5"))
        assertValue(25, repl.eval("val x = 20\nx + y"))
        assertSuccess(repl.eval("fun f(x: Int) = x + 1\nfun f(x: Int, y: Int) = x + y"))
        assertSuccess(repl.eval("fun f(x: Int) = 5"))
        assertValue(5, repl.eval("f(10)"))
    }

    @Test
    fun testGenericsShadowing() {
        assertSuccess(repl.eval("fun <R> f(x: R, y: Int)=x"))
        assertSuccess(repl.eval("fun f(x: Double, y: Int)=x"))
        assertSuccess(repl.eval("fun <K> f(x: K, y: Int)=y"))
        assertValue(10, repl.eval("f(20, 10)"))
        assertValue(20.0, repl.eval("f(20.0, 10)"))
    }

    @Test
    fun testClassShadowing() {
        assertSuccess(repl.eval("class A { val x = 10 }"))
        assertSuccess(repl.eval("class A { val x = 20 }"))
        assertValue(20, repl.eval("val a = A()\na.x"))
    }

    @Test
    fun testObjectShadowing() {
        assertSuccess(repl.eval("object A { val x = 10 }"))
        assertSuccess(repl.eval("object A { val x = 20 }"))
        assertValue(20, repl.eval("A.x"))
    }

    @Test
    fun testNamespace() {
        assertValue(11, repl.eval("val x = 10\nfun x()=1\nx+x()"))
        assertValue(11, repl.eval("object a { val x = 1 }\nfun a() = 10\na() + a.x"))
    }

    @Test
    fun testResult() {
        assertSuccess(repl.eval("1 + 1"))
        assertSuccess(repl.eval("10"))
        assertValue(12, repl.eval("res1 + res2"))
    }

    @Test
    fun testMultipleShadowing() {
        (1 .. 100).forEach {
            assertSuccess(repl.eval("fun f(x: Int) = x + $it"))
        }
        assertValue(101, repl.eval("f(1)"))
    }

    @Test
    fun testType() {
        assertType("kotlin.Int", repl.eval("1 + 1"))
        assertType("kotlin.collections.List<kotlin.Int>", repl.eval("listOf(1,2,3)"))
    }

    @Test
    fun testCompoundTypesSignature() {
        assertSuccess(repl.eval("fun <R, T> f(x: List<T>, y: List<Map<R,T>>)=1"))
        assertEquals("List<#1>,List<Map<#0,#1>>", (repl.state.history.last() as FunctionSnippet).parametersTypes)
    }

    @Test
    fun testImport() {
        assertSuccess(repl.eval("import java.io.File"))
        assertSuccess(repl.eval("fun f(x: File)=x"))
        assertSuccess(repl.eval("fun g(x: File)=x"))
        assertSuccess(repl.eval("import java.io.File"))
        assertSuccess(repl.eval("import java.io.*"))
        assertSuccess(repl.eval("import java.io.*"))
        assertSuccess(repl.eval("fun g1(x: File)=x"))
    }

    @Test
    fun testConsistentImportsForDeferredSnippets() {
        assertSuccess(repl.eval("import java.io.File\nfun f(x: Int, a: File) = x\nfun f(a: File) = a"))
    }

    @Test
    fun testPermanentSnippets() {
        assertSuccess(repl.eval("1 + 1"))
        assertError(repl.eval("val res1 = 10"))
    }

    @Test
    fun testQualifiedImport() {
        assertSuccess(repl.eval("import java.io.File as JFile"))
        assertSuccess(repl.eval("fun f(x: JFile)=x"))
        assertSuccess(repl.eval("fun g(x: JFile)=f(x)"))
        assertSuccess(repl.eval("import java.io.FileInputStream as JFile"))
        assertSuccess(repl.eval("fun f(v: JFile)=v.close()"))
    }

    private fun assertValue(expected: Any?, result: Result<EvalResult, EvalError>) {
        when (result) {
            is Result.Error -> fail(result.error.message)
            is Result.Success -> {
                val data = result.data
                when (data) {
                    is EvalResult.UnitResult -> fail("Value result expected")
                    is EvalResult.ValueResult -> assertEquals(expected, data.value)
                }
            }
        }
    }

    private fun assertType(expected: String, result: Result<EvalResult, EvalError>) {
        when (result) {
            is Result.Error -> fail(result.error.message)
            is Result.Success -> {
                val data = result.data
                when (data) {
                    is EvalResult.UnitResult -> fail("Value result expected")
                    is EvalResult.ValueResult -> assertEquals(expected, data.type)
                }
            }
        }
    }

    private fun assertSuccess(result: Result<EvalResult, EvalError>) {
        when (result) {
            is Result.Error -> fail(result.error.message)
            is Result.Success -> { }
        }
    }

    private fun assertError(result: Result<EvalResult, EvalError>) {
        when (result) {
            is Result.Error -> { }
            is Result.Success -> fail("Should be error but success found")
        }
    }

    private fun assertUnit(result: Result<EvalResult, EvalError>) {
        when (result) {
            is Result.Error -> fail(result.toString())
            is Result.Success -> {
                val data = result.data
                when (data) {
                    is EvalResult.UnitResult -> { }
                    is EvalResult.ValueResult -> fail("Unit result expected, but found $data")
                }
            }
        }
    }
}