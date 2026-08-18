# Exercise 1 Design Brief

Status: approved through D-073. Stages 1 through 5 are accepted and merged. Stage 5 implements D-072 Clean Sections plus D-073 readable command completion and merged into `main` as `d8ef980` after the strict Java 25 all-eleven-suite gate reported 145 successful tests and real-process audits passed. Stage 6 completed the authoritative packaging pipeline. Stage 7 local verification and the supplementary IntelliJ ownership cross-check passed; clean Windows 10 and public-access evidence remain manual gates.

Last updated: 2026-08-18

## How to use this brief

Read this file first when you need the overall design without all of the decision history.

- `EXERCISE-1-DESIGN.md` is the authoritative design ledger. It contains the reasoning, alternatives, approval history, reviewed corrections, and remaining pre-implementation gates.
- `../README.md` maps the public documentation. The approved detailed implementation plan lives under `../planning/` and controls the staged execution order.
- This brief introduces no new decisions. If it conflicts with the design ledger, the ledger controls.

## Current scope

- Build Exercise 1 only.
- Include every base requirement, then attempt the 5-point save-and-restore bonus after the base system passes.
- Keep clean boundaries that can evolve in Exercises 2 and 3, but do not implement their features now.
- Use Java 25.
- Produce three separate application JARs: Console UI, Engine, and DTO.
- The standalone repository is currently private and contains the implemented Stage 1 through Stage 5 Java source, generated JAXB source, tests, and documentation. Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c`; Stage 5, including the approved D-073 manual-review correction, was accepted and merged into `main` as `d8ef980`. Aviad confirmed that the repository must become public for grader access before submission. Nitzan will make that change manually after the final public-safety review.

## Whole-system mental model

```text
Console UI
  reads input, calls the engine interface, and formats output
       |
       | GuessMarketEngine calls and immutable DTO values
       v
Engine
  owns use cases, domain state, validation, LMSR, XML, and persistence
       |
       | creates immutable public snapshots
       v
DTO
  contains data-only public value types and depends only on the JDK
```

Dependency direction:

```text
UI -> Engine
UI -> DTO
Engine -> DTO
DTO -> JDK only
```

The UI never accesses engine domain objects or JAXB-generated objects. The Engine never reads console input or prints output.

## Decisions by module

| Module     | Owns                                                                                                                                                                     | Depends on                                              | Must not own or expose                                                                                                             |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Console UI | `main`, one engine construction point, `Scanner`, menu loop, prompts, input parsing, output formatting, and recovery messages                                            | Engine and DTO                                          | Domain rules, LMSR calculations, authoritative validation, JAXB objects, or direct engine implementation access after startup      |
| Engine     | `GuessMarketEngine`, `GuessMarketEngineImpl`, domain objects, lifecycle, financial state, LMSR calculations, XML loading, validation, and the bonus persistence boundary | DTO, JDK, and later the approved JAXB runtime libraries | Console input/output, caller-specific formatting, mutable public return values, or raw low-level exceptions at its public boundary |
| DTO        | Immutable summaries, details, option snapshots, purchase receipts, history values, and any public value enums used inside them                                           | JDK only                                                | Domain mutation, calculations, XML/JAXB, persistence, console behavior, or calls back into the Engine                              |

This is a build-module split, not a decision to use Java's `module-info.java` system.

Approved package topology:

```text
guessmarket-dto
  guessmarket.dto

guessmarket-engine
  guessmarket.engine
  guessmarket.engine.xml
  guessmarket.engine.xml.generated

guessmarket-ui
  guessmarket.ui.console
```

The supported production UI boundary is narrow: UI code imports the Engine interface, structured Engine failure types, and DTOs. `ConsoleMain` may additionally construct `GuessMarketEngineImpl` once. Domain, LMSR, account, internal-history, persistence, handwritten XML, and generated JAXB types are not supported UI API even when a specific Engine collaboration requires technical public visibility.

## Approved console presentation

D-072 selects the Clean Sections direction for Stage 5. The console remains plain, deterministic, and portable, but it uses stronger hierarchy than the earlier transcript-style sketch:

- a 60-character equals-sign title band around `GUESS MARKET`;
- the stable eight commands grouped, without renumbering, under `DATA`, `TRADING`, and `STATE AND SESSION`;
- `Choose a command [1-8]:` and similarly explicit bracketed ranges for event and option prompts;
- 60-character hyphen separators around top-level blocks such as `EVENTS` and `EVENT DETAILS`, with restrained `OPTIONS`, `ACCOUNT`, and `PURCHASE HISTORY` subsections inside details;
- readable labeled event blocks rather than a fragile fixed-width table;
- no color, ANSI, Unicode box drawing, screen clearing, width detection, animation, extra confirmation, or cancellation grammar.

The complete section grammar, prompts, superseded historical shapes, and test ownership are recorded in D-072. D-073 removes the misleading permanent loaded-system note and pauses after handled commands 1 through 7 so a result remains visible until Enter. Blank or whitespace-only Enter returns to a fresh menu, nonblank pause input retries locally, EOF exits normally from every prompt, and command 8 exits immediately. These presentation decisions change only the console layer. They add no Engine method, DTO field, production class, dependency, or Exercise 2 behavior.

## Test architecture

Automated tests use JUnit Jupiter through the test-only repository dependency `tools/testing/junit-platform-console-standalone-6.1.1.jar`. Tests remain inside each owning module's package-aligned `src/test` roots. Engine use cases are tested mainly through `GuessMarketEngine`; focused calculator and domain tests may share the production package for package-private access; console tests use a small handwritten fake Engine. Production visibility is not widened for tests, Mockito and private-field reflection are not used, and JUnit is excluded from all application JARs and the final runtime distribution.

D-060 fixes the behavior-centered suite: `DtoContractTest`; `LmsrCalculatorTest`; `MarketEventTest`; `GuessMarketEngineUseCaseTest`; `GuessMarketEngineXmlLoadTest`; `GuessMarketEnginePersistenceTest`; `JaxbMarketMapperTest`; `XmlMarketLoaderTest`; `ConsoleInputTest`; `ConsoleRendererTest`; and `GuessMarketConsoleAppTest`, plus the non-test helper `FakeGuessMarketEngine`. The three console suites comprehensively automate parsing, responses, formatting, all command conversations, recovery, end-of-input, and identified edge cases. D-061 supplies the original requirement-to-test matrix, and D-071 supplies its reviewed corrections and missing rows.

D-061 originally establishes requirement-level traceability and seven acceptance gates; D-066 later expands exact-artifact acceptance to eight gates. D-071 corrects UI-versus-Engine proof ownership and adds the previously missing proof rows without adding a twelfth test class. Evidence remains divided among JUnit, authoritative-build and architecture checks, packaged-system checks, and the final manual grader walkthrough. Every specified and identified console edge category must prove both the correct response and recovery behavior. Required tests may not be silently skipped, and reproducible missing-case defects receive regression tests when technically possible. No line-coverage percentage or JaCoCo dependency is required.

The authoritative JUnit phase must capture deterministic JUnit 6.1.1 reports and output, fail on any test or container failure, skip, disabled test, or abort, and verify that all eleven named suites each execute at least one test case. Staging cannot begin until that post-test verifier succeeds. CI may invoke `build.bat`, but it may not replace this phase with a weaker independent test command.

## Packaging launch model

The complete 2020 packaging recording was reviewed and reconciled against the V2 handout and Java 25. Stable lessons about separate JARs, manifests, runtime dependencies, embedded resources, batch launchers, and exact-package testing are retained. Old IntelliJ layouts, the two-module count, and old submission contents do not control the current project.

D-062 makes the UI JAR the only executable application JAR. Its manifest contains `Main-Class: guessmarket.ui.console.ConsoleMain` and directly lists Engine, DTO, and all five JAXB runtime JARs in `Class-Path`. Engine and DTO remain library JARs without custom executable or runtime-classpath entries. The Windows launcher uses one quoted `%~dp0`-relative `java -jar` command, so it locates the UI JAR from another current directory and beneath a path containing spaces.

D-063 fixes the extracted ZIP root as `run.bat`, `README.pdf`, `guessmarket-ui.jar`, and `lib/`, with Engine, DTO, and exactly five JAXB runtime JARs under `lib`. The archive contains no extra wrapper directory. The runtime ZIP intentionally excludes source, tests, build intermediates, development tools, and IDE metadata. GitHub will be a broader source and development repository, with its exact contents and policy still pending.

D-064 selects one repository-root `build.bat` as the authoritative Exercise 1 clean compile, test, JAR, staging, ZIP, and verification entry point. It uses Windows Batch and Java 25 JDK tools only, resolves the project from `%~dp0`, quotes paths, keeps variables local, uses plainly named fail-fast phases, and cleans only the fixed project `build` output. It belongs in the source repository and not the runtime ZIP. The separate `run.bat` remains the grader launcher. D-065 and D-066 resolve the exact commands, outputs, and acceptance gates, with D-071 strengthening JUnit and clean-Windows verification.

D-065 fixes that pipeline. Repository-owned manifest and launcher inputs live under `packaging`; the identity-bearing submission `README.pdf` stays under `private/submission/assignment-1`; and one clean `build` tree separates source lists, production classes, test classes, JUnit reports, JARs, staging, verification extraction, and distributions. DTO, Engine, and UI compile in dependency order, all approved JUnit suites run before packaging, and the three application JARs are staged with exactly five JAXB runtime JARs. The UI manifest uses validated continuation lines, and Java `jar --no-manifest` creates `211722525-submission.zip` without a wrapper directory. One Java 25 `JAVA_HOME` supplies all three JDK tools. Current lecture evidence and Nitzan's confirmation select Oracle JDK 25 with HotSpot locally; Temurin may remain installed but is not used by this project.

D-066 closes Section 10 with eight mandatory acceptance gates: one coherent Oracle Java 25 environment and a clean zero-based build; complete JUnit execution with no hidden required tests; exact application-JAR ownership and manifest inspection; exact ZIP membership; fresh extraction beneath a path containing spaces; real packaged-process flows from an independent working directory; a one-time IntelliJ artifact and module-graph cross-check that never replaces `build.bat`; and a final README-led grader walkthrough with an SHA-256 record. D-071 makes a successful clean Windows 10 run of the exact ZIP mandatory. If that evidence is unavailable, the candidate retains an unresolved submission risk and is not submission-ready. Only the exact verified candidate may later be copied to `submissions` and Drive, after explicit approval.

## Startup and interface boundary

- `GuessMarketEngine` describes what callers can do.
- `GuessMarketEngineImpl` is the concrete coordinator that implements those capabilities.
- The concrete implementation is created once at startup.
- UI code keeps and uses a `GuessMarketEngine` reference, not a `GuessMarketEngineImpl` reference.
- Expected recoverable failures use one checked `EngineOperationException` contract with structured error information.
- The UI handles that common exception, explains the failure, and returns to the menu.
- The Engine does not print and does not expose raw JAXB, XML parser, or I/O exceptions.

## Approved engine capabilities

```java
int loadEventsFromXml(Path path) throws EngineOperationException;
List<MarketEventSummary> listEvents() throws EngineOperationException;
MarketEventDetails getEventDetails(int eventId) throws EngineOperationException;
PurchaseReceipt purchaseShares(int eventId, int optionNumber, int quantity)
        throws EngineOperationException;
MarketEventDetails closeEvent(int eventId, int winningOptionNumber)
        throws EngineOperationException;
void saveState(Path path) throws EngineOperationException;
int restoreState(Path path) throws EngineOperationException;
```

Exit belongs only to the UI. Load and restore return the number of loaded events. Close returns the resulting event details. Save returns after success without an unnecessary result wrapper.

## Domain ownership inside the Engine

### `GuessMarketEngineImpl`

- Owns the collection of loaded events.
- Coordinates complete use cases.
- Stores events as a `Map<Integer, MarketEvent>` using `LinkedHashMap` to preserve XML order.
- Finds an event by its real XML ID.
- Creates immutable DTO snapshots when data crosses the public boundary.

### `MarketEvent`

- Owns one event's lifecycle, two options, liquidity parameter, commission policy, event account, winner, and history.
- Protects event-level invariants and meaningful state transitions.
- Remains stored after closure so its final state and history stay inspectable.

### `MarketOption`

- Owns one XML-provided option label and its share quantity.
- Is one of exactly two ordered options in a `MarketEvent`.
- Uses external option numbers 1 and 2 based on XML order.

### `LmsrCalculator`

- Is a stateless final calculation class.
- Owns only LMSR cost, price, subsidy, and purchase-cost mathematics.
- Does not know about commission, accounts, history, XML, or UI behavior.

### Other approved domain concepts

- `EventStatus` has `OPEN` and `CLOSED` for Exercise 1.
- `CommissionMode` has `ON_PURCHASE` and `ON_CLOSE`.
- Immutable `CommissionPolicy` stores the mode and integer percentage.
- Each event owns a separate `EventAccount` with its market-maker net cash balance and total commission collected. The balance starts at `0.0`; `b * ln(2)` is a calculator-derived maximum subsidy or exposure measure, not account cash.
- Successful purchases create chronological engine-owned history records. Public history is returned as newly created immutable DTO values, newest first when required by the UI.

No abstract domain superclass is justified for Exercise 1.

## Core domain rules

- Every event has exactly two mutually exclusive options.
- Option labels come from XML. They are not hardcoded to Yes and No.
- Event IDs are the integer IDs supplied by XML. Every unique value in the full Java `int` range is valid, including zero, negatives, `Integer.MIN_VALUE`, and `Integer.MAX_VALUE`.
- The UI may show positions 1 through N, but it translates a selection to the actual event ID before calling the Engine.
- New events are `OPEN`.
- Purchases are allowed only for open events.
- Closing selects exactly one of the two options as the winner.
- A closed event cannot be purchased from or closed again.
- Closed events, final balances, details, and history remain visible.
- Prices are derived from quantities and `b`; they are not stored as mutable state.
- Fields remain private, and generic public setters must not bypass invariants.

## Numeric and display rules

- IDs, option numbers, quantities, commission percentages, and `b` use `int`.
- Prices, LMSR costs, fees, subsidies, payouts, and balances use `double`.
- The Engine keeps full calculation precision.
- The UI displays financial values with exactly two decimal places using `Locale.US` and normalizes rendered `-0.00` to `0.00`.
- A tiny positive price may display as `0.00`.
- Tests compare calculated doubles with a documented tolerance, not exact equality.
- Quantity addition is checked for integer overflow before state changes.

## LMSR rules

For quantities `q1`, `q2`, and liquidity parameter `b`:

```text
C(q1, q2) = b * ln(exp(q1 / b) + exp(q2 / b))
purchase cost = C(after) - C(before)
maximum subsidy = b * ln(2)
```

One winning share pays 1 money unit when the event closes.

The implementation uses stable log-sum-exp for total cost and subtract-the-maximum softmax for observational prices. Purchase cost is not calculated by subtracting two independently rounded total costs. D-068 requires an algebraically equivalent cancellation-safe log-domain delta using `log1p`, `expm1`, softplus-style branches, and `long` quantity differences before conversion to `double`.

A derived price may underflow to `0.0` or round to `1.0` at extreme imbalance without invalidating the event. Every successful positive purchase must still have a finite, strictly positive representable base cost calculated independently of that price. An unrepresentable positive delta or required positive commission is rejected atomically with `FINANCIAL_CALCULATION_FAILED`; it is not clamped, treated as free, or moved to `BigDecimal`.

The approved reference example is `b = 100` with a purchase of 100 option-1 shares:

- Base purchase cost: approximately `62.0114506958278`.
- Prices after purchase: approximately `0.731058578630005` and `0.268941421369995`.
- With 5 percent on-purchase commission, commission is approximately `3.10057253479139` and total paid is approximately `65.1120232306191`.

## Purchase and close behavior

### Purchase

1. Validate the event, status, option, quantity, and overflow.
2. Calculate base cost and resulting prices without mutating state.
3. Apply the commission policy.
4. Verify that all financial values are valid and finite.
5. Commit quantity, event-account changes, commission total, and history together.
6. Return one immutable `PurchaseReceipt` containing the complete result.

For `ON_PURCHASE`, commission is added to what the buyer pays and credited to the event account.

### Close

1. Validate the event, open status, and winning option.
2. Calculate gross payout as winning share quantity multiplied by `1.0`.
3. For `ON_CLOSE`, deduct commission from the gross payout and retain it in the event account.
4. Verify the full settlement.
5. Commit winner, commission, payout, final event-account balance, and `CLOSED` status together.

Exercise 1 has no user accounts. Paying winners is aggregate market accounting: the Engine subtracts the total net payout represented by all winning shares.

The market-maker account starts at `0.0`. Purchase base costs and on-purchase commission credit it. At close, subtracting the net payout already retains an on-close commission, so that commission is recorded for reporting but is not credited a second time. The derived maximum subsidy is never added to or removed from cash.

The final event-account balance is not reset. A positive balance is market-maker profit, a negative balance is the market-maker subsidy, and exact zero is break-even. Closed-event output includes an explicit `Profit`, `Subsidy`, or `Break-even` result derived from the unrounded sign. Open-event details show the current account balance without calling it realized profit or subsidy.

## DTO boundary

Approved public DTO concepts are:

- `MarketEventSummary`
- `MarketEventDetails`
- `MarketOptionSnapshot`
- `PurchaseReceipt`
- `TradeHistoryEntry`

DTO rules:

- Private final fields and getters.
- Defensive copies and immutable returned collections.
- Data only, with no calculations, mutations, file operations, or formatting.
- Created on demand from current engine state, not maintained as synchronized shadow copies.
- Every nested public type is from the JDK or DTO module.
- No mutable domain object or JAXB-generated object can cross the boundary.

Approved DTO fields are:

- `MarketEventSummary`: event ID, name, description, commission percentage and mode, two ordered option labels, and status.
- `MarketOptionSnapshot`: one-based option number, label, share quantity, and full-precision current price.
- `MarketEventDetails`: self-contained event identity and commission fields, status, two option snapshots, current event-account balance, total commission collected, newest-first immutable history, and `OptionalInt` winner.
- `PurchaseReceipt`: purchased option number and quantity, base cost, purchase commission, total paid, and the same-operation resulting event details.
- `TradeHistoryEntry`: option number and historical label, quantity, base cost, purchase commission, and total paid.

All DTO types above live in `guessmarket.dto`. DTOs never contain console formatting.

## Validation and atomicity

Validation is layered:

1. UI validates text parsing and menu-choice shape.
2. Engine-owned file/XML code validates path, readability, XML/schema structure, mapping, and complete candidate data.
3. Engine use cases validate loaded state and operation rules.
4. Domain objects protect their own construction and transition invariants.

The Engine repeats all authoritative checks even when the UI performs a simple early precheck.

Every operation follows the same atomic rule:

```text
validate and calculate completely -> commit all related changes
failure -> preserve the previous valid state
```

This applies to XML replacement, purchases, closing, save, and restore. Failed operations never create history entries or partially change quantities, balances, commission totals, winners, or lifecycle state. For save, this guarantees unchanged running Engine state, not preservation of an existing disk target after a failed move; D-070 states that publication limit explicitly.

## XML/JAXB boundary: already fixed

- XML loading is an Engine capability.
- JAXB/XJC is the lecturer's intended course workflow and is the selected workflow for this design.
- XJC generates schema-mapping classes once while the schema is unchanged.
- Generated classes are retained and regenerated only if the schema changes.
- Generated classes are never edited manually.
- Generated classes are engine-private XML integration types, not domain objects and not DTOs.
- Loaded values must be converted into application-owned candidate domain state.
- XSD-valid data can still violate business rules, such as duplicate event IDs or commission outside `0..90`.
- A valid candidate fully replaces the live system only after mapping and all validation succeed.
- An invalid candidate produces a structured engine failure and preserves the previous system.

## XML/JAXB boundary: complete

- XJC generates `guessmarket.engine.xml.generated` through the course-supplied wrapper.
- `XmlMarketLoader` owns path, trusted-XSD, and JAXB mechanics; `JaxbMarketMapper` owns business validation and complete candidate construction in `guessmarket.engine.xml`.
- Only the five supplied runtime JAXB JARs belong on Engine's runtime dependency path. XJC and JXC are development tools.
- A complete ordered candidate replaces live state through one reference assignment only after every structural and business rule succeeds.
- XML failures use the six approved structured XML codes, while low-level JAXB and I/O details remain internal.
- The canonical XSD and XML fixtures under `provided/assignment-1/test-files` are immutable. Runtime, XJC staging, and test copies derive from them.
- Generated Java is retained beneath `modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/generated` after verified staging beside the supplied XJC wrapper.
- The trusted XSD is copied to `modules/guessmarket-engine/src/main/resources/guessmarket/engine/xml/GM-EX1-Schema.xsd` and packaged at the matching Engine JAR classpath entry.
- Supplied XML fixture copies live beneath Engine test resources in `guessmarket/engine/xml/fixtures/supplied`; custom fixtures use the sibling `custom` directory. Tests never mutate canonical or resource files in place.
- IDs accept the full `xs:int` and Java `int` range, including zero, negative values, and both endpoints; uniqueness across the complete candidate is the only additional ID rule.
- Event names follow their XSD list reconstruction, descriptions and option labels keep the approved source normalization, and empty or whitespace-only text remains valid where the XSD permits it. Duplicate option labels remain valid. The mapper adds no nonblank, label-uniqueness, case-normalization, placeholder, or arbitrary-length rule.
- Exercise 1 still requires exactly two ordered options, commission from `0` through `90` inclusive, and positive `b`.
- Engine development references the five runtime JAXB JARs directly beneath `tools/jaxb-ri-4.0.5/mod`. UI and DTO remain JAXB-free, and XJC or JXC compiler tools do not enter the runtime classpath.
- XJC generation, file copying, and IntelliJ configuration have not started.

## Console interaction and recovery: complete

- One stable eight-command menu covers XML load, event overview, detailed status, purchase, close, save, restore, and UI-only exit.
- Commands remain visible with stable numbering. The Engine alone decides whether loaded state exists.
- One `Scanner` reads complete lines. UI-shape errors repeat the current prompt; structured Engine failures end the command and return to the menu; unexpected programming defects are not swallowed.
- Event lists preserve load order. The UI filters summaries by status for OPEN-only purchase and close choices, assigns fresh one-based positions, translates a displayed position into the actual XML ID, and calls the normal Engine operation with that ID. The Engine validates actual IDs and lifecycle state but exposes no console-specific filtering or position API.
- Purchase uses one atomic `PurchaseReceipt`; close uses returned final `MarketEventDetails`; neither performs a redundant post-operation query.
- The UI renders labeled summary and detail blocks, newest-first history, and fixed two-decimal US financial values. Closed details add an explicit final market-maker `Profit`, `Subsidy`, or `Break-even` line from the unrounded account sign. The Engine and DTO modules return typed data, not formatted strings.
- Non-XML failures use the approved loaded-state, event, option, quantity, calculation, and saved-state codes.
- `ConsoleMain`, `GuessMarketConsoleApp`, `ConsoleInput`, and `ConsoleRenderer` live in `guessmarket.ui.console`. Only `ConsoleMain` is public unless a later named requirement justifies wider visibility.
- Section 8 acceptance scenarios cover input recovery, command order, actual-ID mapping, open and closed filtering, atomic purchase and close behavior, save and restore flows, formatting, error mapping, and unexpected-defect visibility.

## Bonus persistence: complete

- The bonus uses Java object serialization with one private Engine-owned `SavedState` root containing `formatVersion = 1` and the complete ordered domain event collection. It does not introduce dedicated duplicate snapshot classes or purchase replay.
- Only reachable Engine domain, option, account, commission-policy, lifecycle, and internal-history types deliberately implement `Serializable`; every custom serializable class declares private `serialVersionUID = 1L`.
- `GuessMarketEngineImpl`, DTOs, handwritten and generated JAXB types, UI types, calculators, loaders, paths, streams, and other technical objects are not serialized.
- Engine resolves the UI-supplied base path to the conventional `.ser` extension and avoids duplicating an already supplied case-insensitive suffix.
- Stored finite historical costs, commissions, totals, and zero-based account balances are preserved exactly as binary64 values. Prices and `b * ln(2)` maximum subsidy remain derived and are not stored as cash.
- Restore installs a stream-specific exact-class allowlist with `maxdepth = 32`, deserializes one root into quarantine, verifies the root and version, validates the complete graph and cross-field invariants, builds a fresh `LinkedHashMap` keyed by unique full-range IDs, and commits by one live-map reference replacement. It does not replay purchases or recompute accepted history.
- Save closes a complete unique temporary sibling before first attempting `Files.move(temp, target, ATOMIC_MOVE)`. If that fails and the temporary file remains, it retries with `REPLACE_EXISTING`; a successful fallback is explicitly non-atomic, and a failed move does not guarantee preservation of the previous target.
- The `.ser` format is trusted-local bonus storage, not an authenticated hostile-input interchange. Invalid roots, versions, classes, streams, or domain graphs are rejected while preserving the previous live Engine state.
- Persistence acceptance scenarios cover first save, overwrite, observable publication paths, honest failure behavior, strict filtering, invalid candidates, real restart through a new Engine, complete state and history, continued operation, full-range IDs, corrected account and numerical history, one-reference commit, path behavior, and temporary-file cleanup.

## Explicitly deferred features

- JavaFX and Exercise 2 screens.
- Users, personal balances, blocking, and permissions.
- Order Book, orders, matching, bid, ask, spread, and mid-price.
- Exercise 2 lifecycle states.
- Tomcat, servlets, HTTP, polling, Gson, and WAR packaging.
- Exercise 3 identity rules and concurrency.

## Current implementation boundary

The adversarial review and D-067 through D-071 correction pass are complete. D-072 records the approved Stage 5 Clean Sections presentation, and D-073 records the approved readable command-completion and graceful-input correction. The requirement and lecturer-checklist audits have no remaining blocking design finding.

The standalone GitHub repository is the approved source repository. It remains private during active implementation and review, then must become public for grader access before submission as confirmed by Aviad on 2026-08-17. It must preserve the unchanged three-module layout, invoke the authoritative `build.bat` in any future CI rather than bypassing its mandatory JUnit verifier, track generated JAXB source, custom XML fixtures, and build inputs, exclude the private submission README and runtime `.ser` files, and treat a clean Windows 10 run of the exact final ZIP as mandatory evidence that CI cannot automatically replace.

The detailed staged implementation plan is approved and active. Stages 1 through 5 are accepted and merged. Stage 5 follows D-072 and D-073; Tasks 11, 12, and 12A merged into `main` as `d8ef980` after the renewed strict gate passed all 145 tests and real-process audits covered the complete workflow, pre-load recovery, and EOF at the pause. Stage 6 is complete. Stage 7 has passed local package, README-led, privacy, and supplementary IntelliJ ownership checks; clean Windows 10 and public-access evidence remain external.

The earlier combined proposal using Section 10 labels D-054 through D-056 was not approved. New D-054, D-055, and D-056 decisions were later explained and approved separately for build authority, module-local roots, and package topology with supported API visibility.
