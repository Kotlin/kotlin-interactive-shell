
package com.github.khud.sparklin.kshell.plugins

import com.github.khud.sparklin.kshell.parser.KotlinLexer
import com.github.khud.sparklin.kshell.parser.KotlinParser
import com.github.khud.sparklin.kshell.parser.KotlinParserListenerForHighlighting
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CodePointCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle

class KotlinHighlighter(private val styles: SyntaxPlugin.HighlightStyles): BaseHighlighter {

    override fun highlight(buffer: String, offset: Int): AttributedString {
        require(offset >= 0)
        val code = buffer.substring(offset)

        val codeStream: CodePointCharStream = CharStreams.fromString(code)
        val lexer = KotlinLexer(codeStream)
        val tokens = CommonTokenStream(lexer)
        val kotlinParser = KotlinParser(tokens)
        val listener = KotlinParserListenerForHighlighting()
        kotlinParser.addParseListener(listener)
        kotlinParser.script()

        val sb = AttributedStringBuilder()
        if (offset != 0) sb.append(buffer.substring(0, offset))
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
            sb.style(st)
            sb.append(code[i])
        }
        return sb.toAttributedString()
    }
}
