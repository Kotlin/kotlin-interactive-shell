# Changelog
All notable changes to this project will be documented in this file.

## [0.2.3]
### Added
- `:conf` and `:set` commands to deal with configuration without restart.
- `sayHello()` method to receive greetings from plugins.

### Changed
- Jline3 is now bundled to `kshell.jar`.
- Small API changes to improve configuration support.

### Fixed
- Kotlin NPE if result is null [#24](https://github.com/khud/sparklin/issues/24)
- The shell should not truncate output after 2 lines already [#23](https://github.com/khud/sparklin/issues/23)
- You typed two blank lines. Starting a new command. [#22](https://github.com/khud/sparklin/issues/22)
- Kotlin v.1.2.41/v.1.2.50 compatibility [#21](https://github.com/khud/sparklin/issues/21)

## [0.2.1]
### Added
- Trap Ctrl-C [#18](https://github.com/khud/sparklin/issues/18)

### Fixed
- Custom dependencies for KShell [#17](https://github.com/khud/sparklin/issues/17)

## [0.2]
### Added
- Customized prompt.
- Syntax highlight.
- JShell-like Kotlin snippets.
- Completely new REPL architecture with object based class layout `kshell-repl-api`.
- HDFS browser plugin.
- Kotlin interpreter for Zeppelin.

### Changed
- Switch to JLine3
- Kotlin version 1.2.20 -> 1.2.30.
- Unit testes moved to `kshell-repl-api`.

### Fixed
- Custom REPL fixes [#7](https://github.com/khud/sparklin/issues/7).
- Problem with custom commands which start with the same letter.

### Removed
- Unnecessary `Shared` class from `KShell`. 
- Additional classes and interfaces to abstract `GenericRepl` results.
- All `GenericRepl` dependencies.

## [0.1.2] - 2018-02-28
### Added
- Paste command.
- Additional classes and interfaces to abstract `GenericRepl` results.
- More unit tests for `KShell`.
### Changed
- Added 2 methods to `ConsoleReader` to support history changes.

### Fixed
- Problem with multiline code [#9](https://github.com/khud/sparklin/issues/9).
- Doubled colon problem [#10](https://github.com/khud/sparklin/issues/10).

## [0.1.1] - 2018-02-08
### Added
- Minimal unit test for `KShell`.
### Fixed 
- Bad platform-types support [#3](https://github.com/khud/sparklin/issues/3) for both `resXXX` and `:type` command.

## 0.1 - 2018-02-02
### Added
- Plugin management system.
- Type inference command `:type`.
- Print symbols by `:symbols`.
- Spark 1.x and 2.x support by separate plugins.
### Changed
- Abstract `ConsoleReader` and `Completer`, jline2 support has moved to separate module.
- `Command` is a class anymore.
### Removed
- Old spark stuff: separate main class and so on.

[0.2.3]: https://github.com/khud/sparklin/compare/v0.2.1...v0.2.3
[0.2.1]: https://github.com/khud/sparklin/compare/v0.2...v0.2.1
[0.2]: https://github.com/khud/sparklin/compare/0.1.2...v0.2
[0.1.2]: https://github.com/khud/sparklin/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/khud/sparklin/compare/0.1...0.1.1
