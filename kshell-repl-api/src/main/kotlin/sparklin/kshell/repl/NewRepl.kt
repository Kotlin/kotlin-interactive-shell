package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock

class NewRepl {
    val replCompiler: ReplCompiler
    val replEvaluator: ReplEvaluator
    val state: State

    init {
        val messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false)
        val moduleName = "my-module"
        val additionalClasspath = listOf<File>()
        val classpath = findRequiredJarFiles(
                includeScriptEngine = false,
                includeKotlinCompiler = false,
                includeStdLib = true,
                includeRuntime = false)

        val conf = CompilerConfiguration().apply {
            addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
            addJvmClasspathRoots(classpath)
            addJvmClasspathRoots(additionalClasspath)
            put(CommonConfigurationKeys.MODULE_NAME, moduleName)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        }

        val baseClassloader = URLClassLoader(conf.jvmClasspathRoots.map { it.toURI().toURL() }
                .toTypedArray(), this.javaClass.classLoader)

        replCompiler = ReplCompiler(
                compilerConfiguration = conf,
                messageCollector = messageCollector)

        replEvaluator = ReplEvaluator(classpath, baseClassloader)
        state = State(ReentrantReadWriteLock())
        println("OK")
    }

    fun eval(no: Int, code: String) {
        val res = replCompiler.compile(state, CodeLine(no, code))
        when (res) {
            is Result.Error -> println("{{{" + res.message)
            is Result.Incomplete -> println("incomplete")
            is Result.Success -> {
                res.data.classes.forEach {
                    println(it.path)
                    writeClass("/Users/vitaly.khudobakhshov/Documents/research_projects/sparklin/temp/" + it.path, it.bytes)
                }
                println("!!!! EVAL")
                println(replEvaluator.eval(state, res.data, null))
            }
        }
    }

    private fun writeClass(path: String, bytes: ByteArray) {
        val out = BufferedOutputStream(FileOutputStream(path))
        out.write(bytes)
        out.flush()
        out.close()
    }
}