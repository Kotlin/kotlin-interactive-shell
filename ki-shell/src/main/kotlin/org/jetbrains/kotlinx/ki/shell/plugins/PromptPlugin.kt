package org.jetbrains.kotlinx.ki.shell.plugins

import org.jetbrains.kotlinx.ki.shell.*
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import java.net.InetAddress
import java.time.LocalTime

class PromptPlugin: Plugin {
    inner class Prompt(conf: ReplConfiguration): BaseCommand() {
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

        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            if (p > 0) {
                this@PromptPlugin.pattern = line.substring(p + 1).trim()
            } else {
                println(this@PromptPlugin.pattern)
            }
            return Command.Result.Success()
        }

        override fun help(): String {
            val help = types.toSortedMap().map {
                val type = it.value
                "%${type.type}\t${type.help}"
            }.joinToString(separator = "\n|")
            return """
                |:prompt [format]
                |
                |The command customizes prompt.
                |Format can contain following specials:
                |$help
                """.trimMargin()
        }

    }

    lateinit var repl: Shell
    lateinit var conf: ReplConfiguration

    lateinit var pattern: String
    lateinit var incomplete: String

    data class PromptType(val type: String, val display: () -> String, val help: String)

    private val types = mutableMapOf(
            "l" to PromptType("l", { "${repl.currentSnippetNo.get()}" }, "line number"),
            "u" to PromptType("u", { System.getProperty("user.name") }, "user name"),
            "h" to PromptType("h", { InetAddress.getLocalHost().hostName }, "host name"),
            "d" to PromptType("d", { formattedTime() }, "current time"),
            "t" to PromptType("t", { totalMemory() }, "total memory"),
            "m" to PromptType("m", { maxMemory() }, "maximum memory"),
            "e" to PromptType("e", { evalTime() }, "evaluation time")
    )

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl
        this.conf = config

        repl.prompt = ::promptFunc
        repl.registerCommand(Prompt(config))
    }

    private fun formattedTime(): String = LocalTime.now().toString()

    private fun totalMemory() = calcHumanReadableSize(Runtime.getRuntime().totalMemory())

    private fun maxMemory() = calcHumanReadableSize(Runtime.getRuntime().maxMemory())

    private fun evalTime() = String.format("%.2fs", repl.evaluationTimeMillis / 1000.0)

    private fun promptFunc(): String = if (repl.incompleteLines.isNotEmpty()) incomplete else format("$pattern ", types.mapValues { it.value.display })

    fun registerCustomType(promptType: PromptType) {
        // TODO: check conflicts
        types[promptType.type] = promptType
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