package kshell

import kshell.console.Completer

interface Command {
    fun execute(line: String)

    fun match(line: String): Boolean

    fun completer(): Completer

    fun help(): String

    fun desc(): String
}