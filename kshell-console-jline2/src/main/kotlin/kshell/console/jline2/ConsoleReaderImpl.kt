package kshell.console.jline2

import kshell.configuration.BooleanConverter
import kshell.console.Completer
import kshell.console.ConsoleReader
import kshell.configuration.Configuration
import kshell.configuration.IdentityConverter
import lib.jline.console.history.FileHistory
import java.io.File

import lib.jline.console.ConsoleReader as JLine2ConsoleReader
import lib.jline.console.completer.Completer as JLine2Completer

class ConsoleReaderImpl: ConsoleReader {
    val reader = JLine2ConsoleReader()
    var history: FileHistory? = null

    override fun init(config: Configuration) {
        reader.expandEvents = false

        val klassName = ConsoleReaderImpl :: class
        val persistHistory = config.get("$klassName.persistHistory", BooleanConverter, true)

        if (persistHistory) {
            val historyPath = config.get("$klassName.historyPath", IdentityConverter,
                    System.getProperty("user.home") + File.separator + ".kshell.history")
            history = FileHistory(File(historyPath))
            reader.history = history
        }
    }

    override fun addCompleter(completer: Completer) {
        reader.addCompleter { buffer, cursor, candidates ->
            if (buffer != null) completer.complete(buffer, cursor, candidates!!)
            else -1
        }
    }

    override fun setPrompt(prompt: String) {
        reader.prompt = prompt
    }

    override fun readLine(): String? = reader.readLine()

    override fun println(s: String) = reader.println(s)

    override fun cleanUp() {
        history?.let {
            it.flush()
        }
    }
}