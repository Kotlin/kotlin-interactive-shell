package org.jetbrains.kotlinx.ki.shell

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class KotlinCompleter(
        val getIdeServices: () -> ReplIdeServices,
        val incompleteLines: ArrayList<String>
) : Completer {

    override fun complete(reader: LineReader?, line: ParsedLine?, candidates: MutableList<Candidate>?) {
        val currentLine = line?.line() ?: return
        val code = (incompleteLines + currentLine).joinToString(separator = "\n")
        val cursor = code.length - currentLine.length + line.cursor()
        getIdeServices().complete(code, cursor).forEach {
            candidates!!.add(Candidate(it.text))
        }
    }
}
