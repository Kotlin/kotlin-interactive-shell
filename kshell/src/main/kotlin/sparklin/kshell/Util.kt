package sparklin.kshell

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.utils.PathUtil
import sparklin.kshell.repl.findRequiredJarFiles
import java.io.File
import java.net.URLClassLoader

object Util {
    @JvmStatic
    fun createCompilerConfiguration(classpath: List<File>, additionalClasspath: List<File>, moduleName: String, messageCollector: MessageCollector): CompilerConfiguration {
        return CompilerConfiguration().apply {
            addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File(System.getProperty("java.home"))))
            addJvmClasspathRoots(classpath)
            addJvmClasspathRoots(additionalClasspath)
            put(CommonConfigurationKeys.MODULE_NAME, moduleName)
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        }
    }

    @JvmStatic
    fun findJars(includeScriptEngine: Boolean,
                 includeKotlinCompiler: Boolean,
                 includeStdLib: Boolean) =
            findRequiredJarFiles(includeScriptEngine = includeScriptEngine,
                    includeKotlinCompiler = includeKotlinCompiler,
                    includeStdLib = includeStdLib)
    @JvmStatic
    fun baseClassloader(conf: CompilerConfiguration) = URLClassLoader(conf.jvmClasspathRoots.map { it.toURI().toURL() }
            .toTypedArray(), this.javaClass.classLoader)
}