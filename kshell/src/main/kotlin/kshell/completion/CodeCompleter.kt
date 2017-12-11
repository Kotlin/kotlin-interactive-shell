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
//        println("***** s = " + proposals.size)

        if (proposals.isNotEmpty()) {
            candidates!!.addAll(proposals.map { it.completion })
            return cursor
        } else return -1
    }
}