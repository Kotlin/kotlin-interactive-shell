package kshell

import kshell.console.Completer

class ContextDependentCompleter(private val commands: List<Command>,
                                private val actionModeAvailable: () -> Boolean,
                                private val defaultCompleter: Completer): Completer {

    override fun complete(buffer: String, cursor: Int, candidates: MutableList<CharSequence>): Int {
        return if (actionModeAvailable() && buffer.startsWith(":")) {
            val action = commands.find { it.match(buffer) }
            action?.completer()?.complete(buffer, cursor, candidates) ?: -1
        } else {
            defaultCompleter.complete(buffer, cursor, candidates)
        }
    }
}