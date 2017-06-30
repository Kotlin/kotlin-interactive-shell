package sparklin.repl

import org.apache.spark.util.Utils
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.script.templates.standard.ScriptTemplateWithArgs
import kshell.*
import lib.jline.console.history.PersistentHistory

/**
 * Apache Spark extension for Kotlin Shell (KShell).
 */
class SparkRepl(additionalClasspath: List<File>,
                val classesOutputDir: File,
                history: PersistentHistory): KShell(
                additionalClasspath = additionalClasspath,
                sharedHostClassLoader = Utils.getContextOrSparkClassLoader(),
                scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                        ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                        listOf("sparklin.repl.Shared.*")),
                shellHistory = history
        ) {

    override fun afterCompile(compiledClasses: ReplCompileResult.CompiledClasses) {
        // class must be written down on disk
        // to be visible for Spark executors by class server
        compiledClasses.classes.forEach {
            writeClass(classesOutputDir.absolutePath + File.separator + it.path, it.bytes)
        }
    }

    fun writeClass(path: String, bytes: ByteArray) {
        val out = BufferedOutputStream(FileOutputStream(path))
        out.write(bytes)
        out.flush()
        out.close()
    }
}