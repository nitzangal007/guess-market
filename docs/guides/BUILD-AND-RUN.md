# Build and Run

## Overview

`build.bat` is the repository-owned Windows build for Guess Market. It requires Oracle JDK 25 through `JAVA_HOME`, performs a clean build, runs the automated test suite, creates the three application JARs, verifies their packaging metadata, assembles a runnable distribution, and checks that the packaged application starts correctly. It does not download dependencies.

The source repository and the packaged application have different run flows:

- From a Git clone, run `build.bat` first to compile and package the application.
- From an already packaged distribution, no build step is required. Extract it and run `run.bat`.

The `packaging/run.bat` file in the repository is a packaging input. It expects `guessmarket-ui.jar` to be beside it, so it is not intended to be launched directly from the source tree.

## Environment

- Oracle JDK 25
- Windows
- `JAVA_HOME` pointing to one coherent JDK 25 installation
- No dependency download during the build

## Build from source

From the repository root in `cmd.exe`:

```bat
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.4"
.\build.bat
```

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
.\build.bat
```

Use the actual Oracle JDK 25 location on the machine. The script verifies Java 25 and uses the configured JDK consistently for compilation and packaging.

The build performs these main steps:

1. Clean the project build directory.
2. Compile the DTO, Engine, and UI modules in dependency order.
3. Compile and run the automated tests.
4. Create and inspect the three application JARs.
5. Assemble the runtime files into a runnable distribution.
6. Extract a fresh copy and run packaged launcher checks.

## Generated application artifacts

- `guessmarket-dto.jar`
- `guessmarket-engine.jar`
- `guessmarket-ui.jar`

Only the UI JAR is executable. Its manifest names `guessmarket.ui.console.ConsoleMain` and references Engine, DTO, and the required JAXB runtime JARs through relative `Class-Path` entries.

## Packaged runtime layout

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

The launcher is:

```bat
@java -jar "%~dp0guessmarket-ui.jar"
```

Because the launcher resolves the UI JAR relative to its own location, the packaged distribution can be extracted to a normal Windows path, including a path containing spaces, and started directly with `run.bat`.

## Vendored dependencies

The repository includes the JAXB runtime components required by the application and the JUnit Platform Console Standalone dependency used by the source-level test workflow. Development-only JAXB tooling and JUnit are not part of the runtime distribution.
