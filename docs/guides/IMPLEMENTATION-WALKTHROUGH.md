# Guess Market Implementation Walkthrough

## Status

The seven-stage plan was approved on 2026-08-15. Stage 1 implemented and verified the test foundation, immutable DTO boundary, and complete supported Engine contract. Stage 2 is in progress: Task 4 implements and verifies the cancellation-safe LMSR calculator, while domain state and transitions remain planned for Task 5.

## Purpose

This file is the learning-oriented companion to `docs/planning/IMPLEMENTATION-PLAN.md`. The plan says what to build and in what order. This walkthrough records what was actually built, why each meaningful method exists, what state it reads or changes, how failure behaves, and which test proves it.

An entry changes from `Planned` to `Implemented` only after the focused test passes. An entire stage changes to `Verified` only after its regression gate passes. If implementation differs from the planned signature for a non-material reason, record the actual signature and reason. If it would change an approved design contract, stop and obtain Nitzan's approval before changing the code or this guide.

## How to update this guide

For every implementation task:

1. Add or update the affected method rows.
2. Explain one important Java or design concept introduced by the task.
3. Record the focused test class and test method names.
4. Record the regression command and result.
5. Record the commit after the stage is accepted.
6. Keep verified behavior, inference, and open questions separate.

## Architecture in one view

```text
ConsoleMain
  -> GuessMarketConsoleApp
      -> GuessMarketEngine interface
          -> GuessMarketEngineImpl
              -> MarketEvent and owned domain state
              -> LmsrCalculator
              -> XmlMarketLoader -> JaxbMarketMapper -> generated JAXB types
              -> SavedStateStore -> SavedStateValidator

Engine and UI
  -> immutable guessmarket.dto values
```

The UI translates temporary displayed positions into real event IDs. Engine receives and validates only the real IDs. XML and saved-state candidates remain quarantined until complete validation, then replace the Engine map through one reference assignment.

## Stage 1 test foundation

The repository vendors JUnit Platform Console Standalone 6.1.1 only as a development tool. A standalone runner fits this repository because the approved build uses direct Java 25 compiler and launcher commands instead of Maven or Gradle. The runner never becomes part of a production module or runtime artifact.

The exact JAR is `2,996,922` bytes with SHA-256 `7B16416E5727C645105C31B533440397F20DF68F6AE850C6BFD0CE1D88DB66C3`. Oracle JDK 25.0.4 reports automatic module `org.junit.platform.console.standalone@6.1.1`. Matching license and notice material was copied from the JAR itself into `tools/testing`.

## Stage 1 immutable DTO boundary

DTO means data transfer object. These classes are immutable snapshots that cross module boundaries. They are not the live domain objects that will later own quantities, account state, history, or lifecycle transitions.

Each constructor validates that its snapshot is internally meaningful. `List.copyOf` performs a defensive copy and returns an unmodifiable list, so changing the caller's original list cannot change the DTO. Empty XML-provided text and duplicate labels remain valid, while null nested values, invalid option numbering, invalid quantities, non-finite financial values, and inconsistent winner state are rejected.

Receipt and history totals compare `totalPaid` with `baseShareCost + purchaseCommission` using an eight-ULP binary64 tolerance. ULP means one representable floating-point step at the current scale. This accepts harmless representation differences such as `0.1 + 0.2` compared with `0.3`, including at tiny LMSR scales, while still rejecting materially inconsistent totals.

`MarketEventDetails` uses `OptionalInt` for the winner because an open event has no winner and option zero is never valid. `PurchaseReceipt` retains the exact `MarketEventDetails` instance created by the same successful operation, so the UI will not need a second Engine query that could observe a later state.

## Stage 1 supported Engine contract

`GuessMarketEngine` defines the seven capabilities available to every caller. The future console will store this interface type instead of the concrete implementation type. That makes the capability boundary clear and prevents UI code from depending on domain, XML, persistence, or calculation helpers.

Every expected operation failure crosses the boundary as one checked `EngineOperationException`. `EngineErrorCode` identifies the category, while typed optional getters expose available path, event, option, quantity, status, field, and XML location context. The low-level cause remains available for developer diagnosis.

The UI must not parse `getMessage()`. It will switch on `getCode()` and read typed context, because message wording is presentation detail while the enum and getters are the stable recovery contract. The path context is transient because this exception is not part of the approved saved-state graph; the public getter remains unchanged during normal use.

## Stage 2 cancellation-safe LMSR mathematics

`LmsrCalculator` is a final, stateless, package-private Engine helper. Its four package-private static methods accept only primitive quantities and `b`, return unrounded `double` values, and do not import or mutate DTO, domain, XML, persistence, commission, or console state.

`totalCost` uses log-sum-exp. It divides both quantities by `b`, subtracts their maximum before exponentiation, and then restores that maximum after taking the logarithm. The largest adjusted exponential is therefore `exp(0)`, so large valid quantities do not overflow merely because the direct formula would evaluate `exp(large value)`.

`priceForOption` uses the same subtract-the-maximum softmax idea as a stable two-option logistic calculation. It forms the quantity difference as `long` before converting to `double`. The favored side uses `1 / (1 + exp(-difference / b))`; the disfavored side uses the algebraically equal branch `exp(difference / b) / (1 + exp(difference / b))`. At extreme imbalance an observational price may legitimately become exactly `0.0` or `1.0` in binary64.

`purchaseCost` is deliberately separate from both `totalCost` and display price. Subtracting two independently rounded total costs can cancel a real small delta to zero, while multiplying an already underflowed price by an overflowing exponential can produce an invalid result. The direct delta instead calculates `logP`, `logExpm1`, and their sum `t` in the log domain. `softplus` uses `max(x, 0) + log1p(exp(-abs(x)))`, and the large-`h` branch rewrites `log(expm1(h))` as `h + log1p(-exp(-h))` before `expm1` can overflow.

For `t < -37.0`, the calculator evaluates `exp(log(b) + t)`. Keeping the multiplication by `b` inside the logarithm preserves representable tiny costs such as `6.392138950083687E-44`, even when `softplus(t)` alone would be too small. If a mathematically positive purchase still rounds to zero, the method rejects it instead of granting free shares. Nonnegative current quantities, positive purchases, positive `b`, and finite outputs are checked before a future domain operation can mutate state.

`maximumSubsidy` derives `b * ln(2)`. It is a maximum-exposure measure, not cash placed into the market-maker account. The calculator tests cover the approved worked example, supplied-simulator oracle cases, ordinary invariants, larger `b`, extreme quantities, both underflow outcomes, and invalid inputs with documented absolute or relative tolerances.

## Planned supported Engine methods

| Method | Responsibility | Success result | Expected failure boundary | Status |
| --- | --- | --- | --- | --- |
| `loadEventsFromXml(Path)` | Build a complete trusted XML candidate and replace live state once | Loaded event count | Structured XML or configuration code, prior state preserved | Planned |
| `listEvents()` | Return compact immutable summaries in load order | Immutable summary list | `NO_SYSTEM_LOADED` | Planned |
| `getEventDetails(int)` | Return one full immutable event snapshot by actual ID | `MarketEventDetails` | `NO_SYSTEM_LOADED` or `EVENT_NOT_FOUND` | Planned |
| `purchaseShares(int, int, int)` | Complete one atomic LMSR purchase | `PurchaseReceipt` with same-operation details | Event, option, quantity, lifecycle, or numerical code, state preserved | Planned |
| `closeEvent(int, int)` | Complete one atomic winner and payout transition | Final `MarketEventDetails` | Event, option, or lifecycle code, state preserved | Planned |
| `saveState(Path)` | Publish one complete versioned `.ser` file | Normal return | State-path or access code, running state unchanged | Planned |
| `restoreState(Path)` | Validate a direct-domain saved graph and replace live state once | Restored event count | State-path, access, or invalid-state code, prior live state preserved | Planned |

## Method ledger

Add constructors, public methods, and meaningful package-private helpers as code is implemented. Trivial getters for one immutable class may share one row when they have the same responsibility.

| Stage | File or type | Method or method group | Responsibility and state effect | Primary proof | Status |
| --- | --- | --- | --- | --- | --- |
| 1 | `CommissionMode` and `EventStatus` | Enum values | Represent the two commission timings and two lifecycle states without display strings | `DtoContractTest` | Implemented |
| 1 | `MarketEventSummary` | Constructor and getters | Preserve a compact event projection, full-range ID, empty text, and exactly two ordered immutable labels | `DtoContractTest.summaryPreservesFullRangeEventIdAndEmptyText` and `summaryDefensivelyCopiesTwoOrderedLabels` | Implemented |
| 1 | `MarketOptionSnapshot` | Constructor and getters | Preserve one numbered option, quantity, and full-precision finite probability | `DtoContractTest.optionSnapshotRejectsInvalidOptionNumberQuantityAndPrice` | Implemented |
| 1 | `TradeHistoryEntry` | Constructor and getters | Preserve one successful purchase's option, quantity, and exact financial breakdown | `DtoContractTest.historyEntryPreservesFinancialBreakdown` | Implemented |
| 1 | `MarketEventDetails` | Constructor and getters | Preserve a self-contained event snapshot with ordered options, newest-first history, account values, and explicit winner absence | `DtoContractTest.detailsDefensivelyCopiesOptionsAndNewestFirstHistory` and `detailsRequiresWinnerOnlyForClosedEvent` | Implemented |
| 1 | `PurchaseReceipt` | Constructor and getters | Preserve the successful purchase breakdown and same-operation resulting details | `DtoContractTest.purchaseReceiptKeepsSameOperationDetails` | Implemented |
| 1 | DTO collection getters | `getOptionLabels`, `getOptions`, and `getPurchaseHistory` | Return stored immutable copies without exposing caller-owned lists | `DtoContractTest.allReturnedCollectionsRejectMutation` | Implemented |
| 1 | `GuessMarketEngine` | Seven supported operations | Define the complete caller capability boundary using only JDK paths, DTO results, and one checked failure type | `GuessMarketEngineUseCaseTest.engineInterfaceDeclaresSevenCheckedOperations` and `javap -public` | Implemented |
| 1 | `EngineErrorCode` | Sixteen enum values | Identify XML, market, calculation, and saved-state failure categories without embedding UI text | `GuessMarketEngineUseCaseTest.errorCodeContainsExactlyApprovedValues` and `javap -public` | Implemented |
| 1 | `EngineOperationException` | Simple and complete constructors | Preserve required failure data, optional context, and a diagnostic cause in one checked type | `GuessMarketEngineUseCaseTest.simpleExceptionPreservesRequiredFields` and `completeExceptionPreservesOptionalContextAndCause` | Implemented |
| 1 | `EngineOperationException` | Structured getters | Return typed `Optional`, `OptionalInt`, enum, detail, and recovery values so callers never parse a message | `GuessMarketEngineUseCaseTest` and `javap -public` | Implemented |
| 2 | `LmsrCalculator` | `totalCost` | Evaluate stable LMSR cost through log-sum-exp without state effects | `LmsrCalculatorTest.maximumSubsidyEqualsInitialTotalCost`, `approvedWorkedExampleMatchesAtFullPrecision`, and `veryLargeQuantitiesRemainFinite` | Implemented |
| 2 | `LmsrCalculator` | `priceForOption` | Derive an observational price through stable two-branch softmax, allowing honest binary64 endpoint rounding | `LmsrCalculatorTest.initialPricesAreOneHalf`, `complementaryPricesSumToOneForUnequalQuantities`, and boundary tests | Implemented |
| 2 | `LmsrCalculator` | `purchaseCost` | Calculate the D-068 cancellation-safe direct delta, preserve representable tiny values, and reject a required-positive zero | `LmsrCalculatorTest.approvedWorkedExampleMatchesAtFullPrecision`, `suppliedSimulatorOracleCasesMatch`, and direct-delta boundary tests | Implemented |
| 2 | `LmsrCalculator` | `maximumSubsidy` | Derive `b * ln(2)` without treating it as account cash | `LmsrCalculatorTest.maximumSubsidyEqualsInitialTotalCost` | Implemented |
| 2 | `MarketEvent` | Constructor | Establish one open two-option event with zero quantities, account, commission, and history | `MarketEventTest` and mapper tests | Planned |
| 2 | `MarketEvent` | `purchase` | Validate and commit quantity, cost, account, commission, and history atomically | `MarketEventTest` | Planned |
| 2 | `MarketEvent` | `close` | Validate and commit winner, payout, final account, commission, and lifecycle atomically | `MarketEventTest` | Planned |
| 3 | `JaxbMarketMapper` | `map` | Convert a complete generated graph into an ordered domain candidate | `JaxbMarketMapperTest` | Planned |
| 3 | `XmlMarketLoader` | `load` | Validate path, trusted schema, JAXB structure, and mapping without touching live state | `XmlMarketLoaderTest` | Planned |
| 4 | `SavedStateStore` | `resolveStatePath` | Apply the case-insensitive `.ser` rule and path validation | `GuessMarketEnginePersistenceTest` | Planned |
| 4 | `SavedStateStore` | `save` | Serialize to a completed temporary sibling and publish with honest move semantics | `GuessMarketEnginePersistenceTest` | Planned |
| 4 | `SavedStateStore` | `restore` | Deserialize one filtered root into quarantine | `GuessMarketEnginePersistenceTest` | Planned |
| 4 | `SavedStateValidator` | `validate` | Prove graph identity, values, history, accounting, and lifecycle before a fresh map is returned | `GuessMarketEnginePersistenceTest` | Planned |
| 4 | `GuessMarketEngineImpl` | Seven interface overrides | Coordinate live state, domain operations, XML, persistence, and DTO conversion | Three public Engine test classes | Planned |
| 5 | `ConsoleInput` | Menu, selection, option, quantity, and path readers | Read full lines, repeat prompt-local shape errors, and signal end-of-input | `ConsoleInputTest` | Planned |
| 5 | `ConsoleRenderer` | `formatFinancial` | Format finite values with two US decimal places and normalize signed zero | `ConsoleRendererTest` | Planned |
| 5 | `ConsoleRenderer` | Menu, DTO, receipt, history, and error renderers | Produce deterministic English output without domain logic | `ConsoleRendererTest` | Planned |
| 5 | `GuessMarketConsoleApp` | `run` and command handlers | Own the stable loop, command conversations, filtering, actual-ID translation, and recovery scope | `GuessMarketConsoleAppTest` | Planned |
| 5 | `ConsoleMain` | `main` | Compose production dependencies once and start the app through the interface | Packaged-process checks | Planned |

## Stage verification log

| Stage | Verified behavior | Focused tests | Regression evidence | Accepted commit | State |
| --- | --- | --- | --- | --- | --- |
| 1 | JUnit foundation, immutable DTO boundary, and complete supported Engine contract implemented | `DtoContractTest`: 8 successful; `GuessMarketEngineUseCaseTest`: 4 successful | Clean Java 25 compile with `-Xlint:all`; 12 tests successful with zero failures, skips, disabled tests, or aborts; JDK-only DTO dependency and public `javap` inspections passed | `219738d`, `2d4e249`, `d628b14`, review fix `0912d68` | Ready for review |
| 2 | Cancellation-safe LMSR implemented; domain transitions remain for Task 5 | `LmsrCalculatorTest`: 14 successful | Clean Java 25 compile with `-Xlint:all -Werror`; 26 total tests successful with zero failures, skips, disabled tests, or aborts; package-private API and JDK-only dependency inspections passed | None before stage approval | In progress |
| 3 | JAXB, mapping, and XML loading | None before execution | None before execution | None before approval | Planned |
| 4 | Persistence and complete Engine | None before execution | None before execution | None before approval | Planned |
| 5 | Console UI | None before execution | None before execution | None before approval | Planned |
| 6 | Authoritative build and exact ZIP | None before execution | None before execution | None before approval | Planned |
| 7 | Exact-artifact acceptance | None before execution | None before execution | None before approval | Planned |

## Concepts to explain as they appear

- Immutable DTOs and defensive copying.
- Interface reference versus concrete construction.
- Checked exceptions with typed context.
- Encapsulation, aggregate ownership, and meaningful transitions.
- Floating-point cancellation, overflow, underflow, `log1p`, and `expm1`.
- Atomic validate-calculate-commit flow.
- JAXB-generated integration types versus application domain objects.
- Trusted schema loading and layered validation.
- Java serialization, `serialVersionUID`, format version, object filters, and quarantine.
- Dependency injection for deterministic console and file-behavior tests.
- Manifest-relative runtime classpaths and non-fat JARs.
- Exact-artifact verification versus source-level test success.

## Final walkthrough shape

When implementation is complete, this guide must let Nitzan explain the application in this order:

1. How `ConsoleMain` composes the application.
2. How the UI reads one command and calls only the Engine interface.
3. How immutable DTOs cross module boundaries.
4. How XML becomes a fully validated candidate and then live state.
5. How a purchase uses the direct LMSR delta and commits all related state once.
6. How close computes commission and the final market-maker result without double-crediting.
7. How saved state is filtered, validated, and installed without replay.
8. How the build proves tests, JAR ownership, manifest classpath, ZIP membership, and real packaged execution.
9. Which evidence remains manual and why clean Windows 10 cannot be replaced silently by CI or IntelliJ.
