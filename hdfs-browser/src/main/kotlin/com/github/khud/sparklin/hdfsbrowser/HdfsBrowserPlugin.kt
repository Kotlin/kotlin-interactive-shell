package com.github.khud.sparklin.hdfsbrowser

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import com.github.khud.sparklin.kshell.BaseCommand
import com.github.khud.sparklin.kshell.KShell
import com.github.khud.sparklin.kshell.Plugin
import com.github.khud.sparklin.kshell.calcHumanReadableSize
import com.github.khud.sparklin.kshell.configuration.Configuration
import com.github.khud.sparklin.kshell.plugins.SparkPlugin
import kotlin.reflect.KClass
import org.apache.hadoop.conf.Configuration as HadoopConfiguration


class HdfsBrowserPlugin : Plugin {
    private lateinit var repl: KShell
    private lateinit var fs: FileSystem
    private var workingDirectory = "."

    inner class LsCommand(conf: Configuration) : BaseCommand() {
        override val name: String by conf.get(default = "hdfs.ls")
        override val short: String? by conf.getNullable()
        override val description: String = "list files in HDFS directory"

        override val params: String = "[-h] [<path>]"

        override fun execute(line: String) {
            val p = line.indexOf(' ')
            val parseResult = parseOpts(if (p < 0) "" else line.substring(p + 1).trim(), "h")
            when (parseResult) {
                is ParseResult.ParseError -> println(parseResult.msg)
                is ParseResult.ParsedOptions -> {
                    val path = if (parseResult.other.isBlank()) workingDirectory else parseResult.other
                    listFiles(path, parseResult.opts.isNotEmpty())
                }
            }
        }
    }

    override fun init(repl: KShell, config: Configuration) {
        this.repl = repl
        this.fs = FileSystem.get(findHadoopConfiguration(config))

        repl.registerCommand(LsCommand(config))
    }

    private fun findHadoopConfiguration(config: Configuration): HadoopConfiguration {
        val spark1 = config.getPluginByClass("com.github.khud.sparklin.spark1x.Spark1xPlugin", SparkPlugin::class)
        val spark2 = config.getPluginByClass("com.github.khud.sparklin.spark2x.Spark2xPlugin", SparkPlugin::class)

        return when {
            spark1 != null -> spark1.hadoopConfiguration()
            spark2 != null -> spark2.hadoopConfiguration()
            else -> throw IllegalStateException("Can't find Spark plugin")
        } ?: throw IllegalStateException("No Hadoop configuration available")
    }


    private inline fun <reified T: Plugin> Configuration.getPluginByClass(klassName: String, type: KClass<T>): T? {
        return this.getPlugin(klassName) as? T
    }

    private fun listFiles(path: String, isHumanReadable: Boolean) {
        fs.listStatus(Path(path)).forEach {
            val summary = fs.getContentSummary(it.path)
            val size = if (isHumanReadable) calcHumanReadableSize(summary.length) else summary.length.toString()
            println(String.format("%-20s%s", size, it.path))
        }
    }

    override fun cleanUp() { }

    companion object {
        sealed class ParseResult {
            data class ParseError(val msg: String) : ParseResult()
            data class ParsedOptions(val opts: String, val other: String) : ParseResult()
        }

        fun parseOpts(line: String, options: String): ParseResult {
            if (!line.startsWith("-")) return ParseResult.ParsedOptions("", line)

            if (line.trim().length == 1) return ParseResult.ParseError("Missing options")

            val p = line.indexOf(' ')
            val opts = line.substring(1, if (p < 0) line.length else p)
            val unrecognized = opts.filter { !options.contains(it) }
            if (unrecognized.isNotEmpty()) return ParseResult.ParseError("Unrecognized options: $unrecognized")

            return ParseResult.ParsedOptions(opts, line.substring(if (p < 0) line.length else p + 1).trim())
        }
    }
}