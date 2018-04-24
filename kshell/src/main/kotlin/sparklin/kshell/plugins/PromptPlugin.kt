package sparklin.kshell.plugins

import sparklin.kshell.BaseCommand
import sparklin.kshell.KShell
import sparklin.kshell.Plugin
import sparklin.kshell.calcHumanReadableSize
import sparklin.kshell.configuration.Configuration
import java.time.LocalTime

class PromptPlugin: Plugin {
    inner class Prompt(conf: Configuration): BaseCommand() {
        override val name: String by conf.get(default = "prompt")
        override val short: String? by conf.getNullable()
        override val description: String = "customize prompt"
        override val params: String = "[pattern]"

        private val pattern: String by conf.get(default = "[%l]")
        private val incomplete: String by conf.get(default = "...")

        init {
            this@PromptPlugin.pattern = pattern
            this@PromptPlugin.incomplete = incomplete
        }

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            if (p > 0) {
                this@PromptPlugin.pattern = line.substring(p + 1).trim()
            } else {
                help()
            }
        }

        override fun help(): String {
            return "no available"
        }

    }

    lateinit var repl: KShell
    lateinit var conf: Configuration

    lateinit var pattern: String
    lateinit var incomplete: String

    private val types = mutableMapOf(
            "l" to { "${repl.state.lineIndex.get()}" },
            "u" to { System.getProperty("user.name") },
            "d" to ::formattedTime,
            "t" to ::totalMemory,
            "m" to ::maxMemory)

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl
        this.conf = config

        repl.prompt = ::promptFunc
        repl.registerCommand(Prompt(config))
    }

    private fun formattedTime(): String = LocalTime.now().toString()

    private fun totalMemory() = calcHumanReadableSize(Runtime.getRuntime().totalMemory())

    private fun maxMemory() = calcHumanReadableSize(Runtime.getRuntime().maxMemory())

    private fun promptFunc(): String = if (repl.incompleteLines.isNotEmpty()) incomplete else format("$pattern ", types)

    fun registerCustomType(type: String, display: ()  -> String) {
        // TODO: check conflicts
        types[type] = display
    }

    override fun cleanUp() { }

    companion object {
        internal fun format(p: String, types: Map<String, () -> String>): String {
            var state = 0
            val sb = StringBuilder()
            val tb = StringBuilder()

            for (ch in p) {
                when (ch) {
                    '%' -> state = 1
                    '{' -> when (state) {
                        1 -> state = 2
                        else -> sb.append(ch)
                    }
                    '}' -> when (state) {
                        2 -> {
                            val t = tb.toString()
                            sb.append(types.getOrDefault(t, { "%{$t}" })())
                            tb.setLength(0)
                            state = 0
                        }
                        else -> sb.append(ch)
                    }
                    else -> when (state) {
                        1 -> {
                            sb.append(types.getOrDefault(ch.toString(), { "%$ch" })())
                            state = 0
                        }
                        2 -> tb.append(ch)
                        else -> sb.append(ch)
                    }

                }
            }
            return sb.toString()
        }
    }
}