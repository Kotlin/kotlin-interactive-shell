package kshell.command

import lib.jline.console.completer.Completer
import kshell.Command
import java.io.File

/**
 * Load and interpret Kotlin script.
 */
open class Load : Command("load", "l", "load script", params = "<path>") {
    override fun execute(line: String) {
        val p = line.indexOf(' ')
        val path = line.substring(p + 1).trim()
        val content = File(path).readLines().joinToString(separator = "\n")
        repl!!.compileAndEval(content)
    }

    override fun completer(): Completer = FileCompleter()
}