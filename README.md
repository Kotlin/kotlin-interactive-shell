# Kotlin Language Interactive Shell [![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/ki-shell.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.kotlinx%22%20AND%20a:%22ki-shell%22)

The shell is an extensible implementation of Kotlin REPL with reach set of features including:

- Syntax highlight
- Type inference command
- Downloading dependencies in runtime using Maven coordinates
- List declared symbols


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

### Arch Linux

On Arch Linux, there's an [AUR package](https://aur.archlinux.org/packages/ki-shell-bin/) available.
After installation, you can run the shell through `ki`.

### Homebrew

Install with [Homebrew](https://brew.sh/) with the following command:
```bash
brew install ki
```

## Build From Source

To build from source use:
```bash
git clone https://github.com/Kotlin/kotlin-interactive-shell
cd kotlin-interactive-shell
mvn -DskipTests package

```
It may be useful to build the project with a specific version of Kotlin. To do so use:
```bash
mvn -Dkotlin.version=1.4.10 -DskipTests package
```
To start the shell, run `bin/ki.sh` on Linux and macOS. On Windows, use `bin\ki.bat` instead.
