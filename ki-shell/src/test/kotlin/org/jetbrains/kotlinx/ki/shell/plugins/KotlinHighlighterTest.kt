package org.jetbrains.kotlinx.ki.shell.plugins

import org.junit.Test
import junit.framework.TestCase
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.*

class KotlinHighlighterTest : TestCase() {
    private val keyword = "keyword"
    private val function = "function"
    private val type = "type"
    private val string = "string"
    private val stringTemplate = "stringTemplate"
    private val number = "number"
    private val parenthesis = "parenthesis"
    private val typeParameter = "typeParameter"

    private val styles: Map<String, AttributedStyle?> = mapOf(
            keyword to BOLD.foreground(RED),
            function to DEFAULT.foreground(YELLOW),
            type to DEFAULT.foreground(MAGENTA),
            string to DEFAULT.foreground(GREEN),
            stringTemplate to BOLD.foreground(YELLOW),
            number to DEFAULT.foreground(CYAN),
            parenthesis to DEFAULT.foreground(BRIGHT),
            typeParameter to DEFAULT.foreground(BLUE))

    private val mnemonics = listOf(
        'k' to keyword,
        'f' to function,
        't' to type,
        's' to string,
        '$' to stringTemplate,
        'n' to number,
        'p' to parenthesis,
        'T' to typeParameter
    )

    class HighlightStylesFromMap(styles: Map<String, AttributedStyle?>): SyntaxPlugin.HighlightStyles {
        override val keyword: AttributedStyle? by styles
        override val function: AttributedStyle? by styles
        override val type: AttributedStyle? by styles
        override val string: AttributedStyle? by styles
        override val stringTemplate: AttributedStyle? by styles
        override val number: AttributedStyle? by styles
        override val parenthesis: AttributedStyle? by styles
        override val typeParameter: AttributedStyle? by styles
        override val identifier: AttributedStyle? by styles
    }

    private val stylesToMnemonics = mnemonics.map {
        (m, e) ->
        styles[e]!! to m
    }.toMap()

    @Test
    fun testFunction() {
        val ht = KotlinHighlighter(styles.filter(listOf(keyword, function)))
        assertEquals("kkkkf(x: Int) = x", ht.highlight("fun g(x: Int) = x").mnemonics())
    }

    @Test
    fun testType() {
        val ht = KotlinHighlighter( styles.filter(listOf(type)))
        assertEquals("fun g(x: ttt): ttt = x", ht.highlight("fun g(x: Int): Int = x").mnemonics())
        assertEquals("val x: tttttt = 1.0", ht.highlight("val x: Double = 1.0").mnemonics())
    }

    @Test
    fun testString() { // FIXME
//        val ht = KotlinHighlighter( styles.filter(listOf(string, stringTemplate)))
//        assertEquals("sssssss\$\$world\$s\$xs", ht.highlight("\"hello \${world} \$x\"").mnemonics())
    }

    @Test
    fun testNumber() {
        val ht = KotlinHighlighter( styles.filter(listOf(number)))
        assertEquals("val x: Double = nnn", ht.highlight("val x: Double = 1.0").mnemonics())
        assertEquals("val x: Double = nnnn", ht.highlight("val x: Double = 1.0f").mnemonics())
        assertEquals("println(nnnn)", ht.highlight("println(1000)").mnemonics())
    }

    @Test
    fun testOffset() {
        val ht = KotlinHighlighter( styles.filter(listOf(number)))
        assertEquals(":t n + n", ht.highlight(":t 1 + 1", 2).mnemonics())
    }

    @Test
    fun testBrackets() {
        val ht = KotlinHighlighter( styles.filter(listOf(parenthesis)))
        assertEquals("2 * p1 + 1p", ht.highlight("2 * (1 + 1)").mnemonics())
    }

    @Test
    fun testTypeParameter() {
        val ht = KotlinHighlighter( styles.filter(listOf(typeParameter)))
        assertEquals("fun <T, T> const(x: P, y: K): P = x", ht.highlight("fun <P, K> const(x: P, y: K): P = x").mnemonics())
    }

    @Test
    fun testIfElse() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kk (1 > 2) 11 kkkk kk (1 > 2) { 12 } kkkk { 13 }",
            ht.highlight("if (1 > 2) 11 else if (1 > 2) { 12 } else { 13 }").mnemonics()
        )
    }

    @Test
    fun testIfNoElse() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kk (1 > 2) { 11 }",
            ht.highlight("if (1 > 2) { 11 }").mnemonics()
        )
    }

    @Test
    fun testIfNoSpace() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kk(1 > 2) { 11 }",
            ht.highlight("if(1 > 2) { 11 }").mnemonics()
        )
    }

    @Test
    fun testForLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kkk (x in \"gg\") x + 1",
            ht.highlight("for (x in \"gg\") x + 1").mnemonics()
        )
    }

    @Test
    fun testForNoSpaceLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kkk(x in \"gg\") x + 1",
            ht.highlight("for(x in \"gg\") x + 1").mnemonics()
        )
    }

    @Test
    fun testWhileLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kkkkk (1 > 2) { 1 + 2 }",
            ht.highlight("while (1 > 2) { 1 + 2 }").mnemonics()
        )
    }

    @Test
    fun testWhileNoSpaceLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kkkkk(1 > 2) { 1 + 2 }",
            ht.highlight("while(1 > 2) { 1 + 2 }").mnemonics()
        )
    }

    @Test
    fun testDoWhileLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kkk{ 1 + 2 } kkkkk (1 > 2)",
            ht.highlight("do { 1 + 2 } while (1 > 2)").mnemonics()
        )
    }

    @Test
    fun testDoNoSpaceWhileLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kk{ 1 + 2 } kkkkk (1 > 2)",
            ht.highlight("do{ 1 + 2 } while (1 > 2)").mnemonics()
        )
    }

    @Test
    fun testDoWhileNoSpaceLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "kkk{ 1 + 2 } kkkkk(1 > 2)",
            ht.highlight("do { 1 + 2 } while(1 > 2)").mnemonics()
        )
    }

    @Test
    fun testInvalidDoNoWhileLoop() {
        val ht = KotlinHighlighter( styles.filter(listOf(keyword, parenthesis)) )
        assertEquals(
            "do { 1 + 2 } 11",
            ht.highlight("do { 1 + 2 } 11").mnemonics()
        )
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
