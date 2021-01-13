package org.jetbrains.kotlinx.ki.shell.plugins

import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString

interface BaseHighlighter: Highlighter {
    fun highlight(buffer: String, offset: Int = 0): AttributedString

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        return highlight(buffer)
    }
}