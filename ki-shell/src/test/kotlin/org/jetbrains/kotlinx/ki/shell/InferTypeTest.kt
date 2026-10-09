package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.script.experimental.api.ResultWithDiagnostics

class InferTypeTest {
    @get:Rule
    val shells = TestShells()

    private val shell by lazy { shells.create() }

    private fun eval(code: String) {
        shell.evalSuccess(code)
    }

    private fun typeOf(expr: String): String {
        val result = shell.ideServices.inferType(expr)
        assertTrue("No type for '$expr': ${result.reports.joinToString { it.render() }}", result is ResultWithDiagnostics.Success)
        return (result as ResultWithDiagnostics.Success).value
    }

    @Test
    fun typesOfExpressions() {
        assertEquals("Int", typeOf("1"))
        assertEquals("List<Int>", typeOf("listOf(1)"))
        assertEquals("UInt", typeOf("1u"))
        assertEquals("Map<String, Int>", typeOf("mapOf(\"a\" to 1)"))
    }

    @Test
    fun typesOfHistoryDeclarations() {
        eval("val text = \"abc\"")
        eval("fun twice(i: Int) = i * 2L")
        assertEquals("String", typeOf("text"))
        assertEquals("Long", typeOf("twice(text.length)"))
    }

    @Test
    fun expressionIsNotEvaluated() {
        val out = captureOut { assertEquals("Unit", typeOf("println(\"side effect\")")) }
        assertFalse(out, out.contains("side effect"))
    }

    @Test
    fun errorsAreReportedWithoutProbeInternals() {
        val result = shell.ideServices.inferType("undefinedName")
        assertTrue(result is ResultWithDiagnostics.Failure)
        val messages = result.reports.map { it.message }
        assertTrue(messages.toString(), messages.any { "undefinedName" in it })
        assertFalse(messages.toString(), messages.any { "__ki_" in it })
    }

    @Test
    fun historyAndNumberingAreUnchanged() {
        eval("val a = 1")
        val snippetNo = shell.currentSnippetNo.get()
        typeOf("a")
        assertEquals(snippetNo, shell.currentSnippetNo.get())
        assertEquals(ResultWrapper.Status.ERROR, shell.eval("__ki_type__").getStatus())
        eval("a + 1")
    }

    @Test
    fun typeCommandPrintsType() {
        val command = shell.listCommands().first { it.name == "type" }
        val out = captureOut { command.execute(":type listOf(\"a\")") }
        assertEquals("List<String>", out.trim())
    }
}
