# Guess Market Implementation Walkthrough

## Status

The seven-stage plan was approved on 2026-08-15. Stage 1 Task 1 established the verified test foundation, and every Java method remains `Planned` until its focused test passes.

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
| 1 | DTO value classes | Constructors and getters | Preserve typed values and immutable nested collections without domain behavior | `DtoContractTest` | Planned |
| 1 | `GuessMarketEngine` | Seven supported operations | Define the complete caller capability boundary | Compile and `javap` inspection | Planned |
| 1 | `EngineOperationException` | Constructors and context getters | Carry one checked structured failure without UI formatting | Engine and renderer tests | Planned |
| 2 | `LmsrCalculator` | `totalCost` | Evaluate stable LMSR cost through log-sum-exp | `LmsrCalculatorTest` | Planned |
| 2 | `LmsrCalculator` | `priceForOption` | Derive an observational price through stable softmax | `LmsrCalculatorTest` | Planned |
| 2 | `LmsrCalculator` | `purchaseCost` | Calculate the D-068 cancellation-safe direct delta | `LmsrCalculatorTest` | Planned |
| 2 | `LmsrCalculator` | `maximumSubsidy` | Derive `b * ln(2)` without treating it as account cash | `LmsrCalculatorTest` | Planned |
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
| 1 | JUnit foundation verified; DTOs and Engine contract pending | No Java tests in Task 1 | JAR module inspection and SHA-256 verification passed | Task 1 commit pending | In progress |
| 2 | LMSR and domain transitions | None before execution | None before execution | None before approval | Planned |
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
