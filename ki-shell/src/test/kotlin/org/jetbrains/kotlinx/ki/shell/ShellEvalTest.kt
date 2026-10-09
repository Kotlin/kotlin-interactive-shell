package org.jetbrains.kotlinx.ki.shell

import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.script.experimental.api.ResultValue

class ShellEvalTest {
    @get:Rule
    val shells = TestShells()

    @get:Rule
    val tmp = TemporaryFolder()

    private val shell by lazy { shells.create() }

    private fun mavenRepository(groupId: String, artifactId: String, version: String, classes: File): File {
        val repository = tmp.newFolder("repository")
        val directory = File(repository, "${groupId.replace('.', '/')}/$artifactId/$version").apply { mkdirs() }
        val jarFile = File(directory, "$artifactId-$version.jar")
        JarOutputStream(jarFile.outputStream()).use { jar ->
            classes.walkTopDown().filter { it.isFile }.forEach { file ->
                jar.putNextEntry(JarEntry(file.relativeTo(classes).invariantSeparatorsPath))
                file.inputStream().use { it.copyTo(jar) }
                jar.closeEntry()
            }
        }
        val pomFile = File(directory, "$artifactId-$version.pom").apply {
            writeText(
                "<project><modelVersion>4.0.0</modelVersion><groupId>$groupId</groupId>" +
                        "<artifactId>$artifactId</artifactId><version>$version</version></project>"
            )
        }
        for (file in listOf(jarFile, pomFile)) {
            val sha1 = MessageDigest.getInstance("SHA-1").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
            File(file.path + ".sha1").writeText(sha1)
        }
        return repository
    }

    @Test
    fun valueDeclaredInEarlierSnippetIsUsableLater() {
        shell.evalSuccess("val x = 1")
        val result = shell.evalValue("x + 1")
        assertEquals(2, result.value)
        assertTrue(result.name, result.name.startsWith("res"))
        assertTrue(result.type, result.type.endsWith("Int"))
    }

    @Test
    fun functionsAndClassesDeclaredInEarlierSnippetsAreUsableLater() {
        shell.evalSuccess("fun twice(i: Int) = i * 2")
        assertEquals(42, shell.evalValue("twice(21)").value)

        shell.evalSuccess("class Point(val x: Int, val y: Int) { fun sum() = x + y }")
        assertEquals(7, shell.evalValue("Point(3, 4).sum()").value)
    }

    @Test
    fun earlierResultsAreReferenceableByName() {
        val first = shell.evalValue("40 + 2")
        assertEquals(42, shell.evalValue("${first.name} + 0").value)
    }

    @Test
    fun incompleteInputIsReportedAndCompletedInputSucceeds() {
        assertEquals(ResultWrapper.Status.INCOMPLETE, shell.eval("fun f() {").getStatus())
        shell.evalSuccess("fun f(): Int {\n  return 1\n}")
        assertEquals(1, shell.evalValue("f()").value)
    }

    @Test
    fun compileErrorsDoNotBreakSubsequentEvaluation() {
        assertEquals(ResultWrapper.Status.ERROR, shell.eval("val y: Int = \"s\"").getStatus())
        assertEquals(2, shell.evalValue("1 + 1").value)

        assertEquals(ResultWrapper.Status.ERROR, shell.eval("undefinedName + 1").getStatus())
        assertEquals(3, shell.evalValue("1 + 2").value)
    }

    @Test
    fun runtimeExceptionIsReportedAsErrorResult() {
        val result = shell.evalSuccess("error(\"boom\")")
        assertTrue("Expected an error result but got $result", result is ResultValue.Error)
        assertEquals("boom", (result as ResultValue.Error).error.message)
    }

    @Test
    fun dependsOnAnnotationResolvesMavenDependency() {
        val repository = mavenRepository("org.jetbrains.kotlinx.ki.test", "greeter", "1.0", tmp.compileGreeter())
        // The resolver reads its local repository from these settings, so nothing is cached in ~/.m2. The property name
        // is `org.apache.maven.user-settings`, relocated by the shading of kotlin-scripting-dependencies-maven-all
        val settings = tmp.newFile("settings.xml").apply {
            writeText("<settings><localRepository>${tmp.newFolder("local-repository").path}</localRepository></settings>")
        }
        val property = "org.jetbrains.kotlin.org.apache.maven.user-settings"
        val previous = System.setProperty(property, settings.path)
        try {
            shell.evalSuccess("@file:Repository(\"${repository.toURI()}\")\n@file:DependsOn(\"org.jetbrains.kotlinx.ki.test:greeter:1.0\")")
        } finally {
            if (previous == null) System.clearProperty(property) else System.setProperty(property, previous)
        }
        assertEquals("Hello, maven", shell.evalValue("greet.Greeter.hello(\"maven\")").value)
        assertTrue(File(tmp.root, "local-repository/org/jetbrains/kotlinx/ki/test/greeter/1.0").isDirectory)
    }

    @Test
    fun interruptStopsRunningEvaluation() {
        var wrapper: ResultWrapper? = null
        val property = "ki.test.evaluation.started"
        System.clearProperty(property)
        val thread = Thread { wrapper = shell.eval("System.setProperty(\"$property\", \"true\")\nwhile (true) { Thread.sleep(10) }") }
        thread.start()
        // Interrupting the compilation instead of the evaluation is not what is tested here
        for (i in 1..300) {
            if (System.getProperty(property) != null) break
            Thread.sleep(100)
        }
        shell.interrupt()
        thread.join(10_000)

        assertFalse("Evaluation was not interrupted", thread.isAlive)
        val finished = wrapper ?: return fail("Evaluation finished without a result")
        when (finished.getStatus()) {
            ResultWrapper.Status.ERROR -> {}
            ResultWrapper.Status.SUCCESS -> {
                val result = finished.evaluatedSnippet.result
                assertTrue("Expected an error result but got $result", result is ResultValue.Error)
                assertTrue((result as ResultValue.Error).error.toString(), result.error is InterruptedException)
            }
            else -> fail("Unexpected status ${finished.getStatus()}")
        }

        assertEquals(3, shell.evalValue("1 + 2").value)
    }

    @Test
    fun warnsWhenInterruptedEvaluationKeepsRunning() {
        val started = "ki.test.abandoned.started"
        val release = "ki.test.abandoned.release"
        val completed = "ki.test.abandoned.completed"
        val repl = this.shell
        listOf(started, release, completed).forEach(System::clearProperty)
        val evaluation = Thread {
            repl.eval(
                "System.setProperty(\"$started\", \"true\")\n" +
                        "while (System.getProperty(\"$release\") == null) { }\n" +
                        "System.setProperty(\"$completed\", \"true\")"
            )
        }
        evaluation.start()

        var abandonedThread: Thread? = null
        var completedValue: String? = null
        try {
            for (attempt in 1..500) {
                if (System.getProperty(started) != null) break
                Thread.sleep(10)
            }
            assertTrue("Evaluation did not start", System.getProperty(started) != null)
            val runningThread = repl.evalThread
            abandonedThread = runningThread
            repl.interrupt()
            assertTrue("Non-cooperative evaluation unexpectedly stopped", runningThread.isAlive)
            evaluation.join(10_000)
            assertFalse("Evaluation caller did not return", evaluation.isAlive)

            val errorOutput = ByteArrayOutputStream()
            val originalErr = System.err
            val failedSnippet = try {
                System.setErr(PrintStream(errorOutput, true))
                repl.eval("val broken: Int = \"s\"")
            } finally {
                System.setErr(originalErr)
            }
            assertEquals(ResultWrapper.Status.ERROR, failedSnippet.getStatus())
            assertTrue(errorOutput.toString(), "interrupted evaluation is still running" in errorOutput.toString())
        } finally {
            System.setProperty(release, "true")
            abandonedThread?.join(10_000)
            evaluation.join(10_000)
            completedValue = System.getProperty(completed)
            listOf(started, release, completed).forEach(System::clearProperty)
        }

        assertFalse("Evaluation caller did not finish", evaluation.isAlive)
        assertFalse("Abandoned evaluation did not stop", abandonedThread?.isAlive ?: false)
        assertEquals("true", completedValue)
    }
}
