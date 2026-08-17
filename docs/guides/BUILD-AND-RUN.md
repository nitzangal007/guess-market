# Build and Run

## Status

`build.bat` is the repository-owned authoritative Exercise 1 build. It requires one Oracle JDK 25 through `JAVA_HOME`, performs a clean build, proves all eleven required JUnit classes, creates the three application JARs, verifies ownership and manifests, creates the exact ZIP, extracts it under a path containing spaces, and runs packaged console scenarios. It does not download dependencies.

## Environment

- Oracle JDK 25
- Windows for the authoritative build and package verification
- `JAVA_HOME` pointing to one coherent JDK 25 installation
- No dependency download during the build

## Authoritative build command

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

Use the actual Oracle JDK 25 location on the machine. The script uses `%~dp0` to locate the project, quotes paths, invokes `java.exe`, `javac.exe`, and `jar.exe` only beneath that `JAVA_HOME`, checks major version 25, uses `setlocal`, and refuses to clean any target except this project’s fixed `build` directory.

The build phases are:

1. Preflight and clean the fixed build directory.
2. Create source lists, compile production modules in dependency order, and copy Engine resources.
3. Compile tests in module dependency order and run JUnit 6.1.1 with captured console output and XML reports.
4. Require every exact test class to execute at least one test with zero failures, container failures, skips, disabled tests, and aborts.
5. Create and inspect DTO, Engine, and UI application JARs.
6. Stage exactly the runtime files, create the ZIP with `jar --no-manifest`, and prove exact ZIP membership.
7. Extract to `build/verification/Fresh Extraction With Spaces` and run deterministic redirected-input launcher checks.

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

## Generated application artifacts

- `guessmarket-dto.jar`
- `guessmarket-engine.jar`
- `guessmarket-ui.jar`

Only the UI JAR is executable. Its manifest names `guessmarket.ui.console.ConsoleMain` and references Engine, DTO, and the five JAXB runtime JARs through relative `Class-Path` entries.

## Exact ZIP layout

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

Important generated locations are:

- `build/reports/junit/` - JUnit XML reports and `junit-proof.txt`.
- `build/reports/junit-console.txt` - captured JUnit console output.
- `build/inspection/` - JAR and ZIP inventories plus extracted UI manifest.
- `build/reports/packaged-*.txt` - launcher transcripts for inside and outside launch, integration, restore, and invalid XML recovery.
- `build/distributions/guess-market-exercise-1.zip` - the candidate ZIP.

`build/staging/` is only the controlled input to ZIP creation. It is not the final verification target. Process checks run from the fresh extraction of the ZIP so they prove the packaged runtime graph, manifest-relative classpath, launcher, and exact ZIP contents together.

Stage 7 must still verify the exact candidate ZIP on a clean Windows 10 machine and complete the README-led grader walkthrough.
