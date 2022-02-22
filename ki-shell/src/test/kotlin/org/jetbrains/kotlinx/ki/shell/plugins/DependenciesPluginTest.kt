package org.jetbrains.kotlinx.ki.shell.plugins

import junit.framework.TestCase
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import org.junit.Test

class DependenciesPluginTest : TestCase() {

    private val repoCommand = DependenciesPlugin().RepositoryCommand(object : ReplConfigurationBase() {})

    private fun extractSnippetFromCommand(command: Command.Result.RunSnippets): String {
        return command.snippetsToRun.toList()[0]
    }

    @Test
    fun testWhenRepoUrlNotPassed() {
        val command = repoCommand.execute(":repository ") as Command.Result.RunSnippets

        assertEquals("@file:Repository(\"\")", extractSnippetFromCommand(command))
    }

    @Test
    fun testWhenRepoUrlPassed() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\")",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testUsernameAndPasswordPassed() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven user pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testFilenamePassed() {
        val filePath = "./src/test/resources/valid.credentials"
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven $filePath")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=fileusername\", \"password=filepassword\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testNotFoundFilename() {
        val filePath = "blabla"
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven $filePath")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=\", \"password=\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testMissingCredentialsFile() {
        val filePath = "./src/test/resources/missing.credentials"
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven $filePath")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=\", \"password=\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testCredentialsFileIsDirectory() {
        val filePath = "./src/test/resources"
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven $filePath")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=\", \"password=\"])",
            extractSnippetFromCommand(command)
        )
    }
}
