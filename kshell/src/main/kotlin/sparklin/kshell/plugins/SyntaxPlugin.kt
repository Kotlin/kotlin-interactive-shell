package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.configuration.BooleanConverter
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.Converter
import sparklin.kshell.org.jline.reader.LineReader
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
            repl.highlighter.apply {
                syntaxHighlighter = kotlinHighlighter
                reader.option(LineReader.Option.DISABLE_HIGHLIGHTER, !on)
            }
        }

        override fun execute(line: String) {
            val args = line.replace('\t', ' ').split(' ')

            if (args.size != 2) {
                println(help())
                return
            }

            when (args[1]) {
                "on" -> reader.option(LineReader.Option.DISABLE_HIGHLIGHTER, false)
                "off" -> reader.option(LineReader.Option.DISABLE_HIGHLIGHTER, true)
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
    lateinit var kotlinHighlighter: KotlinHighlighter
    lateinit var reader: LineReader

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl
        this.reader = repl.reader
        kotlinHighlighter = KotlinHighlighter(repl.state, repl::checker, HighlightStylesFromConfiguration(config))

        repl.registerCommand(Syntax(config))
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