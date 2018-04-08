package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.console.ConsoleReader
import sparklin.kshell.repl.DeclarationSnippet
import sparklin.kshell.repl.EvalError
import sparklin.kshell.repl.Result

class RuntimePlugin : Plugin {
    inner class InferType(fullName: String, shortName: String, description: String):
        BaseCommand(fullName, shortName, description) {

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

    inner class PrintSymbols(fullName: String, shortName: String, description: String):
            sparklin.kshell.BaseCommand(fullName, shortName, description) {

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

        val inferTypeCmdName = config.getLocal("inferTypeCmd", "name", "type")
        val inferTypeCmdShort = config.getLocal("inferTypeCmd", "short", "t")
        val inferTypeCmdDescription = "display the type of an expression without evaluating it"

        val printSymbolsCmdName = config.getLocal("printSymbolsCmd", "name", "symbols")
        val printSymbolsCmdShort = config.getLocal("printSymbolsCmd", "short", "s")
        val printSymbolsCmdDescription = "list defined symbols"

        repl.registerCommand(InferType(inferTypeCmdName, inferTypeCmdShort, inferTypeCmdDescription))
        repl.registerCommand(PrintSymbols(printSymbolsCmdName, printSymbolsCmdShort, printSymbolsCmdDescription))
    }

    private fun Configuration.getLocal(cmd: String, key: String, default: String) =
            this.get("${LoadFilePlugin::class.qualifiedName!!}.$cmd.$key", default)

    override fun cleanUp() { }
}

