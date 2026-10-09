package org.jetbrains.kotlinx.ki.shell

import org.jline.reader.Candidate
import org.jline.reader.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.asErrorDiagnostics

class KotlinCompleterTest {
    @get:Rule
    val shells = TestShells()

    private val requests = mutableListOf<Pair<String, Int>>()

    private val shell by lazy {
        shells.create().apply {
            ideServices = object : ReplIdeServices {
                override fun complete(code: String, cursor: Int): List<CompletionItem> {
                    requests += code to cursor
                    return listOf(
                        CompletionItem("substring", CompletionItem.Kind.FUNCTION, "String", "(startIndex: Int)"),
                        CompletionItem("length", CompletionItem.Kind.PROPERTY, "Int"),
                        CompletionItem("String", CompletionItem.Kind.CLASS),
                        CompletionItem("length", CompletionItem.Kind.FUNCTION, "Int", "()"),
                    )
                }

                override fun inferType(expr: String): ResultWithDiagnostics<String> =
                    ResultWithDiagnostics.Failure("unused".asErrorDiagnostics())
            }
        }
    }

    private fun complete(lineWithCursor: String): List<Candidate> {
        val cursor = lineWithCursor.indexOf('|')
        val line = lineWithCursor.removeRange(cursor, cursor + 1)
        val parsed = shell.parser.parse(line, cursor, Parser.ParseContext.COMPLETE)
        return mutableListOf<Candidate>().also { shell.completer.complete(null, parsed, it) }
    }

    private fun word(lineWithCursor: String): String {
        val cursor = lineWithCursor.indexOf('|')
        val line = lineWithCursor.removeRange(cursor, cursor + 1)
        return shell.parser.parse(line, cursor, Parser.ParseContext.COMPLETE).word()
    }

    @Test
    fun candidatesShowParametersAndTypes() {
        val candidates = complete("\"abc\".|").associateBy { it.value() }
        assertEquals(listOf("substring", "length", "String"), candidates.keys.toList())
        candidates.getValue("substring").let {
            assertEquals("substring(startIndex: Int)", it.displ())
            assertEquals("String", it.descr())
            assertTrue(it.complete())
        }
        candidates.getValue("length").let {
            assertEquals("length", it.displ())
            assertEquals("Int", it.descr())
        }
        assertEquals(null, candidates.getValue("String").descr())
    }

    @Test
    fun codeAndCursorArePassedToServices() {
        complete("val x = \"abc\".le|ngth")
        assertEquals(listOf("val x = \"abc\".length" to 16), requests)
    }

    @Test
    fun incompleteLinesArePrepended() {
        shell.incompleteLines += listOf("val x = listOf(", "1,")
        complete("2).si|")
        assertEquals(listOf("val x = listOf(\n1,\n2).si" to 24), requests)
    }

    @Test
    fun commandArgumentsAreCompletedAsCode() {
        complete(":type \"abc\".le|")
        assertEquals(listOf("\"abc\".le" to 8), requests)
        requests.clear()

        assertTrue(complete(":ty|").isEmpty())
        assertTrue(complete(":type|").isEmpty())
        assertEquals(emptyList<Pair<String, Int>>(), requests)

        complete("String::le|")
        complete("::le|")
        assertEquals(listOf("String::le" to 10, "::le" to 4), requests)
    }

    @Test
    fun wordsAreSplitOnOperatorsAndPunctuation() {
        assertEquals("pri", word("val x = pri|"))
        assertEquals("ma", word("listOf(1).ma|"))
        assertEquals("le", word(":type \"abc\".le|"))
        assertEquals("b", word("a+b|"))
        assertEquals("", word("f(|"))
    }
}
