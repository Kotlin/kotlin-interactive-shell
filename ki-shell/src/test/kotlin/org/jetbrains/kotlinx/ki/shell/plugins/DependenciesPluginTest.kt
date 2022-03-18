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
    fun testUsernameAndPasswordEqualSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username=user password=pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testUsernameOnlyEqualSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username=user barepassword")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testPasswordOnlyEqualSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven bareusername password=pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testUsernameWithEqualEqualSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username=my=user password=pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=my=user\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testPasswordWithEqualEqualSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username=user password=my=pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=my=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testUsernameAndPasswordSemicolonSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username:user password:pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testUsernameOnlySemicolonSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username:user barepassword")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testPasswordOnlySemicolonSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven bareusername password:pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testUsernameWithSemicolonSemicolonSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username:my:user password:pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=my:user\", \"password=pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testPasswordWithSemicolonSemicolonSeparator() {
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven username:user password:my:pass")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=user\", \"password=my:pass\"])",
            extractSnippetFromCommand(command)
        )
    }

    @Test
    fun testFilenamePassed() {
        val filePath = "./src/test/resources/valid.credentials"
        val command = repoCommand.execute(":repository https://packages.team/path/to/maven $filePath")
            as Command.Result.RunSnippets

        assertEquals(
            "@file:Repository(\"https://packages.team/path/to/maven\", options = [\"username=fileusername\", \"password=file password\"])",
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
