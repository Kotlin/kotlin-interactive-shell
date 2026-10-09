# Kotlin Language Interactive Shell

[![Kotlin Stable](https://kotl.in/badges/stable.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains team project](https://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/ki-shell.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.kotlinx%22%20AND%20a:%22ki-shell%22)

The shell is an extensible implementation of Kotlin REPL with a rich set of features including:

- Code completion
- Syntax highlight
- Type inference command
- Downloading dependencies in runtime using Maven coordinates
- List declared symbols

The shell uses the K2 Kotlin compiler (Kotlin 2.5 or newer) and requires JDK 17 or newer to run.


## History

The project is the former Sparklin shell migrated to the new Kotlin scripting and REPL infrastructure and converted to a
generic Kotlin Language Interactive Shell.

The Apache Spark adaptation of it is located in a separate [repository](https://github.com/Kotlin/kotlin-spark-shell).

The previous version of the Sparklin is accessible in the `sparklin` branch.

## Installation and Usage

### Manual

You can download archive from Maven Central: 
1. Go to [Releases](https://github.com/Kotlin/kotlin-interactive-shell/releases) page
2. Download desired version there
3. Unpack it to desired place
4. Launch `bin/ki.sh` for Linux/Mac or `bin\ki.bat` for Windows

### SDKMAN!

Install with [SDKMAN!](https://sdkman.io/) with the following command:

```bash
sdk install ki
```

After installation you can launch `ki` with either `ki` or `ki.sh` comman.

### Arch Linux

On Arch Linux, there's an [AUR package](https://aur.archlinux.org/packages/ki-shell-bin/) available.
After installation, you can run the shell through `ki`.

### Homebrew

Install with [Homebrew](https://brew.sh/) with the following command:
```bash
brew install ki
```

### MacPorts

Install with [MacPorts](https://www.macports.org) with the following command:
```bash
sudo port install ki-shell
```

### Nix

There's a package available in [nixpkgs](https://github.com/NixOS/nixpkgs). Add `kotlin-interactive-shell` to your environment or shell, e.g.:

```bash
nix-shell -p kotlin-interactive-shell
```

After which you can launch with `ki`.

## Build From Source

Building requires JDK 17 or newer. To build from source use:
```bash
git clone https://github.com/Kotlin/kotlin-interactive-shell
cd kotlin-interactive-shell
./mvnw -DskipTests package
```
The build puts the shell into `lib/ki-shell.jar`, the completion plugin into `lib/ki-completion-plugin.jar`, and the
distribution archive into `ki-shell/target/ki-archive.zip`.

Packagers should ship both jars in the same `lib/` directory. When a compiler plugin is given with
`--compiler-option -Xplugin=...`, KI passes `lib/ki-completion-plugin.jar` to the compiler as well; if that jar is
missing, it passes the whole `ki-shell.jar` instead, which works but loads the scripting plugin twice.

The shell is built against a specific Kotlin version, set in `pom.xml`. To build it with a locally built Kotlin
(`2.5.255-SNAPSHOT` installed into the local Maven repository), use:
```bash
./mvnw -Pkotlin-dev-local -DskipTests package
```
To start the shell, run `bin/ki` on Linux and macOS. On Windows, use `bin\ki.bat` instead.
The launchers use `$JAVA_HOME/bin/java` if `JAVA_HOME` is set, or `java` from `PATH` otherwise, and pass `JAVA_OPTS` to it.

To exit the shell, type `:q` or `:quit`.

## Command Line Options

```
ki [-h] [--version] [-f <path>] [-cp <path>] [--compiler-option <option>]
```

- `-f`, `--file <path>`: run the script file without a terminal and exit. The exit code is 1 if the script fails to
  compile or throws an exception.
- `-cp`, `-classpath`, `--classpath <path>`: add jars and directories, separated by `:` (`;` on Windows), to the
  classpath of the snippets.
- `--compiler-option <option>`: pass the option to the compiler, e.g. `--compiler-option -opt-in=kotlin.RequiresOptIn`.
  Can be given more than once.

For example:
```bash
ki -cp libs/my.jar -f script.kts
```

## Commands

Type `:h` to list the commands and `:h <command>` for the help on a command. Some of them:

- `:type <expr>` (`:t`): show the type of the expression without evaluating it.
- `:load <path>` (`:l`): load the file and evaluate it.
- `:paste` (`:p`): enter paste mode.
- `:dependsOn <coordinates>`, `:repository <url>`: add a Maven dependency or repository, see below.
- `:classpath` (`:cp`): show the classpath; `:classpath add <path>` adds jars and directories to it.

Press Tab to complete members, extensions, declarations from earlier snippets (including `res*` results) and imports.

The file annotations `@file:DependsOn(...)`, `@file:Repository(...)` and `@file:CompilerOptions(...)` work in snippets
and in `-f` scripts. `@file:CompilerOptions` applies only to the snippet it is in.

## Prompt

The prompt is set with `:prompt <pattern>`; `:prompt` without a pattern prints the current one. The default is `[%l]`.
The pattern can contain the following specials, also as `%{x}`:

| Special | Value                                  |
|---------|----------------------------------------|
| `%l`    | number of the next snippet (line)      |
| `%u`    | user name                              |
| `%h`    | host name                              |
| `%d`    | current time                           |
| `%t`    | total memory                           |
| `%m`    | maximum memory                         |
| `%e`    | evaluation time of the last snippet    |

For example, `:prompt %u@%h [%l]`.

## Configuration

The shell reads its configuration on start from the properties file `~/.ki-shell`. Another file can be given with the
`KI_CONFIG` environment variable or the `config.path` system property (`JAVA_OPTS=-Dconfig.path=...`).

In the shell, `:conf -v` lists the parameters with their values, and `:set <name> <value>` changes one; the name can be
shortened to any unique suffix, e.g. `:set sayHello false`. Most parameters are read when the shell starts, so put them
into the file to change the behavior. The parameters are:

| Parameter                                                               | Default             | Meaning                                                                    |
|-------------------------------------------------------------------------|---------------------|----------------------------------------------------------------------------|
| `org.jetbrains.kotlinx.ki.shell.Shell.Settings.sayHello`                | `true`              | print the version and the help hint on start                               |
| `org.jetbrains.kotlinx.ki.shell.Shell.Settings.overrideSignals`         | `true`              | Ctrl-C interrupts the evaluation instead of the shell                      |
| `org.jetbrains.kotlinx.ki.shell.Shell.Settings.maxResultLength`         | `10000`             | maximum length of a printed result                                         |
| `org.jetbrains.kotlinx.ki.shell.Shell.Settings.blankLinesAllowed`       | `2`                 | blank lines that cancel an incomplete snippet                              |
| `org.jetbrains.kotlinx.ki.shell.plugins.PromptPlugin.Prompt.pattern`    | `[%l]`              | prompt pattern, see above                                                  |
| `org.jetbrains.kotlinx.ki.shell.plugins.PromptPlugin.Prompt.incomplete` | `...`               | prompt for the continuation lines of a snippet                             |
| `org.jetbrains.kotlinx.ki.shell.plugins.SyntaxPlugin.Syntax.on`         | `true`              | syntax highlighting                                                        |
| `history-file`                                                          | `~/.kshell_history` | history file                                                               |
| `plugins`                                                               | all plugins         | comma-separated plugin class names                                         |
| `<plugin class>.<command class>.name`, `.short`                         |                     | command name and its short form, e.g. `...RuntimePlugin.InferType.short=t` |

For example, `~/.ki-shell` could contain:
```properties
org.jetbrains.kotlinx.ki.shell.Shell.Settings.sayHello=false
org.jetbrains.kotlinx.ki.shell.plugins.PromptPlugin.Prompt.pattern=ki [%l]>
```

## Using the Shell from Code

The shell is published as `org.jetbrains.kotlinx:ki-shell`. To start the interactive shell from your application, call
`KotlinShell.main(args)`, or `KotlinShell.run(args)`, which returns the exit code instead of exiting the process.

Until Kotlin 2.5.0 is published on Maven Central, the shell depends on the Kotlin `2.5.0-Beta2-35` build, so add the
Kotlin dev repository `https://redirector.kotlinlang.org/maven/dev` to the repositories of your build.

To evaluate snippets without a terminal, create the shell yourself:
```kotlin
import org.jetbrains.kotlinx.ki.shell.KotlinShell
import org.jetbrains.kotlinx.ki.shell.wrappers.ResultWrapper
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics

fun main() {
    val shell = KotlinShell.createShell(KotlinShell.configuration(), classpath = listOf(File("libs/my.jar")))
    shell.initEngine(interactive = false)
    try {
        val result = shell.eval("listOf(1, 2).sum()")
        if (result.getStatus() == ResultWrapper.Status.SUCCESS) {
            shell.handleSuccess(result.result as ResultWithDiagnostics.Success<*>) // prints res0: Int = 3
        } else {
            shell.handleError(result.result, result.isCompiled)
        }
    } finally {
        shell.cleanUp()
    }
}
```
The snippets see the whole classpath of the application. `shell.runScript(File("script.kts"))` runs a script file like
`ki -f`.

## Adding maven repositories that require auth

The following options are supported:

```bash
:repository https://myrepo.org username=user password=pwd
:repository https://myrepo.org username:user password:pwd

:repository https:myrepo.org ./path/to/file
# properties file should contain
# username=john
# password=johnpwd
```
