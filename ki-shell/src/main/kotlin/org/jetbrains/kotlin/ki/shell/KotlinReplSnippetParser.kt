package org.jetbrains.kotlin.ki.shell

import org.jline.reader.impl.DefaultParser

class KotlinReplSnippetParser : DefaultParser() {

    val wordDelimiters = ".,?:"

    override fun isDelimiterChar(buffer: CharSequence, pos: Int): Boolean {
        return super.isDelimiterChar(buffer, pos) || wordDelimiters.contains(buffer[pos])
    }
}
