package org.jetbrains.kotlinx.ki.shell

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

class KotlinCompleter(
        val getIdeServices: () -> ReplIdeServices,
        val incompleteLines: ArrayList<String>,
        val isCommandMode: (String) -> Boolean
) : Completer {

    override fun complete(reader: LineReader?, line: ParsedLine?, candidates: MutableList<Candidate>?) {
        var currentLine = line?.line() ?: return
        var cursor = line.cursor()
        if (isCommandMode(currentLine)) {
            val argumentStart = currentLine.indexOf(' ') + 1
            if (argumentStart == 0 || cursor < argumentStart) return
            currentLine = currentLine.substring(argumentStart)
            cursor -= argumentStart
        }
        val code = (incompleteLines + currentLine).joinToString(separator = "\n")
        getIdeServices().complete(code, code.length - currentLine.length + cursor)
            .distinctBy { it.text }
            .forEach {
                candidates!!.add(it.toCandidate())
            }
    }

    private fun CompletionItem.toCandidate(): Candidate =
        Candidate(text, text + parameters.orEmpty(), null, type, null, null, true)
}
