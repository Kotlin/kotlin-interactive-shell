package sparklin.kshell.console

import org.junit.Assert.assertEquals
import org.junit.Test
import sparklin.kshell.repl.ReplTestBase
import sparklin.kshell.plugins.KotlinHighlighter.KotlinElement.*
import sparklin.kshell.org.jline.utils.AttributedString
import sparklin.kshell.org.jline.utils.AttributedStyle.*
import sparklin.kshell.plugins.KotlinHighlighter

class KotlinHighlighterTest : ReplTestBase() {
    private val styles = mapOf(
            KEYWORD to BOLD.foreground(RED),
            FUNCTION to DEFAULT.foreground(YELLOW),
            TYPE to DEFAULT.foreground(MAGENTA),
            STRING to DEFAULT.foreground(GREEN),
            STRING_TEMPLATE to BOLD.foreground(YELLOW),
            NUMBER to DEFAULT.foreground(CYAN))

    private val mnemonics = listOf(
        'k' to KEYWORD,
        'f' to FUNCTION,
        't' to TYPE,
        's' to STRING,
        '$' to STRING_TEMPLATE,
        'n' to NUMBER
    )

    private val stylesToMnemonics = mnemonics.map {
        (m, e) ->
        styles[e]!! to m
    }.toMap()

    @Test
    fun testFunction() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker, styles.filter(listOf(KEYWORD, FUNCTION)))
        assertEquals("kkk f(x: Int) = x", ht.highlight("fun g(x: Int) = x").mnemonics())
    }

    @Test
    fun testType() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker,
                styles.filter(listOf(TYPE)))
        assertEquals("fun g(x: ttt): ttt = x", ht.highlight("fun g(x: Int): Int = x").mnemonics())
        assertEquals("val x: tttttt = 1.0", ht.highlight("val x: Double = 1.0").mnemonics())
    }

    @Test
    fun testString() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker,
                styles.filter(listOf(STRING, STRING_TEMPLATE)))
        assertEquals("sssssss\$\$world\$s\$xs", ht.highlight("\"hello \${world} \$x\"").mnemonics())
    }

    @Test
    fun testNumber() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker,
                styles.filter(listOf(NUMBER)))
        assertEquals("val x: Double = nnn", ht.highlight("val x: Double = 1.0").mnemonics())
        assertEquals("val x: Double = nnnn", ht.highlight("val x: Double = 1.0f").mnemonics())
        assertEquals("println(nnnn)", ht.highlight("println(1000)").mnemonics())
    }

    private fun <T> Map<KotlinHighlighter.KotlinElement, T>.filter(list: List<KotlinHighlighter.KotlinElement>): Map<KotlinHighlighter.KotlinElement, T> =
            filter { list.contains(it.key) }

    private fun AttributedString.mnemonics(): String {
        val sb = StringBuilder()
        for (i in indices) {
            sb.append(stylesToMnemonics.getOrDefault(styleAt(i), this[i]))
        }
        return sb.toString()
    }
}