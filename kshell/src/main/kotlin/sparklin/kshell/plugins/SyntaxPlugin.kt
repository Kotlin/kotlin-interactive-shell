package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.configuration.BooleanConverter
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.ContextHighlighter
import sparklin.kshell.configuration.Converter
import sparklin.kshell.org.jline.utils.AttributedStyle
import sparklin.kshell.org.jline.utils.AttributedStyle.*

class SyntaxPlugin: Plugin {
    inner class Syntax(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "syntax")
        override val short: String? by conf.getNullable()
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

    override fun init(repl: KShell, conf: Configuration) {
        this.repl = repl
        syntaxHighlighter = KotlinHighlighter(repl.state, repl.compiler.checker, HighlightStylesFromConfiguration(conf))

        repl.registerCommand(Syntax(conf))
    }

    override fun cleanUp() { }

    interface HighlightStyles {
        val keyword: AttributedStyle?
        val function: AttributedStyle?
        val type: AttributedStyle?
        val string: AttributedStyle?
        val stringTemplate: AttributedStyle?
        val number: AttributedStyle?
    }

    class HighlightStylesFromConfiguration(conf: Configuration): HighlightStyles {
        override val keyword: AttributedStyle by conf.get(StyleConverter, BOLD.foreground(RED))
        override val function: AttributedStyle by conf.get(StyleConverter, DEFAULT.foreground(YELLOW))
        override val type: AttributedStyle by conf.get(StyleConverter, DEFAULT.foreground(MAGENTA))
        override val string: AttributedStyle by conf.get(StyleConverter, DEFAULT.foreground(GREEN))
        override val stringTemplate: AttributedStyle by conf.get(StyleConverter, BOLD.foreground(YELLOW))
        override val number: AttributedStyle by conf.get(StyleConverter, DEFAULT.foreground(CYAN))
    }

    object StyleConverter: Converter<AttributedStyle> {
        // style: bold; color: yellow; background-color: black;
        override fun convert(s: String): AttributedStyle {
            val parsed = s.split(';').map {
                val (k, v) = it.split(':')
                k.trim() to v.trim()
            }.toMap()
            val attributedStyle = style(parsed.getOrDefault("style", "default"))
            parsed["color"]?.let { attributedStyle.foreground(color(it)) }
            parsed["background-color"]?.let { attributedStyle.background(color(it)) }
            return attributedStyle
        }

        private fun style(s: String) = when (s) {
            "default" -> DEFAULT
            "bold" -> BOLD
            "inverse" -> INVERSE
            else -> throw IllegalArgumentException("Unsupported style: $s")
        }

        private fun color(s: String) = when (s) {
            "black" -> BLACK
            "red" -> RED
            "green" -> GREEN
            "yellow" -> YELLOW
            "blue" -> BLUE
            "magenta" -> MAGENTA
            "cyan" -> CYAN
            "white" -> WHITE
            "bright" -> BRIGHT
            else -> throw IllegalArgumentException("Unsupported color: $s")
        }
    }
}