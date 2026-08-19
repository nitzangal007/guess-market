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

## Build and run from source

From the repository root in Windows PowerShell, set `JAVA_HOME` to an Oracle JDK 25 installation and run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
.\build.bat
```

The build compiles the three modules, runs the automated tests, creates the application JARs, and assembles a runnable distribution.

The `packaging/run.bat` file in the repository is a packaging input. It becomes directly runnable only after it is placed next to the generated UI JAR in the assembled distribution.

For an already packaged distribution, no build step is required: extract the package and run `run.bat`.

## Project layout

- `modules/guessmarket-dto` - shared immutable data-transfer objects
- `modules/guessmarket-engine` - market engine, XML support, and persistence
- `modules/guessmarket-ui` - console application
- `packaging` - manifest and runtime launcher inputs
- `tools` - required JAXB and test dependencies
- `build.bat` - reproducible Windows source build and verification entry point

## Technical documentation

- [Architecture](docs/design/ARCHITECTURE.md)
- [Build and run](docs/guides/BUILD-AND-RUN.md)
- [Testing](docs/guides/TESTING.md)

## License

No open-source license is granted for the project code. Vendored third-party components retain their own licenses.
