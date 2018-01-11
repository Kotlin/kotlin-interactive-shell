package kshell.completion

import lib.jline.console.completer.Completer

class CodeCompleter(val kotlinCompletion: KotlinCompletion): Completer {
    override fun complete(buffer: String?, cursor: Int, candidates: MutableList<CharSequence>?): Int {
        // buffer can be null
        checkNotNull<List<CharSequence>>(candidates)

        if (buffer == null) {
            return -1 // FIXME: should never happen
        }

        val proposals = kotlinCompletion.generateCompletionProposals(buffer, cursor)

        if (proposals.isNotEmpty()) {
            candidates!!.addAll(proposals.map { it.completion })
            return buffer.skipFromRight() + 1
        } else return -1
    }
}

fun String.skipFromRight(): Int {
    var i = this.length - 1
    while (i >= 0 && (this[i].isJavaIdentifierPart() || this[i].isJavaIdentifierStart())) i--
    return i //maxOf(i, 0)
}