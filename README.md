# Guess Market

Guess Market is a Java 25 console-based prediction-market simulator. It loads market definitions from XML, calculates prices with the LMSR model, supports purchasing shares and closing events, and can save and restore the market state.

## Features

- XML loading and validation with JAXB
- Display of all events and detailed trading status
- LMSR-based share pricing and purchase receipts
- Purchase or closing commission policies
- Event closing with the final market-maker result
- Save and restore of the complete engine state
- Console input recovery for invalid commands and recoverable errors

## Architecture

The project uses three modules with one-directional dependencies:

```text
guessmarket-ui -> guessmarket-engine -> guessmarket-dto
       |                  |
       +----------------->+
```

- `guessmarket-dto` contains immutable values shared between modules.
- `guessmarket-engine` contains market behavior, XML loading, LMSR calculations, persistence, and the supported engine interface.
- `guessmarket-ui` contains the console menu, input, output, and application startup.

Each module is packaged as a separate JAR. The UI JAR is the executable application JAR.

## Requirements

- Windows
- Oracle JDK 25

All required JAXB runtime dependencies are included in the repository. The build does not download dependencies.

## Build and run

From the repository root in Windows PowerShell, set `JAVA_HOME` to an Oracle JDK 25 installation and run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
.\build.bat
```

The build compiles the three modules, runs the automated tests, creates the JAR files, and creates a submission archive under `build\distributions`.

To run the packaged application, extract the archive and run `run.bat` from the extracted folder.

## Project layout

- `modules/guessmarket-dto` - shared immutable data-transfer objects
- `modules/guessmarket-engine` - market engine, XML support, and persistence
- `modules/guessmarket-ui` - console application
- `packaging` - manifest and runtime launcher inputs
- `tools` - required JAXB and test dependencies
- `build.bat` - reproducible Windows build and package entry point

## Technical documentation

- [Build and run](docs/guides/BUILD-AND-RUN.md)
- [Testing](docs/guides/TESTING.md)
- [JAXB and XML](docs/guides/JAXB-AND-XML.md)

## License

No open-source license is granted for the project code. Vendored third-party components retain their own licenses.
