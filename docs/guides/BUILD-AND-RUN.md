# Build and Run

## Status

The final build pipeline is completely designed but `build.bat` does not exist yet. Through Stage 5, the verified workflow is source-level only: strict Oracle Java 25 compilation of DTO, Engine, and UI production and test sources, followed by direct execution of all eleven required JUnit classes. The current known strict Java 25 gate reports 137 successful tests with zero failures, skips, disabled tests, or aborts. Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c`. Stage 5 is implemented and independently reviewed on `codex/e1-console-ui`, successfully rebased directly onto `9a8c87c`, and remains unmerged and unpushed while awaiting Nitzan's Stage 5 review and acceptance. The planned commands below are not a working packaged build, and packaging remains Stage 6 and Stage 7 work.

## Environment

- Oracle JDK 25
- Windows for the authoritative build and package verification
- `JAVA_HOME` pointing to one coherent JDK 25 installation
- No dependency download during the build

## Planned build authority

The repository root will contain one `build.bat`. It will use `%~dp0` to locate the project, quote paths, use local variables, fail fast, and clean only the fixed repository `build` directory.

The build phases are:

1. Verify the JDK environment.
2. Clean the fixed build directory.
3. Compile DTO production source.
4. Compile Engine production source against DTO and the five JAXB runtime JARs.
5. Compile UI production source against Engine and DTO.
6. Compile tests in module dependency order.
7. Run all required JUnit suites and verify the report contains no failure, skip, disabled test, or abort.
8. Create and inspect the three non-fat application JARs.
9. Stage the exact runtime package.
10. Create and inspect the ZIP.
11. Extract the ZIP to a fresh path and run packaged-process scenarios.

## Vendored JAXB dependencies

| File | Role |
| --- | --- |
| `angus-activation.jar` | Runtime activation implementation |
| `jakarta.activation-api.jar` | Runtime activation API |
| `jakarta.xml.bind-api.jar` | Runtime JAXB API |
| `jaxb-core.jar` | Runtime JAXB core |
| `jaxb-impl.jar` | Runtime JAXB implementation |
| `jaxb-jxc.jar` | Development schema tool, excluded from runtime package |
| `jaxb-xjc.jar` | Development XJC compiler, excluded from runtime package |
| `xjc-run.bat` | Course-aligned source-generation wrapper |
| `LICENSE.txt` | JAXB distribution license |

The approved JUnit Platform Console Standalone JAR and its license are present under `tools/testing` and are used by the current source-level test workflow. They remain excluded from runtime packaging.

## Planned application artifacts

- `guessmarket-dto.jar`
- `guessmarket-engine.jar`
- `guessmarket-ui.jar`

Only the UI JAR is executable. Its manifest will name `guessmarket.ui.console.ConsoleMain` and reference Engine, DTO, and the five JAXB runtime JARs through relative `Class-Path` entries.

## Planned ZIP layout

```text
run.bat
README.pdf
guessmarket-ui.jar
lib/
  guessmarket-engine.jar
  guessmarket-dto.jar
  angus-activation.jar
  jakarta.activation-api.jar
  jakarta.xml.bind-api.jar
  jaxb-core.jar
  jaxb-impl.jar
```

The launcher is planned as:

```bat
@java -jar "%~dp0guessmarket-ui.jar"
```

The final ZIP must be verified after fresh extraction under a path containing spaces and on a clean Windows 10 or demonstrably compatible clean Windows environment.
