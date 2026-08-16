# Guess Market Implementation Walkthrough

## Status

The seven-stage plan was approved on 2026-08-15. Stage 1 implemented and verified the test foundation, immutable DTO boundary, and complete supported Engine contract. Stage 2 Tasks 4 and 5 implement and verify the cancellation-safe LMSR calculator and complete event domain. The residual numerical correction is complete after re-review, and Nitzan reviewed and accepted Stage 2 on 2026-08-16. Pull request 2 is approved for merge before Stage 3 begins.

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

The whole-branch review and residual re-review exposed two cancellation boundaries when `logP` and the leading `h` term have opposite signs. For every negative quantity difference the calculator uses the exact identity `t = (difference + purchaseQuantity) / b - softplus(z) + log(1 - exp(-h))`. It forms `difference + purchaseQuantity` in `long` before converting once to `double`, preserving low-order numerator information that separately rounded quotients can erase. The correction `log(1 - exp(-h))` uses `log(-expm1(-h))` for `h <= ln(2)`, where subtracting `exp(-h)` from one is vulnerable to cancellation, and `log1p(-exp(-h))` for `h > ln(2)`, where `exp(-h) <= 0.5`. This standard `ln(2)` split keeps both forms in their stable regions. The reachable cases `(qs, qo, d, b) = (0, Integer.MAX_VALUE, Integer.MAX_VALUE - 2, 3)` and `(0, Integer.MAX_VALUE, Integer.MAX_VALUE - 2, 65_075_262)` exercise the large-`h` and moderate-`h` boundaries.

For `t < -37.0`, the calculator evaluates `exp(log(b) + t)`. Keeping the multiplication by `b` inside the logarithm preserves representable tiny costs such as `6.392138950083687E-44`, even when `softplus(t)` alone would be too small. If a mathematically positive purchase still rounds to zero, the method rejects it instead of granting free shares. Nonnegative current quantities, positive purchases, positive `b`, and finite outputs are checked before a future domain operation can mutate state.

`maximumSubsidy` derives `b * ln(2)`. It is a maximum-exposure measure, not cash placed into the market-maker account. The calculator tests cover the approved worked example, supplied-simulator oracle cases, ordinary invariants, larger `b`, extreme quantities, both underflow outcomes, and invalid inputs with documented absolute or relative tolerances.

## Stage 2 event domain and atomic accounting

`MarketEvent` is the aggregate owner. Its public constructor exists only so the later `JaxbMarketMapper` in the sibling `guessmarket.engine.xml` package can construct a validated event. The class exposes no other public method. Its package-private transition, conversion, and observation methods remain internal to Engine callers and same-package persistence validation. Identity-preserving observations return the actual owned nodes and collections only at this trusted package boundary so restore validation can detect accidental mutable aliases.

One event owns two identity-distinct `MarketOption` objects in fixed one-based order, one `CommissionPolicy`, one `EventAccount`, and one chronological `ArrayList<TradeRecord>`. It also owns the open or closed lifecycle and optional winner. The constructor accepts every full-range event ID, empty text, and duplicate option labels, while rejecting null required values, commission outside 0 through 90, and nonpositive `b`. New events start open with zero quantities, zero account balance, zero collected commission, no winner, and empty history.

`purchase` follows validate, calculate, then commit. Before changing any field it proves the event is open, the option and quantity are valid, and checked integer addition cannot overflow. It then asks only `LmsrCalculator` for the base cost and both candidate prices. It calculates the applicable purchase commission, total paid, candidate account balance, candidate commission total, and complete immutable `TradeRecord`, and verifies every required financial value. Only after the full candidate succeeds does it commit the selected quantity, both account values, and the new chronological history record. Arithmetic and representability failures become `FINANCIAL_CALCULATION_FAILED`; all rejected transitions serialize to exactly the same bytes before and after the call in `MarketEventTest`.

Expected failures retain stable error codes while their detail identifies the operation and failed value. A purchase on a closed event and a repeated close both use `EVENT_NOT_OPEN`, but their detail distinguishes purchasing shares from closing the event. Numerical failures identify values such as base share cost or purchase commission without exposing UI wording or low-level implementation text. The positive 1 percent commission-underflow case at `b = 1` after 742 favored shares proves the checked failure preserves serialized values and every owned identity.

`close` uses the same transaction shape. It validates the lifecycle and winner, then calculates gross payout, applicable on-close commission, net payout, candidate balance, and candidate commission total before mutation. Its single commit section updates the account once, records the winner, and changes the status to `CLOSED`. Subtracting only net payout retains an on-close commission, so the balance never receives a second commission credit. The final signed balance is preserved: positive is profit, negative is subsidy, and exact zero is break-even.

Internal history stays chronological for accounting and restore validation. `toDetails` creates new immutable DTO values in reverse chronological order for newest-first display. It derives both current prices on demand, so neither price nor `b * ln(2)` enters the serialized graph. `toSummary`, `toDetails`, and `maximumSubsidy` are observations only and cannot mutate the aggregate.

The five Task 5 domain classes implement `Serializable` and each declares `private static final long serialVersionUID = 1L`. The two DTO enums retain normal enum serialization identity. The owned collection fields use the concrete serializable `ArrayList` type, which also satisfies Java 25 serial lint. DTO classes, `LmsrCalculator`, Engine coordination types, XML types, and technical file objects were not added to the reachable event graph.

## Stage 3 Task 6: XML resources and retained JAXB source

Task 6 establishes the reproducible XML inputs before mapper or loader behavior exists. The production Engine resource owns a byte-identical trusted copy of `GM-EX1-Schema.xsd`; four course fixtures live under `fixtures/supplied` with their canonical SHA-256 values preserved. Four separately authored D-069 fixtures live under `fixtures/custom`, including the full `xs:int` range, duplicate nonpositive IDs, present empty text and duplicate labels, a valid-schema one-option business failure, commission `0` and `90`, positive `b`, a filename containing spaces, and a missing-required-description schema failure.

The intact JAXB RI 4.0.5 wrapper generated eight retained types in `guessmarket.engine.xml.generated` from the verified schema: `Comision`, `GMEvent`, `GMEvents`, `GMLMSR`, `GMMethod`, `GMOptions`, `GuessMarket`, and `ObjectFactory`. Every generated source declares the approved package and matches its staging counterpart by SHA-256. Generated source is schema-owned, kept in Git, and never hand-edited.

The generated source compilation gate used Oracle Java 25.0.4 with `--release 25 -encoding UTF-8 -Xlint:all -Werror` and only the five approved JAXB runtime JARs. It compiled all eight sources cleanly with exit code `0` and no warnings or errors. The initial sandbox denial while creating a temporary output directory was rerun through elevated execution and is retained as diagnostic evidence in the Task 6 report. Task 6 adds no mapper or loader behavior; Task 7 will prove conversion and Task 8 will prove trusted-schema loading.

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
| 2 | `LmsrCalculator` | `purchaseCost` | Calculate the D-068 cancellation-safe direct delta, combine every negative-difference numerator in `long`, preserve representable tiny values, and reject a required-positive zero | `LmsrCalculatorTest.approvedWorkedExampleMatchesAtFullPrecision`, `suppliedSimulatorOracleCasesMatch`, `largeOpposingTermsUseCombinedLongNumerator`, `moderateHNegativeDifferenceUsesCombinedLongNumerator`, and direct-delta boundary tests | Implemented |
| 2 | `LmsrCalculator` | `maximumSubsidy` | Derive `b * ln(2)` without treating it as account cash | `LmsrCalculatorTest.maximumSubsidyEqualsInitialTotalCost` | Implemented |
| 2 | `CommissionPolicy` | Constructor and commission calculations | Own validated timing and 0 through 90 percentage; calculate either purchase or close commission without state mutation | `MarketEventTest.onPurchaseCommissionAtNinetyPercentCreditsCostAndCommission`, `onCloseCommissionAtNinetyPercentIsRetainedExactlyOnce`, and `zeroPercentCommissionWorksInBothModes` | Implemented |
| 2 | `EventAccount` | Constructor, observations, `commitPurchase`, and `commitClose` | Own zero-based balance and collected commission; accept both prevalidated candidate values only at a named transition commit point | Accounting, close, and complete serialized before-after tests in `MarketEventTest` | Implemented |
| 2 | `MarketOption` | Constructor, observations, and `commitPurchase` | Own one fixed number, XML label, and nonnegative quantity; change quantity only from the aggregate's successful purchase commit | `constructorCreatesOpenZeroBasedEventAndPreservesAllowedXmlValues`, `multiplePurchasesAccumulateQuantitiesAccountingAndNewestFirstHistory`, and `quantityOverflowIsRejectedWithoutChangingAnyDomainState` | Implemented |
| 2 | `TradeRecord` | Constructor, observations, and `toHistoryEntry` | Preserve one successful purchase's option identity and finite exact financial breakdown; create a fresh immutable public history DTO | `multiplePurchasesAccumulateQuantitiesAccountingAndNewestFirstHistory` | Implemented |
| 2 | `MarketEvent` | Public constructor | Establish one self-validating open two-option aggregate with distinct owned state and a zero-based account | `constructorCreatesOpenZeroBasedEventAndPreservesAllowedXmlValues` and `constructorRejectsInvalidDomainDefinitions` | Implemented |
| 2 | `MarketEvent` | `purchase` | Validate lifecycle, option, positive quantity, and checked addition; calculate quantity, cost, commission, total, account, both prices, and record; name operation-specific failures; commit quantity, account, and history only after the candidate succeeds | Purchase, commission, history, `reachableLargeOpposingPurchasesUseCorrectedAccountingCost`, `reachableModerateHOpposingPurchasesUseStableAccountingCost`, commission-underflow identity, boundary, numerical rejection, and complete before-after tests in `MarketEventTest` | Implemented |
| 2 | `MarketEvent` | `close` | Validate open lifecycle and winner; calculate payout, commission, net payout, account, and commission total; name operation-specific failures; commit account, winner, and closed status only after the candidate succeeds | Close commission, lifecycle detail, profit, subsidy, break-even, and complete before-after tests in `MarketEventTest` | Implemented |
| 2 | `MarketEvent` | `toSummary`, `toDetails`, and `maximumSubsidy` | Return fresh immutable public projections and derived values without exposing or changing owned domain state; convert history newest-first | `constructorCreatesOpenZeroBasedEventAndPreservesAllowedXmlValues`, `multiplePurchasesAccumulateQuantitiesAccountingAndNewestFirstHistory`, and all accounting tests | Implemented |
| 2 | `MarketEvent` | Package-private state and ownership observations | Let only same-package Engine coordination and persistence validation inspect exact stored values, nodes, collections, lifecycle, and winner identities without widening the public API | `packagePrivateObservationsPreserveOwnedIdentityForLaterValidation` and public `javap` inspection | Implemented |
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
| 2 | Cancellation-safe LMSR plus self-validating event state, accounting, chronological history, DTO conversion, serialization boundary, atomic transitions, whole-branch review corrections, and residual numerical correction | `LmsrCalculatorTest`: 16 successful; `MarketEventTest`: 20 successful | Clean Java 25 compile with `--release 25 -encoding UTF-8 -Xlint:all -Werror`; focused Stage 2 gate: 36 successful; 48 total regressions successful with zero failures, skips, disabled tests, or aborts; dependency, visibility, serialization, source, and diff inspections passed | Task 4 `542768d` and `cf14539`; Task 5 `7502964`, `e0a341a`, and `3daadb1`; final review fix `be0ba85`; residual numerical fix subject `fix: stabilize residual LMSR cancellation` | Ready for human review |
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
