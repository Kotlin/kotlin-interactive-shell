package sparklin.kshell.console

import org.junit.Assert.assertEquals
import org.junit.Test
import sparklin.kshell.repl.ReplTestBase
import sparklin.kshell.org.jline.utils.AttributedString
import sparklin.kshell.org.jline.utils.AttributedStyle
import sparklin.kshell.org.jline.utils.AttributedStyle.*
import sparklin.kshell.plugins.KotlinHighlighter
import sparklin.kshell.plugins.SyntaxPlugin

class KotlinHighlighterTest : ReplTestBase() {
    private val keyword = "keyword"
    private val function = "function"
    private val type = "type"
    private val string = "string"
    private val stringTemplate = "stringTemplate"
    private val number = "number"

    private val styles: Map<String, AttributedStyle?> = mapOf(
            keyword to BOLD.foreground(RED),
            function to DEFAULT.foreground(YELLOW),
            type to DEFAULT.foreground(MAGENTA),
            string to DEFAULT.foreground(GREEN),
            stringTemplate to BOLD.foreground(YELLOW),
            number to DEFAULT.foreground(CYAN))

    private val mnemonics = listOf(
        'k' to keyword,
        'f' to function,
        't' to type,
        's' to string,
        '$' to stringTemplate,
        'n' to number
    )

    class HighlightStylesFromMap(styles: Map<String, AttributedStyle?>): SyntaxPlugin.HighlightStyles {
        override val keyword: AttributedStyle? by styles
        override val function: AttributedStyle? by styles
        override val type: AttributedStyle? by styles
        override val string: AttributedStyle? by styles
        override val stringTemplate: AttributedStyle? by styles
        override val number: AttributedStyle? by styles
    }

    private val stylesToMnemonics = mnemonics.map {
        (m, e) ->
        styles[e]!! to m
    }.toMap()

    @Test
    fun testFunction() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker, styles.filter(listOf(keyword, function)))
        assertEquals("kkk f(x: Int) = x", ht.highlight("fun g(x: Int) = x").mnemonics())
    }

    @Test
    fun testType() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker,
                styles.filter(listOf(type)))
        assertEquals("fun g(x: ttt): ttt = x", ht.highlight("fun g(x: Int): Int = x").mnemonics())
        assertEquals("val x: tttttt = 1.0", ht.highlight("val x: Double = 1.0").mnemonics())
    }

    @Test
    fun testString() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker,
                styles.filter(listOf(string, stringTemplate)))
        assertEquals("sssssss\$\$world\$s\$xs", ht.highlight("\"hello \${world} \$x\"").mnemonics())
    }

    @Test
    fun testNumber() {
        val ht = KotlinHighlighter(repl.state, repl.compiler.checker,
                styles.filter(listOf(number)))
        assertEquals("val x: Double = nnn", ht.highlight("val x: Double = 1.0").mnemonics())
        assertEquals("val x: Double = nnnn", ht.highlight("val x: Double = 1.0f").mnemonics())
        assertEquals("println(nnnn)", ht.highlight("println(1000)").mnemonics())
    }

    private fun  Map<String, AttributedStyle?>.filter(list: List<String>): HighlightStylesFromMap =
            HighlightStylesFromMap(map { if (list.contains(it.key)) Pair(it.key, it.value) else Pair(it.key, null) }.toMap())

    private fun AttributedString.mnemonics(): String {
        val sb = StringBuilder()
        for (i in indices) {
            sb.append(stylesToMnemonics.getOrDefault(styleAt(i), this[i]))
        }
        return sb.toString()
    }
}