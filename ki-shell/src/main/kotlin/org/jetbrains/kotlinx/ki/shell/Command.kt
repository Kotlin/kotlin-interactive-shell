package org.jetbrains.kotlinx.ki.shell

import org.jline.reader.Highlighter

interface Command {

    sealed class Result(val message: String? = null) {
        class Failure(message: String) : Result(message)
        class Success(message: String? = null) : Result(message)
        class RunSnippets(val snippetsToRun: Iterable<String>) : Result()
    }

    fun execute(line: String): Result

    val name: String

    val short: String?

    fun help(): String

    fun desc(): String

    fun highlighter(): Highlighter? = null
}

fun Command.match(line: String): Boolean = match(line, { x, y -> x.equals(y, ignoreCase = true) })

fun Command.weakMatch(line: String): Boolean = match(line, { x, y -> x.startsWith(y, ignoreCase = true) })

inline fun Command.match(line: String, func: (String, String) -> Boolean): Boolean {
    val ind = line.indexOf(' ')
    val command = if (ind < 0) line else line.substring(0, ind)
    return (short != null && func(command, ":$short")) || func(command, ":$name")
}