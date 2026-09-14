# Guess Market - Exercise 2

A Java 25 prediction-market application with a JavaFX interface, user accounts, LMSR trading and Order Book trading. Implemented bonuses: **None**.

## Run the submitted application

1. Extract the complete submitted ZIP into a writable folder. Keep `run.bat`, `lib` and `runtime` together.
2. Double-click `run.bat`. The Windows x64 package includes Java 25.0.4 and JavaFX 25.0.4; running it requires no Java installation or environment-variable setup.
3. Select **Load File** and choose an Exercise 2 XML file. Original course files are supplied separately from the ZIP.
4. Use **Events** to inspect markets. Under **Users**, choose the acting user and open Available events, Owned events or Participations to trade or manage an event.
5. Review a purchase/order before confirming. An event owner can open an event, select its winning outcome and review settlement payouts.

An invalid XML load preserves the previous world. Successfully reloading XML starts a fresh simulation. The Exercise 2 interface does not save manual trades between launches.

## Build, test and run from source

Source builds require Windows x64, **Oracle JDK 25.0.4** and the **JavaFX 25.0.4 Windows x64 SDK**. Use a short checkout path, such as `C:\Projects\GuessMarket`: Windows `xcopy` can fail on deeply nested fixture paths. These development tools are separate from the runtime inside the submitted ZIP.

From the repository root in PowerShell, replace the two installation paths and run:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25.0.4'
$env:JAVAFX_HOME = 'C:\path\to\javafx-sdk-25.0.4'
.\build-javafx.bat
```

The build compiles DTO, Engine and JavaFX sources with UTF-8, `--release 25`, `-Xlint:all` and `-Werror`. It packages FXML, CSS and schemas, creates three application JARs and runs all **236 tests**. Dependencies are vendored; the build downloads nothing.

Generated output is under `build/javafx`:
- `dev/lib`: application JARs and JAXB runtime libraries.
- `reports`: JUnit XML and `junit-output.txt`.
- `native`: screenshots from test-owned JavaFX stages.

After a successful build, run the development application with the same environment settings:

```powershell
.\build\javafx\dev\run-javafx.bat
```

## Architecture and source layout

```text
JavaFX UI -> Engine -> DTO
       \------------> DTO
```

| Module | Responsibility |
| --- | --- |
| `modules/guessmarket-javafx-ui` | FXML/CSS, controls, background commands and display. Entry point: `guessmarket.ui.javafx.GuessMarketApplication`. |
| `modules/guessmarket-engine` | XML/schema and business validation, accounts, LMSR, matching, holdings and settlement. Independent of JavaFX. |
| `modules/guessmarket-dto` | Immutable requests, snapshots, previews and receipts shared by UI and Engine. |
| `modules/guessmarket-ui` | Preserved Exercise 1 console interface. Exercise 2 uses the JavaFX module above. |

Each module has its own source/test directory. The Exercise 2 submission contains the DTO, Engine and JavaFX JARs. `build.bat` and the console packaging inputs belong to the earlier Exercise 1 workflow; use `build-javafx.bat` for this exercise.

## Trading and accounting choices

- Ordinary matching runs first, at the resting order price. Best price takes priority; equal prices use FIFO. Partial remainders retain their priority.
- When automatic mint is enabled, opposite BUY orders can create pairs if their limits sum to at least `d`, including equality. Best opposite price takes priority, then FIFO. Self-trading and self-minting reject the command atomically.
- SELL orders reserve owned shares; BUY orders do not reserve cash. Users have no manual order-cancel action in this assignment flow.
- An overdraft purchase blocks buying/opening. A later positive receipt restores eligibility when resulting cash is strictly positive. Blocked owners can still settle their own events; cancelled orders do not return after recovery.
- Order Book arithmetic uses exact `BigDecimal` values. Entered limits retain their precision; calculated displays round half up to at most two decimal places. Authoritative calculations never use rounded display values.

For main class roles, funding, valuation, settlement and the complete implementation choices, see the [application manual](packaging/README-exercise2.md).

## Submission packaging and dependencies

`packaging/package-exercise2.ps1` assembles the portable ZIP after the complete build gate. It requires explicit `RuntimeDirectory`, `JavaFxLegalDirectory`, `ManualPdf` and `ArchiveName` arguments: a linked Windows x64 Java 25.0.4/JavaFX 25.0.4 runtime, the SDK legal folder, a verified PDF manual and a ZIP filename. The runtime and submission-specific manual are supplied separately from this source repository.

`tools` contains JAXB RI 4.0.5, its activation dependencies, code-generation tools and JUnit Platform Console 6.1.1. Their licenses/notices are retained alongside them. The submitted package includes five JAXB runtime JARs and runtime legal notices, with root `JAXB-LICENSE.txt` and `THIRD-PARTY-NOTICES.txt`.

No open-source license is granted for application code. Third-party components retain their own licenses.
