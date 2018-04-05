package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.Plugin
import sparklin.kshell.console.Completer
import java.io.File

class LoadFilePlugin: Plugin {
    inner class Load(name: String, short: String, description: String):
            BaseCommand(name, short, description) {

        override val params = "<path>"

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val path = line.substring(p + 1).trim()
            val content = File(path).readText()
            repl.eval(content)
        }

        override fun completer(): Completer = FileCompleter()
    }

    lateinit var repl: KShell

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl

        val name = config.getLocal("name", "load")
        val short = config.getLocal("short", "l")
        val description = "load script"

        repl.registerCommand(Load(name, short, description))
    }

    private fun Configuration.getLocal(key: String, default: String) =
            this.get("${LoadFilePlugin::class.qualifiedName!!}.cmd.$key", default)

    override fun cleanUp() { }
}