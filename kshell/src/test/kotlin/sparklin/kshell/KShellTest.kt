package sparklin.kshell

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.Converter
import sparklin.kshell.console.Completer
import sparklin.kshell.console.ConsoleReader
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class KShellTest {
    private lateinit var repl: KShell
    private var echo: String? = null

    @Before
    fun initRepl() {
        val defs = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                listOf(Shared::class.qualifiedName + ".*"))

        repl = KShell(testConfig, additionalClasspath=findClassJarsOrEmpty(sparklin.kshell.Shared::class),
                sharedHostClassLoader = this.javaClass.classLoader,
                scriptDefinition = defs)
        repl.initEngine()
    }

    @Test
    fun testResultValues() {
        repl.compileAndEval("1 + 1")
        assertEquals("res1: kotlin.Int = 2", echo)
    }

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
            echo = s
        }

        override fun cleanUp() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}