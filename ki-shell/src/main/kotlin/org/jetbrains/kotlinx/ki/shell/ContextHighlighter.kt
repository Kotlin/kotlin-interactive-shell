package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.plugins.BaseHighlighter
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import java.util.regex.Pattern

class ContextHighlighter(private val isSyntaxMode: (String) -> Boolean,
                         private val findCommand: (String) -> Command?): Highlighter {

    var syntaxHighlighter: BaseHighlighter = DEFAULT

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        if (isSyntaxMode(buffer)) return syntaxHighlighter.highlight(reader, buffer)
        val highlighter = findCommand(buffer)?.highlighter() ?: DEFAULT
        return highlighter.highlight(reader, buffer)
    }

    override fun setErrorPattern(p0: Pattern?) {
        TODO("Not yet implemented")
    }

    override fun setErrorIndex(p0: Int) {
        TODO("Not yet implemented")
    }

    companion object {
        private val DEFAULT = object : BaseHighlighter {
            override fun highlight(buffer: String, offset: Int): AttributedString {
                val sb = AttributedStringBuilder()
                sb.append(buffer)
                return sb.toAttributedString()
            }

            override fun setErrorPattern(p0: Pattern?) {
                TODO("Not yet implemented")
            }

            override fun setErrorIndex(p0: Int) {
                TODO("Not yet implemented")
            }
        }
    }
}