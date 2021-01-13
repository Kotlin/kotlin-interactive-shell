package org.jetbrains.kotlinx.ki.shell

import org.jline.reader.impl.DefaultParser

class KotlinReplSnippetParser : DefaultParser() {

    val wordDelimiters = ".,?:"

    override fun isDelimiterChar(buffer: CharSequence, pos: Int): Boolean {
        return wordDelimiters.contains(buffer[pos])
    }
}
