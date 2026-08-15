# Build and Run

## Status

The build pipeline is completely designed but not implemented yet. Commands in this document describe the approved target and must not be presented as working until `build.bat` exists and passes its verification gates.

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

The later testing foundation adds the approved JUnit Platform Console Standalone JAR and its license under `tools/testing`.

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
