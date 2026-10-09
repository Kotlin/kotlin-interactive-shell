package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class CliOptionsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @get:Rule
    val shells = TestShells()

    private class Output(val exitCode: Int, val out: String, val err: String)

    private fun runKi(vararg args: String): Output {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val oldOut = System.out
        val oldErr = System.err
        System.setOut(PrintStream(out, true))
        System.setErr(PrintStream(err, true))
        val exitCode = try {
            KotlinShell.run(args)
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
        return Output(exitCode, out.toString(), err.toString())
    }

    private fun Shell.value(code: String): Any? = evalValue(code).value

    private fun Shell.evalErrors(code: String): String {
        val wrapper = eval(code)
        assertEquals(ResultWrapper.Status.ERROR, wrapper.getStatus())
        return wrapper.result.reports.joinToString("\n") { it.message }
    }

    private fun Shell.command(line: String): Command.Result = commands.first { it.match(line) }.execute(line)

    private val optInCode = "mapOf(1 to 2).entries.first().copy().key"

    @Test
    fun helpExitsWithZero() {
        val output = runKi("-h")
        assertEquals(0, output.exitCode)
        assertTrue(output.out, output.out.startsWith("Usage: ki"))
        listOf("--file", "--classpath", "--compiler-option").forEach { assertTrue(output.out, it in output.out) }
        assertEquals(0, runKi("--help").exitCode)
    }

    @Test
    fun unknownOptionExitsWithError() {
        val output = runKi("--unknown")
        assertEquals(2, output.exitCode)
        assertTrue(output.out, "Unexpected positional argument: '--unknown'" in output.out)
    }

    @Test
    fun optionsAreParsed() {
        val options = KotlinShellOptions(
            arrayOf(
                "-cp", "a${File.pathSeparator}b", "--classpath=c",
                "--compiler-option", "-opt-in=x", "--compiler-option=-Xfoo=bar",
                "-f", "s.kts"
            )
        )
        assertEquals(listOf("a", "b", "c").map(::File), options.classpath)
        assertEquals(listOf("-opt-in=x", "-Xfoo=bar"), options.compilerOptions)
        assertEquals("s.kts", options.script)
        assertEquals(false, options.version)
    }

    @Test
    fun classpathOption() {
        val shell = shells.create(classpath = listOf(tmp.compileGreeter()))
        assertEquals("Hello, cp", shell.value("greet.Greeter.hello(\"cp\")"))
    }

    @Test
    fun missingClasspathEntriesAreRejected() {
        val missing = File(tmp.root, "missing.jar")
        val output = runKi("-cp", "${tmp.root}${File.pathSeparator}$missing", "-f", tmp.newFile("unused.kts").path)
        assertEquals(2, output.exitCode)
        assertTrue(output.err, "not found: $missing" in output.err)

        val shell = shells.create()
        assertTrue(shell.command(":classpath add $missing") is Command.Result.Failure)
    }

    @Test
    fun classpathAddCommand() {
        val shell = shells.create()
        shell.value("1")
        val classes = tmp.compileGreeter()
        assertTrue(shell.command(":classpath add ${classes.path}") is Command.Result.Success)
        assertEquals("Hello, add", shell.value("greet.Greeter.hello(\"add\")"))
        assertTrue(classes.path in (shell.command(":cp") as Command.Result.Success).message.orEmpty())
    }

    @Test
    fun classpathAddCommandBeforeFirstSnippet() {
        val shell = shells.create()
        assertTrue(shell.command(":classpath add ${tmp.compileGreeter().path}") is Command.Result.Success)
        assertEquals("Hello, first", shell.value("greet.Greeter.hello(\"first\")"))
    }

    @Test
    fun compilerOption() {
        assertTrue(shells.create().evalErrors(optInCode).contains("opt-in"))
        val shell = shells.create(compilerOptions = listOf("-opt-in=kotlin.ExperimentalStdlibApi"))
        assertEquals(1, shell.value(optInCode))
        assertEquals(1, shell.value(optInCode))
    }

    @Test
    fun compilerOptionsAnnotationAppliesToItsSnippet() {
        val shell = shells.create()
        val classes = tmp.compileGreeter()
        val code = """
            @file:CompilerOptions("-opt-in=kotlin.ExperimentalStdlibApi")
            @file:DependsOn("${classes.path.replace("\\", "\\\\")}")
            greet.Greeter.hello("${'$'}{$optInCode}")
        """.trimIndent()
        assertEquals("Hello, 1", shell.value(code))
        assertTrue(shell.evalErrors(optInCode).contains("opt-in"))
    }

    @Test
    fun explicitCompilerPluginKeepsCompletion() {
        val allOpen = File("target/test-compiler-plugins/kotlin-allopen-compiler-plugin-embeddable.jar")
        assertTrue("$allOpen is missing", allOpen.isFile)
        val shell = shells.create(compilerOptions = listOf("-Xplugin=${allOpen.absolutePath}"))
        assertEquals(3, shell.value("listOf(1, 2).sum()"))
        val code = "\"abc\".len"
        assertTrue(shell.ideServices.complete(code, code.length).any { it.text == "length" })
    }

    @Test
    fun completionPluginLocation() {
        val classes = tmp.newFolder("classes")
        assertEquals(classes, KotlinShell.completionPluginLocation(classes))
        val pluginJar = tmp.newFile("ki-completion-plugin-0.6.0.jar")
        assertEquals(pluginJar, KotlinShell.completionPluginLocation(pluginJar))

        val lib = tmp.newFolder("lib")
        val shadedJar = File(lib, "ki-shell.jar").apply { createNewFile() }
        assertEquals(shadedJar, KotlinShell.completionPluginLocation(shadedJar))
        val shippedPluginJar = File(lib, "ki-completion-plugin.jar").apply { createNewFile() }
        assertEquals(shippedPluginJar, KotlinShell.completionPluginLocation(shadedJar))
    }

    @Test
    fun scriptOutputHasNoTerminalSequences() {
        val script = tmp.newFile("plain.kts").apply { writeText("println(1)") }
        assertEquals("1\n", runKi("-f", script.path).out.replace("\r\n", "\n"))
    }

    @Test
    fun scriptFile() {
        val classes = tmp.compileGreeter()
        val script = tmp.newFile("hello.kts").apply {
            writeText(
                """
                @file:DependsOn("${classes.path.replace("\\", "\\\\")}")
                val names = listOf("a", "b")
                println(greet.Greeter.hello(names.joinToString()))
                names.size
                """.trimIndent()
            )
        }
        val output = runKi("-f", script.path)
        assertEquals(output.err, 0, output.exitCode)
        assertEquals("Hello, a, b", output.out.trim())
    }

    @Test
    fun scriptFileErrors() {
        val compileError = tmp.newFile("error.kts").apply { writeText("val x: Int = \"s\"") }
        val output = runKi("-f", compileError.path)
        assertEquals(1, output.exitCode)
        assertTrue(output.err, "Initializer type mismatch" in output.err && "(error.kts:1:" in output.err)

        val exception = tmp.newFile("exception.kts").apply { writeText("println(\"before\")\nerror(\"boom\")") }
        val exceptionOutput = runKi("-f", exception.path)
        assertEquals(1, exceptionOutput.exitCode)
        assertEquals("before", exceptionOutput.out.trim())
        assertTrue(exceptionOutput.err, "IllegalStateException: boom" in exceptionOutput.err)

        assertEquals(2, runKi("-f", File(tmp.root, "missing.kts").path).exitCode)
    }
}
