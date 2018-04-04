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
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URLClassLoader

class ReplTest {
    private val repl: Repl

    init {
        val messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false)
        val moduleName = "my-module"
        val additionalClasspath = listOf<File>()
        val classpath = findRequiredJarFiles(
                includeScriptEngine = false,
                includeKotlinCompiler = false,
                includeStdLib = true,
                includeRuntime = false)

        val conf = CompilerConfiguration().apply {
            addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
            addJvmClasspathRoots(classpath)
            addJvmClasspathRoots(additionalClasspath)
            put(CommonConfigurationKeys.MODULE_NAME, moduleName)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        }

        val baseClassloader = URLClassLoader(conf.jvmClasspathRoots.map { it.toURI().toURL() }
                .toTypedArray(), this.javaClass.classLoader)

       repl = Repl(conf, messageCollector, classpath, baseClassloader)
    }

    @Test
    fun testSimple() {
        assertValue(2, repl.eval("1 + 1"))
    }

    @Test
    fun testMultipleLinesWithShadowing() {
        assertValue(11, repl.eval("val x = 10\nfun f(x: Int)=x\nx + 1"))
        assertUnit(repl.eval("class A() { val x = 10 }"))
        assertUnit(repl.eval("println(res1)\nval a = A()"))
        assertUnit(repl.eval("fun f(x: A)=1"))
        assertUnit(repl.eval("fun f(x: A)=2"))
        assertValue(2, repl.eval("f(a)"))
    }


    private fun assertValue(expected: Any?, result: Result<EvalResult, EvalError>) {
        when (result) {
            is Result.Error -> fail(result.toString())
            is Result.Success -> {
                val data = result.data
                when (data) {
                    is EvalResult.UnitResult -> fail("Value result expected")
                    is EvalResult.ValueResult -> assertEquals(expected, data.value)
                }
            }
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