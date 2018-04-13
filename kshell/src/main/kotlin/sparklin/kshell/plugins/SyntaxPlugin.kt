package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.configuration.BooleanConverter
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.ContextHighlighter
import sparklin.kshell.org.jline.utils.AttributedStyle.*
import  sparklin.kshell.plugins.KotlinHighlighter.KotlinElement.*

class SyntaxPlugin: Plugin {
    inner class Syntax(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "syntax")
        override val short: String by conf.get(default = "s")
        override val description: String = "syntax highlighting"
        private val on: Boolean by conf.get(BooleanConverter, default = true)

        override val params: String = "{on | off}"

        init {
            if (on) repl.highlighter.bind(ContextHighlighter.Context.CODE, syntaxHighlighter)
        }

        override fun execute(line: String) {
            val args = line.replace('\t', ' ').split(' ')

            if (args.size != 2) {
                println(help())
                return
            }

            when (args[1]) {
                "on" -> repl.highlighter.bind(ContextHighlighter.Context.CODE, syntaxHighlighter)
                "off" -> repl.highlighter.default(ContextHighlighter.Context.CODE)
                else -> {
                    println("Unknown option ${args[1]}")
                    println(help())
                }
            }
        }

        override fun help(): String = """
            Enables syntax highlighting
            :$name on - enable highlighting
            :$name off - disable highlighting""".trimIndent()
    }

    lateinit var repl: KShell
    lateinit var syntaxHighlighter: KotlinHighlighter
    private val styles = mapOf(
            KEYWORD to BOLD.foreground(RED),
            FUNCTION to DEFAULT.foreground(YELLOW),
            TYPE to DEFAULT.foreground(MAGENTA),
            STRING to DEFAULT.foreground(GREEN),
            STRING_TEMPLATE to BOLD.foreground(YELLOW),
            NUMBER to DEFAULT.foreground(CYAN))

    override fun init(repl: KShell, conf: Configuration) {
        this.repl = repl
        syntaxHighlighter = KotlinHighlighter(repl.state, repl.compiler.checker, styles)

        repl.registerCommand(Syntax(conf))
    }

    override fun cleanUp() { }
}