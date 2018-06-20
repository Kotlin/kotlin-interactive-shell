package com.github.khud.sparklin.kshell

import sparklin.kshell.org.jline.reader.Highlighter

interface Command {
    fun execute(line: String)

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