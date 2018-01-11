package kshell

import lib.jline.console.completer.Completer

/**
 * Created by vitaly.khudobakhshov on 18/04/17.
 */
class ContextDependentCompleter(private val commands: List<Command>,
                                private val defaultCompleter: Completer = DUMMY_COMPLETER): Completer {

    companion object {
        val DUMMY_COMPLETER = Completer { _, _, _ -> -1 }
    }

    override fun complete(buffer: String?, cursor: Int, candidates: MutableList<CharSequence>?): Int {
        if (buffer != null) {
            if (buffer.startsWith(":")) {
                val command = commands.filter { cmd -> cmd.match(buffer) }
                if (!command.isEmpty()) {
                    return command[0].completer().complete(buffer, cursor, candidates)
                } else
                    return -1
            } else {
                return defaultCompleter.complete(buffer, cursor, candidates)
            }
        } else {
            return -1
        }
    }
}