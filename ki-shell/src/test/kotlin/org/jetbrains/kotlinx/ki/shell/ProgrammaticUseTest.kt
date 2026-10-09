package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.script.experimental.api.ResultWithDiagnostics

// The scenarios of "Using the Shell from Code" in README.md
class ProgrammaticUseTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun Shell.evalAndPrint(code: String) {
        val result = eval(code)
        if (result.getStatus() == ResultWrapper.Status.SUCCESS) {
            handleSuccess(result.result as ResultWithDiagnostics.Success<*>)
        } else {
            handleError(result.result, result.isCompiled)
        }
    }

    @Test
    fun evalWithoutTerminal() {
        val shell = KotlinShell.createShell(KotlinShell.configuration(), classpath = listOf(tmp.compileGreeter()))
        val out = captureOut {
            shell.initEngine(interactive = false)
            try {
                shell.evalAndPrint("listOf(1, 2).sum()")
                shell.evalAndPrint("greet.Greeter.hello(\"code\")")
                shell.evalAndPrint("val x: Int = \"s\"")
                shell.evalAndPrint("res0 + 1")
            } finally {
                shell.cleanUp()
            }
        }
        val lines = out.lines()
        assertTrue(out, "res0: Int = 3" in lines)
        assertTrue(out, "res1: String = Hello, code" in lines)
        assertTrue(out, lines.any { "Initializer type mismatch" in it })
        assertTrue(out, "res3: Int = 4" in lines)
    }

    @Test
    fun runScriptFile() {
        val script = tmp.newFile("script.kts").apply { writeText("println(listOf(1, 2).sum())") }
        val shell = KotlinShell.createShell(KotlinShell.configuration())
        var exitCode = -1
        val out = captureOut {
            exitCode = try {
                shell.runScript(script)
            } finally {
                shell.cleanUp()
            }
        }
        assertEquals(0, exitCode)
        assertEquals("3", out.trim())
    }

    @Test
    fun mainReturnsOnSuccess() {
        val script = tmp.newFile("main.kts").apply { writeText("println(\"from main\")") }
        val out = captureOut { KotlinShell.main(arrayOf("-f", script.path)) }
        assertEquals("from main", out.trim())
    }
}
