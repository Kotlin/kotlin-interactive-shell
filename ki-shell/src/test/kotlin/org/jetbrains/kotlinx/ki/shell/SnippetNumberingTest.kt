package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.script.experimental.api.ScriptDiagnostic

class SnippetNumberingTest {
    @get:Rule
    val shells = TestShells()

    private val shell by lazy { shells.create() }

    private fun resultName(code: String): String = shell.evalValue(code).name

    private fun error(code: String): ScriptDiagnostic {
        val wrapper = shell.eval(code)
        assertEquals(ResultWrapper.Status.ERROR, wrapper.getStatus())
        return wrapper.result.reports.first { it.severity == ScriptDiagnostic.Severity.ERROR }
    }

    @Test
    fun resultNameMatchesSnippetNumberShownBeforeIt() {
        assertEquals(0, shell.currentSnippetNo.get())
        assertEquals("res0", resultName("1"))
        assertEquals(1, shell.currentSnippetNo.get())
        assertEquals("res1", resultName("2"))
    }

    @Test
    fun incompleteInputDoesNotTakeSnippetNumbers() {
        assertEquals(ResultWrapper.Status.INCOMPLETE, shell.eval("listOf(").getStatus())
        assertEquals(ResultWrapper.Status.INCOMPLETE, shell.eval("listOf(\n1,").getStatus())
        assertEquals(0, shell.currentSnippetNo.get())
        assertEquals("res0", resultName("listOf(\n1,\n2)"))
    }

    @Test
    fun errorReportsSnippetNumberAndLineOfUserInput() {
        resultName("1")
        val error = error("val a = 1\nval b: String = a")
        assertEquals("Line_1.kts", error.sourcePath?.substringAfterLast('/')?.substringAfterLast('\\'))
        assertEquals(2, error.location?.start?.line)
        assertEquals(17, error.location?.start?.col)
    }

    @Test
    fun errorInMultiLineInputReportsLineWithinSnippet() {
        assertEquals(ResultWrapper.Status.INCOMPLETE, shell.eval("foo(").getStatus())
        val error = error("foo(\n1,\n2)")
        assertEquals("Line_0.kts", error.sourcePath?.substringAfterLast('/')?.substringAfterLast('\\'))
        assertEquals(1, error.location?.start?.line)
        assertEquals("res1", resultName("3"))
    }
}
