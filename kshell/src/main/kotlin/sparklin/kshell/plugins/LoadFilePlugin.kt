package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.Plugin
import sparklin.kshell.console.Completer
import java.io.File

class LoadFilePlugin: Plugin {
    inner class Load(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "load")
        override val short: String by conf.get(default = "l")
        override val description: String = "load file and evaluate"

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

        repl.registerCommand(Load(config))
    }

    private fun Configuration.getLocal(key: String, default: String) =
            this.get("${LoadFilePlugin::class.qualifiedName!!}.cmd.$key", default)

    override fun cleanUp() { }
}