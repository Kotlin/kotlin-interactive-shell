package sparklin.kshell

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import sparklin.kshell.configuration.CachedInstance
import sparklin.kshell.configuration.Configuration
import sparklin.kshell.configuration.ConfigurationImpl
import sparklin.kshell.repl.Repl
import sparklin.kshell.repl.findRequiredJarFiles
import java.io.File
import java.net.URLClassLoader

object KotlinShell {
    @JvmStatic
    fun main(args: Array<String>) {
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

        val repl = KShell(Disposer.newDisposable(), configuration(), conf, messageCollector, classpath, baseClassloader)

        Runtime.getRuntime().addShutdownHook(Thread({
            println("\nBye!")
            repl.cleanUp()
        }))

        repl.doRun()
    }

    fun configuration(): Configuration {
        val instance = CachedInstance<Configuration>()
        val klassName: String? = System.getProperty("config.class")

        return if (klassName != null) {
            instance.load(klassName, Configuration::class)
        } else {
            instance.get { ConfigurationImpl() }
        }
    }
}