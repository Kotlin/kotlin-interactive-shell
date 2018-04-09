package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.IdentityConverter
import sparklin.kshell.console.ConsoleReader
import sparklin.kshell.repl.DeclarationSnippet
import sparklin.kshell.repl.EvalError
import sparklin.kshell.repl.PropertySnippet
import sparklin.kshell.repl.Result

class RuntimePlugin : Plugin {
    inner class InferType(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "type")
        override val short: String by conf.get(default = "t")
        override val description: String = "display the type of an expression without evaluating it"

        override val params = "<expr>"

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val expr = line.substring(p + 1).trim()

            val compileResult = repl.compile(expr)
            when (compileResult) {
                is Result.Incomplete ->
                    console.println("Incomplete line")
                is Result.Error ->
                    repl.handleError(EvalError.CompileError(compileResult.error.message))
                is Result.Success -> {
                    compileResult.data.classes.type?.let {
                        console.println(it)
                    }
                }
            }
        }
    }

    inner class ListSymbols(conf: Configuration) : sparklin.kshell.BaseCommand() {
        override val name: String by conf.get(default = "list")
        override val short: String? by conf.get(default = "ls")
        override val description: String = "list defined symbols"

        override fun execute(line: String) {
            repl.state.history.forEach {
                if (it is DeclarationSnippet && !it.shadowed) {
                    println(it.name)
                }
            }
        }
    }

    private lateinit var repl: KShell
    private lateinit var console: ConsoleReader

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl
        this.console = config.getConsoleReader()

        repl.registerCommand(InferType(config))
        repl.registerCommand(ListSymbols(config))
    }

    private fun Configuration.getLocal(cmd: String, key: String, default: String) =
            this.get("${LoadFilePlugin::class.qualifiedName!!}.$cmd.$key", default)

    override fun cleanUp() { }
}

