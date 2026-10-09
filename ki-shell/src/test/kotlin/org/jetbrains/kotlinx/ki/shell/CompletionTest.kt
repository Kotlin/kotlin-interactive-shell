package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompletionTest {
    @get:Rule
    val shells = TestShells()

    private val shell by lazy { shells.create() }

    private fun eval(code: String) {
        shell.evalSuccess(code)
    }

    private fun complete(codeWithCursor: String): List<CompletionItem> {
        val cursor = codeWithCursor.indexOf('|')
        val code = codeWithCursor.removeRange(cursor, cursor + 1)
        return shell.ideServices.complete(code, cursor)
    }

    private fun completeNames(codeWithCursor: String): List<String> = complete(codeWithCursor).map { it.text }

    private fun assertContains(names: List<String>, vararg expected: String) {
        expected.forEach { assertTrue("$it is not in $names", it in names) }
    }

    @Test
    fun membersOfReceiver() {
        val items = complete("\"abc\".len|")
        assertContains(items.map { it.text }, "length")
        assertTrue(items.all { it.text.startsWith("len") })
        assertEquals("Int", items.first { it.text == "length" }.type)
    }

    @Test
    fun extensionsOfReceiver() {
        val names = completeNames("listOf(1).ma|")
        assertContains(names, "map", "mapNotNull", "max")
        assertFalse(names.contains("while"))
    }

    @Test
    fun allMembersAfterDot() {
        assertContains(completeNames("\"abc\".|"), "length", "substring", "uppercase", "let")
    }

    @Test
    fun topLevelCallablesAndKeywords() {
        assertContains(completeNames("pri|"), "println", "print", "private")
        assertContains(completeNames("val l = mutableL|"), "mutableListOf")
        assertFalse(completeNames("requireNonNeg|").contains("requireNonNegativeLimit"))
        assertFalse(completeNames("|").any { it.startsWith("$$") })
    }

    @Test
    fun classifiers() {
        assertContains(completeNames("StringBu|"), "StringBuilder")
    }

    @Test
    fun historyDeclarationsAndResults() {
        eval("val myCounter = 5")
        eval("fun myFunction(i: Int) = i")
        eval("class MyClass(val myProperty: Int)")
        eval("42")
        assertContains(completeNames("my|"), "myCounter", "myFunction")
        assertContains(completeNames("My|"), "MyClass")
        assertContains(completeNames("MyClass(1).my|"), "myProperty")
        val results = complete("re|")
        assertTrue(results.toString(), results.any { it.text.startsWith("res") })
        assertFalse(results.toString(), results.any { it.type.orEmpty().contains("__ki_") })
    }

    @Test
    fun historyExtensions() {
        eval("fun String.shout() = uppercase() + \"!\"")
        assertContains(completeNames("\"a\".sh|"), "shout")
    }

    @Test
    fun localsAndLambdaParameters() {
        assertContains(completeNames("val localValue = 1\nlocal|"), "localValue")
        assertContains(completeNames("listOf(\"a\").map { it.len|"), "length")
        assertContains(completeNames("listOf(1).forEach { element -> ele|"), "element")
        assertContains(completeNames("buildString { appe|"), "append")
    }

    @Test
    fun qualifiers() {
        assertContains(completeNames("Integer.MAX|"), "MAX_VALUE")
        assertContains(completeNames("kotlin.math.ab|"), "abs")
    }

    @Test
    fun incompleteCode() {
        assertContains(completeNames("println(listOf(1, 2).fi|"), "filter", "first")
        assertContains(completeNames("if (\"a\".isNotE|"), "isNotEmpty")
    }

    @Test
    fun noCompletionInsideStringsAndComments() {
        assertTrue(completeNames("\"pri|").isEmpty())
        assertTrue(completeNames("// pri|").isEmpty())
    }

    @Test
    fun closingBracketsOfIncompleteCode() {
        assertEquals("", closingBrackets("val x = foo(1).bar"))
        assertEquals(")}", closingBrackets("run { listOf(1).map(it"))
        assertEquals("}\"", closingBrackets("\"a\${b.c"))
        assertEquals(")", closingBrackets("f(\"(\", '(', /* ( */ x"))
        assertEquals(null, closingBrackets("\"\"\"text"))
        assertEquals(null, closingBrackets("x /* comment"))
    }

    @Test
    fun completionDoesNotAffectHistoryOrNumbering() {
        eval("val a = 1")
        val snippetNo = shell.currentSnippetNo.get()
        completeNames("a.|")
        completeNames("val b = 2\nb.|")
        assertEquals(snippetNo, shell.currentSnippetNo.get())
        assertEquals(ResultWrapper.Status.ERROR, shell.eval("b").getStatus())
        eval("a + 1")
    }

    @Test
    fun completionAfterFailedSnippet() {
        assertEquals(ResultWrapper.Status.ERROR, shell.eval("val broken: Int = \"s\"").getStatus())
        assertFalse(completeNames("bro|").contains("broken"))
        eval("val fixed = 1")
        assertContains(completeNames("fix|"), "fixed")
    }
}
