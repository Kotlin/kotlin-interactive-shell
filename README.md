# Kotlin Language Interactive Shell

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

## Installation

The latest version can be downloaded and installed by the following commands:

```bash
wget https://github.com/Kotlin/kotlin-interactive-shell/releases/latest/download/ki-shell-0.3-SNAPSHOT-bin.tar.gz
tar xvzf ki-shell-0.3-SNAPSHOT-bin.tar.gz
```

It can also be obtained directly from
GitHub [releases](https://github.com/Kotlin/kotlin-interactive-shell/releases/latest) page. 

To start the shell, run `bin/ki.sh` on Linux and macOS. On Windows, use `bin\ki.bat` instead.

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
