package kshell

import lib.jline.console.completer.Completer

/**
 * Base class for all shell commands.
 */
abstract class Command(val cmd: String,
                       val short: String,
                       val description: String,
                       val params: String = "",
                       val ignoreCase: Boolean = true) {
    lateinit var repl: KShell

    open fun init(repl: KShell) {
        this.repl = repl
    }

    internal fun match(line: String): Boolean {
        val ind = line.indexOf(' ')
        val command = if (ind < 0) line else line.substring(0, ind)
        return (short.isNotEmpty() && command.startsWith(":$short", ignoreCase = ignoreCase)) || command.startsWith(":$cmd", ignoreCase = ignoreCase)
    }

    class DefaultCompleter: Completer {
        override fun complete(buffer: String?, cursor: Int, candidates: MutableList<CharSequence>?): Int {
            return -1
        }
    }

    val DEFAULT_COMPLETER = DefaultCompleter()

    abstract fun execute(line: String): Unit

    open fun completer(): Completer = DEFAULT_COMPLETER

    override fun toString(): String {
        return signature() + description
    }

    open fun signature(): String {
        val s = if (short.isNotEmpty()) "or :$short" else ""
        return String.format("%-30s", ":$cmd $s $params")
    }

    open fun help(): String = "(no extended help available)"
}