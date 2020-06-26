
package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.parser.KotlinLexer
import com.github.khud.sparklin.kshell.parser.KotlinParser
import com.github.khud.sparklin.kshell.parser.KotlinParserListenerForHighlighting
import org.antlr.v4.runtime.*
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.util.regex.Pattern

class KotlinHighlighter(private val styles: SyntaxPlugin.HighlightStyles): BaseHighlighter {

    override fun highlight(buffer: String, offset: Int): AttributedString {
        require(offset >= 0)
        return AttributedStringBuilder().run {
            buildHighlighting(buffer, offset)
            toAttributedString()
        }
    }

    private fun AttributedStringBuilder.buildHighlighting(buffer: String, offset: Int) {
        if (offset >= buffer.length) return
        val code = buffer.substring(offset)
        if (code.isBlank()) return

        val codeStream: CodePointCharStream = CharStreams.fromString(code)
        val lexer = KotlinLexer(codeStream)
        val tokens = CommonTokenStream(lexer)
        val listener = KotlinParserListenerForHighlighting()
        with (KotlinParser(tokens)) {
            addParseListener(listener)
            removeErrorListeners()
            script()
        }

        if (offset != 0) append(buffer.substring(0, offset))
        for (i in code.indices) {
            val element = listener.result.firstOrNull { i >= it.start && i <= it.end }?.element
            val st = when (element) {
                null -> null
                KotlinParserListenerForHighlighting.RecogizedElements.Keyword -> styles.keyword
                KotlinParserListenerForHighlighting.RecogizedElements.Function -> styles.function
                KotlinParserListenerForHighlighting.RecogizedElements.Number -> styles.number
                KotlinParserListenerForHighlighting.RecogizedElements.String -> styles.string
                KotlinParserListenerForHighlighting.RecogizedElements.StringTemplate -> styles.stringTemplate
                KotlinParserListenerForHighlighting.RecogizedElements.Type -> styles.type
                KotlinParserListenerForHighlighting.RecogizedElements.Parenthesis -> styles.parenthesis
                KotlinParserListenerForHighlighting.RecogizedElements.TypeParameter -> styles.typeParameter
                else -> null
            } ?: AttributedStyle.DEFAULT
            style(st)
            append(code[i])
        }
    }

    override fun setErrorPattern(p0: Pattern?) {}

    override fun setErrorIndex(p0: Int) {}
}
