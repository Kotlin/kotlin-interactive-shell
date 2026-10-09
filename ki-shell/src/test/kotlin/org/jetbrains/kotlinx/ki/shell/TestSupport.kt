package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.tools.ToolProvider
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.jvm.KJvmEvaluatedSnippet
import kotlin.script.experimental.util.LinkedSnippet

class TestShells : ExternalResource() {
    private val shells = mutableListOf<Shell>()

    fun create(): Shell =
        KotlinShell.createShell(object : ReplConfigurationBase() {})
            .apply { initEngine() }
            .also { shells += it }

    override fun after() {
        shells.forEach { it.cleanUp() }
    }
}

@Suppress("UNCHECKED_CAST")
internal val ResultWrapper.evaluatedSnippet: KJvmEvaluatedSnippet
    get() = ((result as ResultWithDiagnostics.Success<*>).value as LinkedSnippet<KJvmEvaluatedSnippet>).get()

internal fun Shell.evalSuccess(code: String): ResultValue {
    val wrapper = eval(code)
    assertEquals(wrapper.result.reports.joinToString("\n") { it.render() }, ResultWrapper.Status.SUCCESS, wrapper.getStatus())
    return wrapper.evaluatedSnippet.result
}

internal fun Shell.evalValue(code: String): ResultValue.Value {
    val result = evalSuccess(code)
    assertTrue("Expected a value for '$code' but got $result", result is ResultValue.Value)
    return result as ResultValue.Value
}

internal fun captureOut(body: () -> Unit): String {
    val original = System.out
    val buffer = ByteArrayOutputStream()
    System.setOut(PrintStream(buffer, true))
    try {
        body()
    } finally {
        System.setOut(original)
    }
    return buffer.toString()
}

// The class `greet.Greeter` with `static String hello(String)`
internal fun TemporaryFolder.compileGreeter(): File {
    val sources = newFolder("src", "greet")
    val source = File(sources, "Greeter.java").apply {
        writeText("package greet; public class Greeter { public static String hello(String n) { return \"Hello, \" + n; } }")
    }
    val classes = newFolder("classes")
    val exitCode = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.path, source.path)
    assertEquals(0, exitCode)
    return classes
}
