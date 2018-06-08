package com.github.khud.sparklin.kshell

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.utils.PathUtil
import com.github.khud.sparklin.kshell.configuration.CachedInstance
import com.github.khud.sparklin.kshell.configuration.Configuration
import com.github.khud.sparklin.kshell.configuration.ConfigurationImpl
import com.github.khud.kshell.repl.stdlibPathForJar

object KotlinShell {
    @JvmStatic
    fun main(args: Array<String>) {
        val messageCollector: MessageCollector = PrintingMessageCollector(System.out, MessageRenderer.WITHOUT_PATHS, false)

        val repl = KShell(Disposer.newDisposable(),
                configuration(),
                messageCollector,
                listOf(PathUtil.stdlibPathForJar()),
                "kshell",
                KShell::class.java.classLoader)

        repl.addClasspathRoots(replJars())
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