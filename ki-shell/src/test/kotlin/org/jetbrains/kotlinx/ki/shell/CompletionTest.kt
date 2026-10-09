package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    fun declarationsInCurrentSnippet() {
        assertContains(
            completeNames("fun recursiveFunction(n: Int): Int = if (n <= 1) 1 else n * recursiveFun|"),
            "recursiveFunction"
        )
        assertContains(completeNames("class Node { fun create() { No| } }"), "Node")
        assertContains(completeNames("fun String.currentExtension() = 1; \"a\".currentExt|"), "currentExtension")
        assertFalse(completeNames("val selfReference = selfRef|").contains("selfReference"))
        assertContains(completeNames("run { fun localRecursive() { localRec| } }"), "localRecursive")
    }

    @Test
    fun nullableReceiverOnlyOffersApplicableExtensions() {
        eval("val nullableString: String? = null")

        val regularCall = completeNames("nullableString.|")
        assertFalse(regularCall.contains("length"))
        assertContains(regularCall, "isNullOrEmpty", "let")
        assertContains(completeNames("nullableString?.|"), "length")
    }

    @Test
    fun genericExtensionsMatchReceiverArguments() {
        val stringList = completeNames("listOf(\"a\").|")
        assertFalse(stringList.contains("sum"))
        assertFalse(stringList.contains("flatten"))
        assertContains(completeNames("listOf(listOf(1)).flatt|"), "flatten")
    }

    @Test
    fun javaPackageClassifiersArePublicTopLevelNames() {
        val classes = complete("java.util.|").filter { it.kind == CompletionItem.Kind.CLASS }.map { it.text }
        assertContains(classes, "ArrayList", "HashMap")
        assertFalse(classes.toString(), classes.any { '$' in it })
        assertFalse(classes.toString(), "ArrayPrefixHelpers" in classes)
    }

    @Test
    fun shellsCompleteAgainstTheirOwnHistory() {
        val first = shells.create()
        val second = shells.create()
        first.evalSuccess("val firstShellOnly = 1")
        second.evalSuccess("val secondShellOnly = 2")

        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstResult = executor.submit<List<String>> {
                start.await()
                first.ideServices.complete("firstShellO", "firstShellO".length).map { it.text }
            }
            val secondResult = executor.submit<List<String>> {
                start.await()
                second.ideServices.complete("secondShellO", "secondShellO".length).map { it.text }
            }
            start.countDown()
            val firstNames = firstResult.get(60, TimeUnit.SECONDS)
            val secondNames = secondResult.get(60, TimeUnit.SECONDS)

            assertContains(firstNames, "firstShellOnly")
            assertFalse(firstNames.contains("secondShellOnly"))
            assertContains(secondNames, "secondShellOnly")
            assertFalse(secondNames.contains("firstShellOnly"))
        } finally {
            executor.shutdownNow()
        }
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
