package sparklin.kshell

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.Converter
import sparklin.kshell.console.Completer
import sparklin.kshell.console.ConsoleReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class KShellTest {
    private lateinit var repl: KShell
    private lateinit var stream: SingleLineOutputStream
    private val out = System.out

    val testConfig = object : Configuration {
        override fun <T : Any> get(key: String, converter: Converter<T>, default: () -> T): T {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun load() { }

        override fun getPlugin(klassName: String): Plugin? = null

        override fun plugins(): Iterator<Plugin> = listOf<Plugin>().iterator()

        override fun getConsoleReader(): ConsoleReader = echoReader
    }

    val echoReader = object : ConsoleReader {
        override fun dropHistory(n: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun addHistoryItem(item: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun init(config: Configuration) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun addCompleter(completer: Completer) { }

        override fun setPrompt(prompt: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun readLine(): String? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun println(s: String) {
            kotlin.io.println(s)
        }

        override fun cleanUp() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    init {
        val defs = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                listOf())

        repl = KShell(testConfig, additionalClasspath=listOf(),
                sharedHostClassLoader = this.javaClass.classLoader,
                scriptDefinition = defs)
        repl.initEngine()

        stream = SingleLineOutputStream()

        val ps = PrintStream(stream)
        System.setOut(ps)
    }

    @Before
    fun init() {
        repl.resultCounter.set(1)
    }

    @After
    fun cleanUp() {
        System.setOut(out)
    }

    @Test
    fun testResultValues() {
        repl.compileAndEval("1 + 1")
        assertEquals("res1: kotlin.Int = 2", stream.last())
        repl.compileAndEval("res1 + res1")
        assertEquals("res2: kotlin.Int = 4", stream.last())
    }

    @Test
    fun testObjects() {
        repl.compileAndEval("listOf(1, 2, 3)")
        repl.compileAndEval("res1[0]")
        assertEquals("res2: kotlin.Int = 1", stream.last())
    }

    @Test
    fun testIncomplete() {
        repl.compileAndEval("{")
        repl.compileAndEval("val x = 10")
        repl.compileAndEval("}")
        // FIXME: need more informative result
        assertEquals("res1: () -> kotlin.Unit = () -> kotlin.Unit", stream.last())
    }

    @Test
    fun testPlatformTypes() {
        repl.compileAndEval("val m = java.util.HashMap<String,String>()")
        repl.compileAndEval("m.get(\"hello\")")
        assertEquals("res1: kotlin.String? = null", stream.last())

        repl.compileAndEval("val list = java.util.ArrayList<String>()")
        repl.compileAndEval("list.add(\"Item\")") // res2
        repl.compileAndEval("val size = list.size") // non-null (primitive int)
        repl.compileAndEval("list[0]")   // platform type inferred (ordinary Java object)
        assertEquals("res3: kotlin.String = Item", stream.last())

        // FIXME: test like sc.parallelize(listOf(1,2,3,4)) needed
    }

    @Test
    fun testMultilineResult() {
        repl.compileAndEval("val x = 10\nx * x")
        assertEquals(stream.toString(), "100 : kotlin.Int", stream.last())
    }
}
