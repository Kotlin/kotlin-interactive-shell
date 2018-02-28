# Changelog
All notable changes to this project will be documented in this file.

## [0.1.2] - 2018-02-28
### Added
- Paste command.
- Additional classes and interfaces to abstract `GenericRepl` results.
- More unit tests for `KShell`.
### Changed
- Added 2 methods to `ConsoleReader` to support history changes.

## Fixed
- Problem with multiline code [#9](https://github.com/khud/sparklin/issues/9)
- Doubled colon problem [#10](https://github.com/khud/sparklin/issues/10)

## [0.1.1] - 2018-02-08
### Added
- Minimal unit test for `KShell`.
### Fixed 
- Bad platform-types support [#3](https://github.com/khud/sparklin/issues/3) both for `resXXX` and `:type` command.

## [0.1] - 2018-02-02
### Added
- Plugin management system.
- Type inference command `:type`.
- Print symbols by `:symbols`.
- Spark 1.x and 2.x support via separate plugins.
### Changed
- Abstract `ConsoleReader` and `Completer`, jline2 support has moved to separate module.
- `Command` is a class anymore.
### Removed
- Old spark stuff: separate main class and so on.

[0.1.2]: https://github.com/khud/sparklin/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/khud/sparklin/compare/0.1...0.1.1
