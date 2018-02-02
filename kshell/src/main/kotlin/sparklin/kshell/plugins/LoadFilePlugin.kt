package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.Plugin
import sparklin.kshell.Repl
import sparklin.kshell.console.Completer
import java.io.File

class LoadFilePlugin: Plugin {
    inner class Load(fullName: String, shortName: String, description: String):
            BaseCommand(fullName, shortName, description) {

        override val params = "<path>"

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val path = line.substring(p + 1).trim()
            val content = File(path).readText()
            repl.compileAndEval(content)
        }

        override fun completer(): Completer = FileCompleter()
    }

    lateinit var repl: Repl

    override fun init(repl: Repl, config: Configuration) {
        this.repl = repl

        val fullName = config.getLocal("name", "load")
        val shortName = config.getLocal("short", "l")
        val description = "load script"

        repl.registerCommand(Load(fullName, shortName, description))
    }

    private fun Configuration.getLocal(key: String, default: String) =
            this.get("${LoadFilePlugin::class.qualifiedName!!}.cmd.$key", default)

    override fun cleanUp() { }
}