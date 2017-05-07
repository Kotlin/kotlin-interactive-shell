package kshell

import lib.jline.console.completer.Completer

/**
 * Created by vitaly.khudobakhshov on 18/04/17.
 */
class ContextDependentCompleter(val commands: List<Command>): Completer {
    override fun complete(buffer: String?, cursor: Int, candidates: MutableList<CharSequence>?): Int {
        if (buffer != null) {
            if (buffer.startsWith(":")) {
                val command = commands.filter { cmd -> cmd.match(buffer) }
                if (!command.isEmpty()) {
                    return command[0].completer().complete(buffer, cursor, candidates)
                } else
                    return -1
            } else return -1 // code completer can be here
        } else {
            return -1
        }
    }
}