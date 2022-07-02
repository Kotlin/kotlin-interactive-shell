package org.jetbrains.kotlinx.ki.shell.plugins

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlinx.ki.shell.parser.KotlinLexer
import org.jetbrains.kotlinx.ki.shell.parser.KotlinParser
import org.jetbrains.kotlinx.ki.shell.parser.KotlinParserListenerForHighlighting
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.util.regex.Pattern

class KotlinHighlighter(private val styles: SyntaxPlugin.HighlightStyles): BaseHighlighter {

    private var lastCodeCausingError: String? = null

    override fun highlight(buffer: String, offset: Int): AttributedString {
        require(offset >= 0)
        return AttributedStringBuilder().run {
            buildHighlighting(buffer, offset)
            toAttributedString()
        }
    }

    private fun AttributedStringBuilder.buildHighlighting(buffer: String, offset: Int) {
        if (offset != 0) append(buffer.substring(0, offset))
        if (offset >= buffer.length) return
        val code = buffer.substring(offset)
        if (code.isBlank()) return

        val listener = KotlinParserListenerForHighlighting()

        if (lastCodeCausingError != code) {
            lastCodeCausingError = null
            try {
                with(KotlinParserForHighlighting(code)) {
                    addParseListener(listener)
                    script()
                }
            } catch (e: Throwable) {
                lastCodeCausingError = code
                throw e
            }
        }

        for (i in code.indices) {
            val element = listener.result.firstOrNull { i >= it.start && i <= it.end }?.element
            val st = when (element) {
                null -> null
                KotlinParserListenerForHighlighting.RecogizedElements.Keyword -> styles.keyword
                KotlinParserListenerForHighlighting.RecogizedElements.FunctionIdentifier -> styles.function
                KotlinParserListenerForHighlighting.RecogizedElements.Number -> styles.number
                KotlinParserListenerForHighlighting.RecogizedElements.String -> styles.string
                KotlinParserListenerForHighlighting.RecogizedElements.StringTemplate -> styles.stringTemplate
                KotlinParserListenerForHighlighting.RecogizedElements.TypeIdentifier -> styles.type
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

    private class KotlinParserForHighlighting(code: String) :
            KotlinParser(
                CommonTokenStream(
                    KotlinLexer(CharStreams.fromString(code)).also {
                        it.removeErrorListeners()
                    }
                )
            )
    {
        init {
            _buildParseTrees = false
            removeErrorListeners()
        }
    }
}
