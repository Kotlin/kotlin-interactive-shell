package com.github.khud.sparklin.kshell.plugins

import sparklin.kshell.org.jline.reader.Highlighter
import sparklin.kshell.org.jline.reader.LineReader
import sparklin.kshell.org.jline.utils.AttributedString

interface BaseHighlighter: Highlighter {
    fun highlight(buffer: String, offset: Int = 0): AttributedString

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        return highlight(buffer)
    }
}