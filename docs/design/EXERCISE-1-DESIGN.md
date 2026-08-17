# Exercise 1 Design

Status: approved through D-072. Stages 1 through 3 are accepted and merged. Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c`. Stage 5 implements the approved Clean Sections console presentation, is independently reviewed on `codex/e1-console-ui`, and has been successfully rebased directly onto `9a8c87c`. Current Task 12 evidence is commit `9fd2d5d`, and the current known strict Java 25 all-eleven-suite result is 137 successful tests. Stage 5 remains unmerged and unpushed, and now awaits Nitzan's Stage 5 review and acceptance.

Last updated: 2026-08-17

Evidence status: reconciled through the 2026-08-15 adversarial review against the current V2 handout, supplied schema and fixtures, lecturer checklist and learning guide, current forum guidance including Aviad Cohen's market-maker account clarification, the supplied simulator, Java 25 and JUnit primary documentation, and the complete decision history through D-066.

## Purpose and continuity rule

This is the authoritative, continuous design ledger for Guess Market Exercise 1. The master roadmap explains the process from design through submission. This file records the actual choices Nitzan approved, their motivation, their consequences, the items still pending, and the exact next checkpoint.

After every approval round, update this file before advancing to the next design checkpoint. A chat summary, internal memory, or project handoff is not a substitute for this ledger. The repository-local design record controls when another summary conflicts with it, while the current handout and lecturer material continue to control assignment requirements.

## Current boundary

- The written design is complete. Stages 1 through 3 are accepted and merged. Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c`. The Java source tree, three-module project, retained generated JAXB classes, and Stage 5 console implementation exist on `codex/e1-console-ui`, successfully rebased directly onto that merge. Stage 5 remains unmerged and unpushed while awaiting Nitzan's Stage 5 review and acceptance.
- Exercise 1 is the implementation scope.
- Exercise 2 and Exercise 3 are compatibility evidence only. Their features will not be designed or implemented early.
- The target is every base requirement plus the 5-point save-and-restore bonus.
- The earlier personal target of 2026-08-13 has passed. The current official working deadline remains 2026-08-19 unless newer LMS or forum guidance overrides it.
- Nitzan participates in every material design decision and must be able to explain the resulting project.

## Source authority

1. Current LMS and forum instructions.
2. Current `Guess Market - v2.docx` and its appendices.
3. Supplied XSD, XML fixtures, and LMSR simulator.
4. Current lecturer clarifications and the two current Exercise 1 discussions.
5. `Exercise 1 - Lecturer Quality Checklist.md` and `Exercise 1 - What to Know and Learn.md`.
6. Current lecture material for the concepts it teaches.
7. Older recordings for concepts only.

## Decision workflow

- Begin every checkpoint with where the design currently stands, what is being decided, why it is needed now, and what later work depends on it.
- Before showing options for a major decision, explain the subject in plain language, give its mental model, motivations, architectural role, and downstream effects. Names, directories, or option labels alone are not enough context for approval.
- For a major decision, explain the motivation, intuition, fixed requirements, realistic options, tradeoffs, recommendation, consequences, and the exact approval question. Handle major decisions individually.
- For a medium decision, explain the motivation and meaningful tradeoffs concisely, then ask for approval when more than one approach is genuinely reasonable.
- Group related small decisions into one clearly numbered batch. Do not force Nitzan to approve mechanical details one at a time.
- Offer alternatives only when several approaches are genuinely reasonable. Nitzan chooses among those approaches after seeing the recommendation.
- When the handout fixes the answer or one approach is clearly superior and alternatives would be artificial, state the answer directly, explain the motivation and consequences, and continue without creating fake options.
- A rejected or clearly inferior path may still be explained to show why the recommendation is sound, but it must not be presented as a selectable option when Codex would not genuinely recommend choosing it.
- A decision counts as approved only when Nitzan explicitly approves the identified checkpoint. An accidental approval that is withdrawn does not count.
- Record approved, not selected, withdrawn, pending, and deferred items separately before advancing to the next design checkpoint.
- Update this ledger after every approval round. Do not rely on chat history, a context window, internal memory, or a handoff as the only record.
- Check the lecturer quality checklist throughout design, implementation, and review.
- Use abstract classes and lambdas only when they have a real responsibility or readability benefit, not to demonstrate syntax.
- Use text explanations and diagrams by default. The web preview is deferred unless a visual is materially more useful than text.

## Decision status register

This section preserves both the selected path and the meaningful alternatives that were actually discussed. It prevents a rejected or premature proposal from becoming an accidental commitment in a later task.

Status meanings:

- **Approved**: Nitzan explicitly accepted the identified decision.
- **Not selected**: a realistic alternative was discussed but another option was approved.
- **Withdrawn**: a proposal appeared before its prerequisite checkpoint and was explicitly removed from consideration in that form.
- **Deferred**: deliberately postponed to a later exercise or later design checkpoint.
- **Pending**: still requires design and approval for Exercise 1.

### Scope and naming alternatives

| Topic                     | Approved choice                                                       | Alternatives not selected                                                                                              | Reason                                                                                                                   |
| ------------------------- | --------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Exercise scope posture    | Build Exercise 1 only, with measured boundaries that can evolve later | Current-only design that would likely require broad restructuring; generalized Exercises 1 to 3 architecture built now | The selected posture preserves useful seams without implementing future features prematurely.                            |
| Central event name        | `MarketEvent`                                                         | `Event`; `PredictionMarket`                                                                                            | `MarketEvent` follows the handout's event vocabulary, adds domain clarity, and avoids confusion with JavaFX event types. |
| Two-option representation | Two dedicated `MarketOption` objects owned by `MarketEvent`           | Separate first/second fields; parallel name and quantity collections                                                   | The object keeps one option's related data together and avoids synchronization and branching errors.                     |
| Literal outcome labels    | XML-provided labels, exactly two                                      | Hardcoded `YES` and `NO` enum                                                                                          | Official fixtures use labels such as Argentina and Spain, so binary means two outcomes, not fixed wording.               |

### Architecture and contract alternatives

| Topic                         | Approved choice                                                                                                                                                                                     | Alternatives not selected or withdrawn                                                                                                                                                | Reason                                                                                                                                                                                                                            |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Module architecture           | Focused three-module design: console UI depends on the engine contract and DTOs, engine depends on DTOs, and DTO depends only on the JDK                                                            | Keeping DTO classes inside the engine JAR; minimal two-module design where UI calls the concrete engine directly; oversized API, implementation, launcher, XML, and persistence split | A dedicated DTO JAR makes the public data boundary explicit without splitting every technical concern into its own module.                                                                                                        |
| Internal ownership            | Engine implementation coordinates, `MarketEvent` protects event state, `MarketOption` owns option state, `LmsrCalculator` owns pure mathematics                                                     | No artificial ownership alternatives were created because this responsibility split was clearly preferable                                                                            | Each state item has one owner, while UI, XML mapping, and calculations stay separate.                                                                                                                                             |
| Expected engine failures      | Checked `EngineOperationException` with structured error information                                                                                                                                | Generic result wrapper around every success; `null`; boolean-only returns; engine printing; raw JAXB or I/O exceptions; unchecked exceptions for ordinary user mistakes               | The checked contract forces callers to handle recoverable failures while keeping successful values direct and UI messaging separate.                                                                                              |
| Engine operation shape        | Seven explicit typed capabilities                                                                                                                                                                   | One generic `executeCommand(commandNumber, arguments)` method                                                                                                                         | The engine represents market use cases rather than console menu numbers and retains Java type safety.                                                                                                                             |
| Engine return boundary        | Immutable summaries, details, snapshots, history values, and `PurchaseReceipt`; simple `int` or `void` where sufficient                                                                             | Mutable domain objects; JAXB objects; formatted strings; unnecessary `LoadSummary`, `SaveSummary`, `RestoreSummary`, and `CloseResult` wrappers                                       | The UI receives coherent read-only information without unnecessary wrapper classes or access to live state.                                                                                                                       |
| JAXB integration boundary     | Generated package `guessmarket.engine.xml.generated`, with handwritten `XmlMarketLoader` and `JaxbMarketMapper` in `guessmarket.engine.xml`                                                         | One combined JAXB loader and mapper; JAXB code directly inside `GuessMarketEngineImpl`; generated classes used as domain objects                                                      | File and JAXB mechanics remain separate from application mapping and validation, generated code stays replaceable, and the engine coordinator remains focused.                                                                    |
| XJC generation workflow       | Follow the course guide: run the supplied wrapper beside its `mod` directory, generate the approved package tree beside the wrapper, then copy the complete tree into the future Engine source root | Moving the wrapper; editing generated files; generating directly into an undecided project source tree                                                                                | The wrapper depends on its relative `mod` path, and the physical Engine source root is intentionally deferred to the package and build checkpoint.                                                                                |
| JAXB dependency placement     | Keep the intact seven-JAR distribution under `tools`; associate the five supplied runtime JARs only with the Engine module; use XJC and JXC JARs as development tools only                          | Put all seven JARs on the application runtime classpath; attach JAXB to UI or DTO; replace the supplied distribution with another provider or version                                 | Only Engine source imports or executes JAXB. The supplied release notes classify five JARs as runtime and two as compiler tools, and an isolated Java 25 smoke check verified generation plus unmarshalling with that separation. |
| XML validation and load order | Validate the path, unmarshal against an Engine-controlled XSD, map and validate every business value into a fresh ordered candidate, then replace live state once                                   | Trust the input file's schema-location hint; rely only on XSD validation; mutate live state while reading                                                                             | External XML is untrusted until every structural and business rule succeeds. A staged candidate gives precise responsibility boundaries and preserves the previous valid system after any failure.                                |
| Candidate-state construction  | Mapper builds the Engine's complete `LinkedHashMap<Integer, MarketEvent>` candidate; loader returns it; Engine takes sole ownership with one reference replacement                                  | Add a one-field candidate wrapper; return a list and rebuild the map in the Engine; clear and refill the live map                                                                     | The direct map matches approved storage, keeps generated types inside the XML package, preserves one validation owner, and makes successful commit a single visible step.                                                         |
| Persistence representation    | Serialize one private Engine-owned `SavedState` root containing the deliberately serializable ordered domain graph, then validate it in quarantine before one live-map replacement                  | Dedicated duplicate snapshot plus replay; serialize `GuessMarketEngineImpl`; design a second XML or custom text or binary format                                                       | D-070 keeps the five-point bonus small, preserves all required state, and prevents any deserialized candidate from becoming live before complete type, identity, value, and cross-field validation.                                  |
| Build authority               | Constrained hybrid: IntelliJ remains the development and module environment, while a repository-owned Java 25 command-line build is authoritative for clean compilation, three-JAR packaging, and verification | IntelliJ-artifact-only authority; command-line-only development; Maven or Gradle                                                                                                      | The selected model preserves the course's IntelliJ workflow while making the actual submission reproducible and testable outside the IDE without adding another build framework.                                                  |
| Module and source layout      | `modules` contains `guessmarket-dto`, `guessmarket-engine`, and `guessmarket-ui`; each uses conventional module-local `src/main/java`, optional `src/main/resources`, `src/test/java`, and optional `src/test/resources` roots | One undifferentiated `src` per module; centralized project-level DTO, Engine, and UI source folders                                                                                    | Explicit module-local roots make production code, resources, and tests predictable to IntelliJ and the authoritative build while leaving the codebase ready to evolve through later assignments.                                  |
| Package topology and API boundary | DTOs use `guessmarket.dto`; the supported Engine contract and cohesive Engine internals use `guessmarket.engine`; handwritten XML adapters use `guessmarket.engine.xml`; generated mappings use `guessmarket.engine.xml.generated`; console classes use `guessmarket.ui.console` | A dedicated `.api` package without JPMS enforcement; fine-grained Engine subpackages that would force internal types and methods to become public or require extra adapters | The selected topology makes module ownership and the caller boundary obvious while preserving useful package-private collaboration and avoiding public leakage caused by Java's non-hierarchical subpackages. |
| JAXB-derived file and development dependency placement | Retain generated Java beneath the Engine production source root; package a verified trusted XSD beneath the matching Engine resource path; keep supplied and custom XML test fixtures separate beneath Engine test resources; reference the five runtime JARs in place under `tools` during development | Generate directly into production source; load the canonical `provided` XSD at runtime; mutate canonical fixtures; duplicate vendor JARs inside a module; put XJC or JXC on the runtime classpath | Each derived copy has one explicit role, the canonical course files remain immutable, Engine tests and packaging are self-contained, and development has one vendor-distribution authority. |
| Final packaging decision timing | Let a separate task review the mandatory recording in parallel while this design task continues; reconcile its report before approving the exact distribution model and always before final packaging or submission verification | Stop the current design session for the 61-minute recording; ignore the recording until the submission deadline | Exact manifests, launcher syntax, filenames, and staging do not affect domain or test architecture, while a parallel evidence task preserves momentum and an explicit reconciliation gate preserves the mandatory review. |
| Test framework and placement | Use JUnit Jupiter through the repository-owned JUnit Platform Console Standalone 6.1.1 test JAR; keep tests module-local in package-aligned `src/test` roots; use interface-first Engine tests and a handwritten fake Engine for UI tests | Manual checks alone; a custom JDK-only test harness; Mockito or reflection-based private-state testing | One test-only runner gives IntelliJ and the authoritative command line the same discoverable suites and useful failure reporting without changing the application runtime or weakening production encapsulation. |
| Test-suite topology | Use eleven behavior-centered test classes across DTO, Engine, Engine XML, and console UI, plus one handwritten fake Engine helper; place each scenario at the lowest layer that can explain its failure | One giant end-to-end suite; one test class for every production class, getter, or private helper | Behavior-centered suites keep failures local without duplicating trivial implementation structure, while the three console suites automate input, response, formatting, command-flow, and recovery edge cases. |
| Test traceability and acceptance | Map every approved requirement and edge-case category to JUnit, authoritative-build, packaged-system, or final-manual evidence; require all applicable gates to pass with no silently skipped required tests | Rely on an informal test list; use a line-coverage percentage as the main quality gate | Requirement traceability proves the specified behavior directly, while layered acceptance checks cover source behavior, architecture, produced artifacts, and the real grader experience. |
| Runtime launch and classpath model | Make the UI JAR executable with `Main-Class` and a complete manifest `Class-Path`; launch it with one quoted `%~dp0`-relative `java -jar` batch command | Put the complete classpath and fully qualified main class in an explicit `java -cp` batch command | The selected model remains valid in Java 25, matches the lecturer's preferred and handout-aligned executable-JAR flow, resolves dependency paths relative to the UI JAR, and stays safe when the launcher is invoked from another working directory or a path containing spaces. |
| Submission distribution layout | Put `run.bat`, `README.pdf`, and executable `guessmarket-ui.jar` directly at the extracted ZIP root; put `guessmarket-engine.jar`, `guessmarket-dto.jar`, and exactly five JAXB runtime JARs under one `lib` directory | Put every JAR at the root; add `bin`, `lib/app`, or `lib/third-party` subdivisions; include an extra wrapper directory inside the ZIP | The flat grader-facing root makes launching and reading instructions immediate, while one `lib` directory keeps all non-entry-point runtime dependencies together and gives the UI manifest stable, simple relative paths. |
| Authoritative build automation | Use one repository-root `build.bat` as the authoritative clean compile, test, JAR, staging, ZIP, and verification entry point | Use one PowerShell build script | Windows Batch is sufficient for the bounded Exercise 1 pipeline when kept simple, quoted, path-relative, phase-oriented, and fail-fast. It matches Nitzan's preference and the course's existing Batch usage while requiring no build system beyond Windows command processing and Java 25 JDK tools. |
| Build, manifest, staging, and archive mechanics | Use one clean `build/` tree with separate production and test outputs, a repository-owned manifest and launcher, a private submission README input, ordered Java 25 compilation and tests, exact clean staging, and a no-wrapper ZIP | Manually assemble output or reuse IntelliJ directories and artifacts as submission inputs | Clean isolated outputs prevent stale or test classes from leaking into application JARs; controlled inputs and exact staging make the produced ZIP reproducible and auditable. |
| Package acceptance verification | Require eight gates covering one coherent Java 25 environment, a clean build and complete automated tests, application-JAR inspection, exact ZIP membership, fresh extraction, real packaged-process scenarios, a one-time IntelliJ artifact cross-check, and a final grader walkthrough | Treat successful compilation or a successful launch from staging as sufficient; make IntelliJ output the submission authority | Separate gates prove behavior, architecture, archive contents, portability, and grader usability against the exact extracted candidate. IntelliJ provides an independent course-aligned cross-check without replacing the reproducible `build.bat` authority. |

### Numerical and language-feature alternatives

| Topic                      | Approved choice                                                                    | Alternative not selected                                                     | Reason                                                                                                                                                          |
| -------------------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Financial calculation type | `double` with full internal precision and tolerance-based tests                    | `BigDecimal`                                                                 | LMSR depends on `Math.exp` and `Math.log`; converting their `double` results to `BigDecimal` would not restore exactness and would add course-level complexity. |
| LMSR evaluation            | Stable log-sum-exp totals, stable logistic prices, and a direct cancellation-safe log-domain purchase delta | Direct exponentials; subtraction of two rounded total costs                   | D-068 avoids overflow and catastrophic cancellation while stating the unavoidable binary64 underflow boundary honestly.                                          |
| Domain abstraction         | Concrete domain classes and an engine interface; no abstract domain superclass now | Abstract `MarketEvent` or calculator created only to demonstrate inheritance | No current Exercise 1 subtype or shared inherited state justifies an abstract base.                                                                             |
| Lambdas                    | Use only where a standard functional operation becomes clearer                     | Decorative lambda use                                                        | Course concepts should improve readability, not become ceremony.                                                                                                |
| Visual companion           | Text-first design discussion                                                       | Continuous web preview                                                       | Nitzan found the preview unhelpful; it remains available only for a materially useful visual.                                                                   |

### Approval correction history

1. Small Decision Batch A initially appeared to be approved, but Nitzan immediately withdrew that accidental approval. The batch was presented again in full and then explicitly approved. Only the second approval is authoritative.
2. The first interface sketch containing `LoadSummary`, `PurchaseResult`, `CloseResult`, `SaveSummary`, and `RestoreSummary` was paused before approval when Batch A was reopened.
3. A later combined `PurchaseResult` and `CloseResult` proposal was introduced before the interface checkpoint had restarted. Nitzan challenged the jump, and those names were explicitly discarded as commitments.
4. The interface was then designed in dependency order: seven capabilities, method names and parameters, validation and recovery, and finally return types. The approved contract in D-010 and D-011 supersedes every earlier sketch.
5. The LMSR architecture and numerical-stability checkpoint is approved. Nitzan reconfirmed that approval during the 2026-08-11 ledger recovery.
6. Lecturer guidance received after the earlier architecture approval explicitly raised a DTO module as a valid additional module. Nitzan approved the dedicated DTO-module choice on 2026-08-12. D-002 and D-011 now supersede the earlier two-module placement while preserving the already approved UI-engine separation.
7. An earlier proposed Section 10 batch labeled D-054 through D-056 was stopped before approval because it compressed important build, module, package, and source-layout choices without first explaining the complete section. That combined proposal remains withdrawn. New D-054, D-055, and D-056 decisions were later explained and approved separately for build authority, module-local roots, and package topology with the supported API boundary.

## Approved architecture

```text
Console UI JAR
  - main
  - Scanner input and menu loop
  - output formatting and recovery messages
  - knows engine behavior only through GuessMarketEngine
  - consumes immutable DTO values
  - depends on Engine JAR and DTO JAR
             |
             v
Engine JAR
  - GuessMarketEngine interface
  - GuessMarketEngineImpl use-case coordinator
  - domain objects and invariants
  - LMSR mathematics
  - XML boundary
  - bonus persistence boundary
  - creates DTO values at its public boundary
  - depends on DTO JAR
             |
             v
DTO JAR
  - immutable summaries, details, snapshots, receipts, and history values
  - public value types needed inside those DTOs
  - no UI, engine, domain, JAXB, persistence, or console dependency
  - depends only on the JDK
```

### D-001: Balanced future-compatible posture

Decision: implement only Exercise 1 while choosing clean boundaries that can be adapted for Exercises 2 and 3.

Motivation: the same project will evolve later, but predicting and implementing later features now would increase complexity and risk the current grade. UI independence, typed engine results, isolated LMSR rules, and one clear state owner provide useful seams without building JavaFX, users, order books, HTTP, or concurrency early.

### D-002: Focused three-module split

Decision: use three IntelliJ build modules, each compiled separately and packaged as its own application JAR.

- The console UI module produces the UI JAR and owns `main`, `Scanner`, prompts, menus, display formatting, and console output.
- The engine module produces the engine JAR and owns the public engine contract, use-case coordination, domain state, calculations, XML loading, validation, and persistence boundary.
- The DTO module produces the DTO JAR and owns only the immutable data types exchanged across the engine boundary.
- The UI depends on the engine and DTO modules. The engine depends on the DTO module. The DTO module depends on neither the UI nor the engine and uses only JDK types.
- The engine never reads console input and never prints. It remains caller-independent.
- The concrete engine is created once at startup and passed to UI code through the interface type.
- This is a build and dependency split, not a decision to use Java's `module-info.java` module system.

Motivation: separate UI and engine artifacts are required, Aviad strongly recommends expressing that boundary through a Java interface, and the dedicated DTO module gives both layers a small shared vocabulary without allowing the UI to depend on mutable domain or JAXB classes. The cost is one additional JAR and explicit mapping code, which is proportionate because these DTO types already exist in the approved engine contract.

### D-003: Engine interface and implementation relationship

Decision:

- `GuessMarketEngine` is the interface that describes the capabilities available to callers.
- `GuessMarketEngineImpl` is the ordinary concrete class that implements those capabilities and coordinates use cases.
- UI code holds a `GuessMarketEngine` reference, not a `GuessMarketEngineImpl` reference.

Motivation: the interface says what the engine can do, while the implementation owns how it does it. This protects the UI from internal domain, XML, persistence, and LMSR changes.

### D-004: Internal ownership split

Decision:

- `GuessMarketEngineImpl` owns the loaded event collection and coordinates use cases.
- `MarketEvent` owns one event's lifecycle, financial state, options, account, and history and protects their invariants.
- `MarketOption` owns one outcome label and its share quantity.
- `LmsrCalculator` is a stateless calculation class that owns LMSR formulas only.

Motivation: the engine coordinates the system, the event protects event-level consistency, and the calculator remains pure mathematics. This avoids both a single oversized engine class and unnecessary framework-like abstractions.

No domain superclass or abstract domain class is currently justified. `MarketEvent`, `MarketOption`, and `GuessMarketEngineImpl` are classes. The lecturer checklist does not require an abstract class merely for its own sake.

## Approved domain model

### D-005: Binary event meaning

Decision: every Exercise 1 event has exactly two mutually exclusive options.

Binary means exactly two outcomes. It does not require the literal labels `Yes` and `No`. Labels come from XML and may be values such as `Argentina` and `Spain`. The application must not hardcode Yes/No wording.

### D-006: Event and option representation

Decision:

- Store events as `Map<Integer, MarketEvent>` with `LinkedHashMap` as the concrete implementation.
- Preserve XML event order for predictable listing.
- Each event stores exactly two `MarketOption` objects in an ordered `List<MarketOption>`.
- External option numbers are `1` and `2`, derived from XML order.
- Event identifiers remain the integer IDs supplied by Exercise 1 XML.
- The UI may display events as positions `1..N`, but it maps that selection to the actual XML event ID before calling the engine.
- Fields are private and there are no generic public setters that can bypass invariants.
- Current option prices are derived from quantities and `b`; they are not stored as mutable state.
- Closed events remain stored and visible for inspection and history.

Motivation: a map provides direct ID lookup, `LinkedHashMap` preserves deterministic display order, and the ordered two-item list matches the assignment's binary event model without hardcoded labels.

### D-007: Lifecycle

Decision: Exercise 1 uses `EventStatus.OPEN` and `EventStatus.CLOSED`.

- A loaded event begins open.
- Purchases are allowed only while open.
- Closing an event selects exactly one of its two options as the winner.
- A closed event cannot be purchased from or closed again.
- Closed events and their history remain inspectable.
- Exercise 2 lifecycle states are deferred.

### D-008: Numeric types and display precision

Decision:

- IDs, option numbers, quantities, commission percentages, and `b` use `int`.
- Prices, LMSR costs, fees, subsidies, payouts, and account balances use `double`.
- Calculations retain full internal precision.
- The console displays numeric financial values with at most two decimal places using `Locale.US`.
- A valid tiny nonzero price may therefore display as `0.00`; no special extra digits or scientific notation are required for this Exercise 1 edge case.
- Comparisons involving calculated `double` values use a documented tolerance rather than exact equality.
- Integer additions such as existing quantity plus purchase quantity are checked for overflow before mutation.

Motivation: these types match the handout's data and Java course level. Rounding inside the engine would accumulate errors, so rounding belongs only at the presentation boundary.

## Approved engine contract

### D-009: Checked failure contract

Decision: expected engine operation failures use checked `EngineOperationException`.

- Engine public operations declare this exception.
- `EngineOperationException` belongs to `guessmarket.engine` because it is part of the Engine's supported public failure contract.
- The engine never prints and does not expose raw JAXB, parsing, or low-level I/O failures as its public contract.
- The exception carries structured information for the failure category and, where meaningful, what failed, why it failed, the involved event, option, field, or path, and a recovery hint.
- The UI catches the common `EngineOperationException` base type, reads structured fields rather than parsing `getMessage()`, explains the failure, and returns to the menu. The checked choice therefore does not require one catch block per failure category.
- Low-level causes may be retained for developer diagnosis while remaining outside the user-facing engine contract.
- An expected failure must not terminate the application.
- A rejected operation must leave the previous valid state unchanged.

Motivation: callers are forced to handle recoverable assignment failures, while UI recovery and messaging remain outside the engine.

The lecturer permits checked or unchecked application exceptions and notes that many distinct checked exceptions can create excessive catch blocks. This design keeps one checked public base type, so callers handle one engine failure contract while structured categories preserve specific information. The choice can be revisited later if API evolution creates real propagation noise.

### D-010: Engine capabilities and names

Decision:

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

Exit is UI-only and therefore is not an engine operation.

### D-011: Dedicated immutable DTO boundary

Decision:

- `MarketEventSummary` is the compact event-list view.
- `MarketEventDetails` is the detailed event snapshot.
- `MarketOptionSnapshot` is the immutable public representation of one option inside summaries or details.
- `PurchaseReceipt` reports base share cost, purchase commission, total paid, and the resulting `MarketEventDetails` from the same completed operation.
- The public history representation is immutable and contains the approved `TradeHistoryEntry` values without exposing the mutable internal list.
- Load and restore return the number of loaded events.
- Close returns the resulting `MarketEventDetails`.
- Save returns no value after success.
- These result and view types live in the DTO module. They are ordinary immutable classes with private final fields, getters, and defensive copies of collections.
- Returned lists are immutable.
- DTOs are created on demand when an engine operation returns data. The engine does not maintain synchronized shadow DTO copies of its live domain state.
- DTOs contain data only. They do not perform market calculations, mutate engine state, load files, format console output, or call back into the engine.
- Every type that appears in a DTO field is either a JDK type or another DTO-module type. If a public status or commission-mode enum is exposed, that public enum also belongs to the DTO module or is mapped to another DTO-safe value.
- The public `TradeHistoryEntry` is distinct from the engine's stored history state. The engine builds immutable history DTO values when data crosses the boundary.
- JAXB-generated classes are XML integration objects inside the engine boundary, not the public DTOs.
- Current engine inputs are already small typed values such as `Path` and `int`, so Exercise 1 does not need separate input DTO classes now.
- The engine never returns mutable domain objects, JAXB-generated objects, or preformatted console strings, including through nested DTO fields.

Motivation: callers receive exactly the information they need without gaining the ability to corrupt engine state. The separate module makes that promise enforceable at compile time and reusable by a later UI, while on-demand deep conversion prevents stale snapshots and hidden references back into the engine.

## Approved validation and recovery design

### D-012: Validation layers

Decision: validation is divided by responsibility.

1. Console UI validates parsing and menu-choice shape.
2. The engine-owned file/XML boundary validates path and readability, XML/schema structure, safe conversion, and application data before committing candidate state.
3. The engine validates loaded-system and use-case rules.
4. Domain objects protect invariants during construction and state transitions.

The UI's responsibility ends after collecting and parsing user input. It may perform a simple path-shape or extension precheck for immediate feedback, but the engine repeats every authoritative file and XML check because it cannot trust a particular UI caller.

Motivation: no one class should parse text, understand XML, enforce market rules, and print recovery messages.

### D-013: Atomic operations

Decision: validate and calculate completely before changing live state.

```text
valid candidate load -> validate fully -> replace current system
invalid candidate load -> report failure -> preserve current system

valid purchase or close -> calculate fully -> commit all related changes
invalid purchase or close -> report failure -> change nothing
```

This includes quantities, account balance, commission totals, lifecycle state, and history. Partial state changes are forbidden.

### D-014: Input normalization

Decision:

- Read console input as complete lines and parse explicitly.
- Trim outer whitespace where appropriate.
- Treat relevant textual choices case-insensitively.
- Use `Locale.ROOT` for programmatic case normalization.
- Preserve meaningful internal spaces and original labels and paths.
- Do not reject an otherwise valid choice only because the user used capital letters differently.
- Check the `.xml` extension case-insensitively while preserving the original path text.

### D-015: Required validation coverage

The design must cover, at minimum:

- no loaded system when an operation requires one;
- missing, unreadable, malformed, or schema-invalid XML;
- invalid XML-to-domain values;
- duplicate event IDs;
- an event with anything other than exactly two options;
- invalid or blank required text according to the final XML field matrix;
- non-positive `b`;
- commission outside the handout's inclusive `0..90` range;
- nonexistent event ID;
- invalid option or winning-option number;
- non-positive purchase quantity;
- integer quantity overflow;
- purchase from a closed event;
- repeated close;
- non-finite or invalid financial calculation;
- invalid save or restore path and incompatible or invalid saved state.

The exact XML field-by-field matrix remains part of the pending XML checkpoint and must be reconciled with both the XSD and handout rather than invented.

## Approved financial and history model

### D-016: Commission policy

Decision:

- `CommissionMode` is an enum with `ON_PURCHASE` and `ON_CLOSE`.
- `CommissionPolicy` is immutable and stores the mode and integer percentage.
- On-purchase commission is added to the base LMSR purchase cost and credited to the event account.
- On-close commission is deducted from the gross payout of winning shares and retained by the event account.
- The LMSR calculator never calculates commission.

### D-017: Event account

Decision: each `MarketEvent` has its own `EventAccount`.

The account owns:

- the initial LMSR subsidy;
- current account balance;
- total commission collected.

There is no global market account for all events. Event-specific ownership keeps one event's subsidy, purchases, fees, and payouts internally consistent.

Closing an event does not reset this account to zero. After final commission and payout accounting, the remaining balance is preserved and remains visible in closed-event details. A positive balance represents market-maker profit, a negative balance represents the amount the market maker had to subsidize, and zero is break-even.

### D-018: Purchase history

Decision:

- A successful purchase creates an engine-owned history record.
- It records the option number and label, quantity, base share cost, purchase commission, and total paid.
- Internal records are stored chronologically.
- Callers receive newly created immutable `TradeHistoryEntry` DTO values in reverse chronological order when the UI needs newest-first display.
- Failed purchases never create history entries.

### D-019: Purchase and close transaction order

Purchase:

1. Validate event, status, option, quantity, and overflow.
2. Ask `LmsrCalculator` for the base cost and resulting prices without mutating state.
3. Apply the event's commission policy.
4. Verify all calculated values.
5. Credit the base cost to the event account. When the mode is `ON_PURCHASE`, also credit the purchase commission and increase total commission collected.
6. Commit option quantity, account state, commission total, and history together.
7. Return an immutable `PurchaseReceipt`.

Close:

1. Validate event, open status, and winning option.
2. Calculate gross payout as winning share quantity multiplied by `1.0`.
3. When the mode is `ON_CLOSE`, calculate closing commission from the gross payout, retain it in the event account, and increase total commission collected.
4. Verify the complete settlement.
5. Subtract the aggregate net winner payout from the event account and commit account state, commission, winner, and `CLOSED` status together.
6. Preserve the resulting account balance without resetting it, even when it is negative.
7. Keep the event, final balance, and history available for inspection.

Exercise 1 has no user entities or personal balances. "Pay the winners" is therefore aggregate market accounting only: the engine subtracts the total payout represented by the winning shares from the event account. Personal winner accounts remain deferred to Exercise 2.

## Approved LMSR design

### D-020: Calculator responsibility

Decision: `LmsrCalculator` is a stateless final class inside the engine.

- It performs base LMSR mathematics only.
- It does not own or mutate events, options, accounts, commission policies, or history.
- It uses full `double` precision and verifies finite results.

### D-021: LMSR formulas

For two option quantities `q1` and `q2` and liquidity parameter `b`:

```text
C(q1, q2) = b * ln(exp(q1 / b) + exp(q2 / b))

price1 = exp(q1 / b) / (exp(q1 / b) + exp(q2 / b))
price2 = exp(q2 / b) / (exp(q1 / b) + exp(q2 / b))

purchaseCost = C(after) - C(before)
initialSubsidy = C(0, 0) = b * ln(2)
```

One winning share pays 1 money unit when the event closes.

### D-022: Numerically stable implementation

Decision: calculate costs with log-sum-exp and prices with subtract-the-maximum softmax.

```text
x1 = q1 / b
x2 = q2 / b
m = max(x1, x2)

e1 = exp(x1 - m)
e2 = exp(x2 - m)

stableCost = b * (m + ln(e1 + e2))
stablePrice1 = e1 / (e1 + e2)
stablePrice2 = e2 / (e1 + e2)
```

Motivation: this is algebraically equivalent to the handout formula but prevents avoidable overflow such as `exp(100000)`, which would otherwise produce `Infinity` and potentially `NaN`.

### D-023: Approved LMSR worked example

For `b = 100`, initial quantities `0` and `0`, and a purchase of 100 option-1 shares:

```text
C(before) = 69.3147180559945
C(after) = 131.326168751822
base purchase cost = 62.0114506958278
option-1 price after = 0.731058578630005
option-2 price after = 0.268941421369995
```

With 5 percent on-purchase commission:

```text
commission = 3.10057253479139
total paid = 65.1120232306191
event account after purchase = 134.426741286614
```

The UI may display `$62.01`, `$3.10`, `$65.11`, `0.73`, and `0.27`, but the engine keeps the full values.

### D-024: LMSR invariants and acceptance examples

The design requires tests or independently checked examples for all of these properties:

- initial prices are approximately `0.5` and `0.5`;
- both option prices sum to approximately `1.0`;
- buying one option raises its price and lowers the other option's price;
- purchase cost equals `C(after) - C(before)` and is finite and positive for a positive purchase;
- a larger `b` produces a smaller price movement for the same purchase;
- initial subsidy equals `b * ln(2)`;
- the approved `b = 100`, 100-share example matches within tolerance;
- very large valid quantities do not produce `NaN` or infinity;
- rejected calculations change no event state;
- Exercise 1 provides no selling operation.

## Approved XML/JAXB boundary

### D-025: Generated package, loader, and mapper

Decision:

- Use JAXB/XJC for the Exercise 1 XML boundary.
- XJC-generated classes use package `guessmarket.engine.xml.generated`.
- Handwritten XML integration classes use package `guessmarket.engine.xml`.
- `XmlMarketLoader` owns path and readability checks, schema validation, JAXB unmarshalling, resource handling, and conversion of low-level file, schema, and JAXB failures into the engine failure boundary.
- `JaxbMarketMapper` reads generated JAXB objects, validates application-level data, and constructs a completely new candidate collection of application-owned `MarketEvent` objects.
- `GuessMarketEngineImpl` asks this boundary for a fully validated candidate collection and replaces its live collection once, only after the complete load succeeds.
- Generated classes remain engine-private XML integration types. They are not domain objects, DTOs, or public engine return values.
- Generated source is never edited manually and is regenerated only when the XSD changes.

The approved course-aligned generation process is:

1. Keep `xjc-run.bat` in the root of the intact JAXB distribution beside its `mod` directory.
2. Place a temporary working copy of `GM-EX1-Schema.xsd` beside `xjc-run.bat`.
3. From that directory, run:

```text
xjc-run.bat -p guessmarket.engine.xml.generated GM-EX1-Schema.xsd
```

4. XJC creates the package directory `guessmarket\engine\xml\generated` beside the wrapper.
5. Copy that complete package tree into the future Engine module's approved Java source root so the directory structure matches the package declaration.
6. Keep the canonical candidate XSD unchanged. The temporary XSD copy and generated staging tree are development artifacts, not replacements for the supplied evidence.

D-055 through D-057 later approved the Engine module, production source root, generated package topology, exact retained-source path, and staging cleanup rule. XJC generation itself remains an implementation action and has not been run for the project.

Motivation: the course guide explicitly teaches the package-first generation and copy workflow, and the supplied wrapper depends on its position relative to the `mod` libraries. Separating `XmlMarketLoader` from `JaxbMarketMapper` keeps file and JAXB mechanics apart from domain conversion and business validation. This makes mapping independently testable and prevents the generated schema representation from becoming the application's domain model.

### D-026: Course-supplied JAXB dependency placement

Decision:

- Keep the complete course-supplied `jaxb-ri-4.0.5` distribution unchanged under `tools`, including all seven `mod` JARs, scripts, documentation, and samples.
- Create one IntelliJ JAXB runtime library from these five supplied JARs:

```text
jakarta.activation-api.jar
angus-activation.jar
jakarta.xml.bind-api.jar
jaxb-core.jar
jaxb-impl.jar
```

- Associate that runtime library only with the Engine module and verify it under the Engine module's Dependencies tab.
- UI and DTO have no direct JAXB library dependency because their source code neither imports JAXB nor exposes generated JAXB types.
- `jaxb-xjc.jar` remains a development-time compiler tool used through the supplied `xjc-run.bat` workflow.
- `jaxb-jxc.jar` remains part of the intact supplied development distribution but is not needed for Exercise 1, which generates Java classes from an XSD rather than generating an XSD from Java classes.
- The final launcher must place the five runtime JARs on the application process classpath because the Engine executes in the same process as the UI.
- The third-party JAXB JARs remain separate files and are not merged into a fat application JAR.
- D-057 later approved direct development references to these five JARs in the intact `tools` distribution. Exact IntelliJ library naming, the physical dependency folder in the final package, manifest or launcher syntax, and submission placement remain pending for the build and packaging checkpoints.

Motivation: this uses Aviad's exact supplied third-party distribution and generic relevant-module workflow while preserving the approved architecture. The distribution's own release documentation explicitly classifies the five runtime JARs and two compiler JARs. An isolated Java 25 audit used the exact course wrapper to generate the approved package, compiled the generated classes with only the five runtime JARs, and successfully unmarshalled `multiple.xml`. Compiler tools therefore do not need to leak into the running application's classpath.

### D-027: Trusted schema, validation order, and field matrix

Decision:

1. `XmlMarketLoader` validates the input path before opening the file.
2. The loader unmarshals with schema validation enabled against the application-controlled `GM-EX1-Schema.xsd` copied from `modules/guessmarket-engine/src/main/resources/guessmarket/engine/xml`. It does not trust or depend on the input file's `xsi:noNamespaceSchemaLocation` value.
3. Only after JAXB and XSD validation succeeds does `JaxbMarketMapper` reconstruct values, apply the Exercise 1 business rules, and build a fresh `LinkedHashMap<Integer, MarketEvent>` candidate in XML event order.
4. `GuessMarketEngineImpl` replaces the live event collection once, only after the complete candidate succeeds. Any path, I/O, XML, schema, JAXB, mapping, or business-validation failure discards the candidate and preserves the previous live system.

The authoritative validation order is:

```text
path -> trusted XSD and JAXB -> business mapping -> complete ordered candidate -> one atomic replacement
```

Path checks:

- The supplied `Path` is non-null.
- It exists, is a regular file, and is readable.
- Its filename ends with `.xml`, using a case-insensitive extension check.

Field and collection matrix:

| XML value or structure              | XSD responsibility                                                | Exercise 1 business validation and mapping                                                                                                                                     |
| ----------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Guess-Market` root and `GM-events` | Required structure and at least one `GM-event`                    | Preserve the event order from the XML.                                                                                                                                         |
| `GM-event/@name`                    | Required `xs:list` of strings, generated by XJC as `List<String>` | Join tokens with one space, trim outer whitespace, require a nonblank result, and preserve case.                                                                               |
| `id`                                | Required `xs:int`                                                 | Require a positive value and uniqueness across the complete candidate, tracked with a set while mapping.                                                                       |
| `description`                       | Required `xs:string`                                              | Trim outer whitespace, require a nonblank result, and preserve internal whitespace and case.                                                                                   |
| `comision` text                     | Required `xs:int`                                                 | Require an integer from `0` through `90`, inclusive, and map the schema's supplied spelling `comision` to the correctly named domain `CommissionPolicy`.                       |
| `comision/@type`                    | Required exact enumeration: `on-close` or `on-purchase`           | Map the case-sensitive schema value to the matching domain commission timing.                                                                                                  |
| `GM-options`                        | XSD permits one or two `GM-option` elements                       | Require exactly two options for Exercise 1 and preserve their order.                                                                                                           |
| Each `GM-option`                    | Required `xs:string`                                              | Trim outer whitespace, require a nonblank result, and preserve internal whitespace and case.                                                                                   |
| Two mapped option labels            | Not covered by the XSD                                            | Reject an exact duplicate after trimming. Comparison is case-sensitive, so `Yes` and `Yes` are invalid while `Yes` and `yes` remain distinct under the handout's default rule. |
| `GM-method` and `GM-LMSR`           | Required LMSR structure                                           | Accept only the supplied Exercise 1 LMSR representation and create the LMSR domain configuration.                                                                              |
| `b`                                 | Required `xs:int`                                                 | Require a positive value.                                                                                                                                                      |

The mapper does not invent additional restrictions that are absent from the current contract. In particular, it does not require unique event names, add arbitrary maximum text lengths, normalize text to upper or lower case, reject meaningful internal spaces, or validate Exercise 2 structures.

Motivation: XSD validation proves that the document has the expected XML shape, but it cannot prove all application rules. The supplied `error-2.xml` is schema-valid despite a duplicate ID, and `error-3.xml` is schema-valid despite commission `115`. Using the Engine's trusted XSD prevents an input hint from selecting the application's validation rules. Reconstructing and validating every value before one replacement turns untrusted XML into a complete trusted candidate and makes the atomic recovery promise enforceable.

### D-028: Structured XML load-failure mapping

Decision: all expected XML-load failures cross the public Engine boundary as the already approved checked `EngineOperationException`. The UI catches this one exception type and uses structured fields rather than parsing `getMessage()` or understanding JAXB and I/O classes.

The XML-related `EngineErrorCode` values are:

| Code                         | Meaning                                                                                                                                                                                           |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `INVALID_XML_PATH`           | The supplied path is null, names a directory instead of a regular file, or does not have the required case-insensitive `.xml` filename extension.                                                 |
| `XML_FILE_NOT_FOUND`         | The selected file does not exist.                                                                                                                                                                 |
| `XML_FILE_ACCESS_FAILED`     | The file exists but cannot be read or opened, or an I/O failure occurs while reading it.                                                                                                          |
| `XML_STRUCTURE_INVALID`      | The XML is malformed, has the wrong root or structure, omits a schema-required value, or otherwise violates the trusted XSD.                                                                      |
| `XML_DATA_INVALID`           | Structurally valid XML violates an Exercise 1 application rule during mapping, such as a duplicate ID, invalid commission, blank text, duplicate option label, or non-positive `b`.               |
| `ENGINE_CONFIGURATION_ERROR` | The Engine's trusted XSD resource or JAXB configuration is missing, unreadable, or internally broken. This identifies an application packaging or configuration problem rather than bad user XML. |

This is deliberately a small category set. Individual business rules do not each receive a separate enum value. Their exact meaning is expressed through structured context carried by `EngineOperationException`.

Every `EngineOperationException` carries:

- `EngineErrorCode code`;
- a clear detail explaining what failed and why;
- a recovery hint that the UI can present or use when constructing its own message.

When meaningful and available at the failure point, it also carries:

- the input `Path`;
- the one-based XML event number;
- the parsed event ID;
- the XML or application field name;
- the one-based option number;
- XML line and column numbers for structural parsing or schema failures.

The exact Java representation of absent optional context and the final getter names remain part of the package and class-detail checkpoint. The semantic fields above are fixed.

Failure translation rules:

- Expected file, XML, schema, JAXB, and application-validation failures are translated at the Engine XML boundary into the matching structured engine code.
- Low-level I/O, schema, or JAXB exceptions may be retained as the Java cause for developer diagnosis, but they never become the public exception type and the normal UI never prints their raw message or stack trace.
- Business-validation failures normally have no low-level cause. They report the rule and the most specific event, option, field, and value location that is safely available.
- The UI switches on `EngineErrorCode` and reads context fields. It never infers the failure category by parsing `EngineOperationException.getMessage()`.
- Code must not broadly catch every `Exception` or every `RuntimeException` around loading. Unexpected programming defects remain visible during testing instead of being mislabeled as invalid user input.
- Every rejected load returns control to the UI and preserves the previous valid system, as required by D-013 and D-027.

The enum may later gain non-XML codes for event operations, purchases, closing, saving, and restoring. Those later additions do not change these six XML categories.

Motivation: the XML layer knows the technical cause, while the UI knows how to communicate with the user. A small stable code set lets the UI distinguish choosing another file, correcting XML structure, correcting one business value, and repairing an application installation without depending on JAXB implementation text. Structured location data keeps messages precise, and retained low-level causes preserve diagnostic evidence without leaking technical details into normal console interaction.

### D-029: Candidate construction, initialization, ownership, and commit

Decision: the mapper creates the same ordered map shape that the Engine uses. No extra `LoadedMarketCandidate` wrapper and no intermediate event list are introduced because Exercise 1 XML defines no separate global market metadata.

Initial Engine state:

- `GuessMarketEngineImpl` stores events through a private `Map<Integer, MarketEvent>` field whose concrete initial value is an empty `LinkedHashMap`.
- An empty event map means no valid system is loaded. This is unambiguous because the trusted XSD requires at least one event and Exercise 1 never removes individual events from a loaded system.
- No separate `systemLoaded` boolean is stored, avoiding two pieces of state that could disagree.
- The map field remains private and reassignable so a successful load can replace the complete reference once.

Private construction flow:

```text
GuessMarketEngineImpl.loadEventsFromXml(path)
    -> XmlMarketLoader.load(path)
        -> validate path
        -> unmarshal with trusted XSD
        -> JaxbMarketMapper.map(generatedRoot)
            -> build complete LinkedHashMap<Integer, MarketEvent>
    -> assign the candidate map to the Engine field once
    -> return the loaded event count
```

- JAXB-generated types remain inside `guessmarket.engine.xml` and its `generated` subpackage. `GuessMarketEngineImpl` receives only application-owned domain objects.
- `JaxbMarketMapper` inserts events into a fresh `LinkedHashMap<Integer, MarketEvent>` in XML order while applying D-027.
- The mapper and loader do not cache the candidate, retain its reference after returning, expose it through the public Engine API, or mutate live Engine state.
- After a successful return, `GuessMarketEngineImpl` becomes the sole owner of the candidate map. A defensive copy is unnecessary because the creating XML components relinquish it and the map never crosses the private Engine boundary.

Every mapped `MarketEvent` begins with this complete runtime state:

- validated ID, reconstructed name, description, commission policy, two ordered options, and positive LMSR `b`;
- `EventStatus.OPEN`;
- no winning option selected;
- both option quantities equal to `0`;
- empty purchase history;
- initial subsidy calculated by the authoritative `LmsrCalculator` as `b * ln(2)`;
- `EventAccount` initial subsidy and current balance both equal to that calculated subsidy;
- total commission collected equal to `0`.

Initial option prices are derived as approximately `0.5` and `0.5`; they are not stored. The mapper asks the authoritative calculator for the initial subsidy instead of duplicating the LMSR formula. The precise Java representation of the absent winner remains a later class-detail choice, but the semantic state is fixed.

Successful commit is conceptually:

```java
events = candidate;
return candidate.size();
```

The Engine does not call `events.clear()` followed by `events.putAll(candidate)`. That would mutate the live collection in multiple steps. One private reference replacement makes the approval boundary observable: everything before the assignment is preparation, and the assignment is the commit.

Failure behavior:

- If candidate construction fails at any event, the incomplete local candidate is discarded and the existing Engine map reference remains unchanged.
- An invalid first load leaves the Engine with no loaded system.
- An invalid reload preserves every previous event, quantity, history entry, account balance, commission total, winner, and status.
- A successful reload completely replaces the earlier events and runtime state; it does not merge candidates with old state.

Motivation: this keeps one owner for XML business validation and one owner for live state. The candidate is a quarantined future system, not a partially active system. Returning the Engine's actual private storage shape avoids redundant conversion, while sole ownership and one reference assignment provide the simplest student-appropriate implementation of the already approved atomic-load rule.

### D-030: Fixture provenance and local working locations

Provenance decision:

- Nitzan confirmed on 2026-08-12 that the five clean files in the temporary `new_raw` folder were direct re-downloads from the same official course source as the earlier damaged captures.
- The original files directly under `provided/assignment-1/test-files` had been captured incorrectly and contained extracted text rather than usable XML or XSD markup.
- The five clean re-downloads replaced those damaged root files byte-for-byte. Their hashes match the values already recorded below.
- The now-redundant `new_raw` copies were removed after every root copy matched its clean-source SHA-256. Both the earlier damaged versions and clean re-downloads remain recoverable through Git history.
- The canonical local supplied set is now the five clean files directly under `provided/assignment-1/test-files`. They are treated as immutable course-owned evidence and are never edited to create test cases.
- No Drive file was read, changed, or synchronized during this canonicalization.

Approved local roles:

| Role                           | Source and treatment                                                                                                                                                                                                                          |
| ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Canonical supplied XSD and XML | The five immutable files directly under `provided/assignment-1/test-files`. Their bytes and hashes are the local authority for Exercise 1 XML work.                                                                                           |
  | Trusted runtime XSD            | During implementation, copy the canonical `GM-EX1-Schema.xsd` to `modules/guessmarket-engine/src/main/resources/guessmarket/engine/xml/GM-EX1-Schema.xsd` and verify its hash against the canonical copy.                                       |
| XJC generation input           | Use a temporary copy of the canonical XSD beside the intact course `xjc-run.bat`, as approved in D-025. The staging copy is not a new source of truth and may be discarded after generation.                                                  |
  | Supplied test fixtures         | Copy the four supplied XML files beneath `modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/fixtures/supplied`. Preserve their filenames and hashes. Tests must not mutate the canonical `provided` copies.                 |
  | Custom test fixtures           | Author separately named files beneath the sibling `fixtures/custom` directory. Never alter a supplied fixture to manufacture another scenario.                                                                                               |

D-057 later fixed the runtime resource, test-resource, generated-source, and development dependency paths. Their roles and source authority remain as defined here.

Motivation: provenance answers where a file came from, while working location answers what a copy is allowed to do. Keeping one immutable supplied set prevents accidental edits from changing the assignment evidence. Runtime, generation, and test copies serve different technical purposes and can be regenerated or replaced from that canonical source without ambiguity.

### D-031: Stable command menu and Engine-owned loaded state

Decision:

- The console always displays one stable, one-based eight-command menu:
  1. Load system from XML.
  2. Display all events.
  3. Display one event's trading status.
  4. Purchase shares.
  5. Close an event.
  6. Save current system state.
  7. Restore saved system state.
  8. Exit.
- Commands 1, 7, and 8 are usable without a currently loaded system.
- Commands 2 through 6 require a loaded system. The menu may label them statically with `[requires a loaded system]`, but it does not hide or renumber them.
- Load and restore remain available after a system is loaded because either operation may atomically replace the current system under the previously approved load and restore rules.
- The Engine is the sole authority on whether a system is loaded. The console UI does not maintain a duplicate `systemLoaded` boolean, and no `hasLoadedSystem()` capability is added merely to control menu presentation.
- When the user selects a state-dependent command before a system is loaded, the UI follows the normal Engine call path. The Engine reports structured error code `NO_SYSTEM_LOADED`; the UI explains that the user must load XML or restore a saved system and then returns to the unchanged main menu.
- `Exit` remains a UI-only command and does not become an Engine capability.

Alternatives not selected:

- Hiding unavailable commands was rejected because it obscures the complete command set, destabilizes menu presentation, and avoids rather than implements the handout's required invalid-command-order recovery.
- Keeping the stable menu while letting the UI block commands through its own loaded-state boolean was rejected because that duplicates Engine state and can drift after a failed load or restore. The Engine must enforce the rule regardless of any UI precheck.

Motivation: menu visibility describes the application's capabilities, while operation validity is an Engine rule. A stable menu is easier to learn, script, and test. Keeping loaded-state authority inside the Engine gives the console and later JavaFX UI the same behavior, prevents duplicated state, and preserves the approved thin-UI boundary: numeric choice to typed Engine capability to DTO result or structured error to English output.

### D-032: Main-loop recovery boundary

Decision: use layered central recovery so normal user mistakes remain recoverable without hiding programming defects.

Main-loop behavior:

- The UI displays the menu, reads one complete line, parses a menu choice, dispatches at most one command, displays its result or expected failure, and then displays a fresh main menu.
- Blank, non-numeric, and out-of-range main-menu input produces a clear UI-owned message and returns directly to a fresh main menu.
- Selecting command 8 exits normally and immediately. Exercise 1 adds no confirmation prompt and performs no automatic save on exit.
- If the input stream ends, the UI prints a short `Input closed. Exiting.` message and exits normally because further recovery is impossible without more input.
- A recoverable `EngineOperationException` ends only the current command. The UI translates its structured code and context into an English message, does not print a stack trace, and returns to a fresh main menu.
- An unexpected `RuntimeException`, `Error`, or other programming defect is not caught by the ordinary user-recovery boundary. It remains visible and may terminate the application so testing does not continue through an unknown or corrupted condition.
- D-032 establishes that invalid input inside a secondary command prompt cannot terminate the application. Whether such input repeats the current question or abandons the command is decided with the reusable prompt rules in D-033.

Alternatives not selected:

- Letting every command independently catch all of its parsing and Engine failures was rejected because it duplicates control flow and makes recovery inconsistent across commands.
- Wrapping the complete loop in a broad `catch (Exception)` or `catch (RuntimeException)` was rejected because it can mislabel a programming defect as a user mistake and conceal failures that tests must expose.

Motivation: malformed text, a valid request rejected by the Engine, a closed input stream, and a programming defect are different conditions with different owners. The UI explains text-shape problems, structured Engine failures explain rejected operations through UI wording, end-of-input terminates cleanly, and unexpected defects remain diagnosable. This satisfies the handout's continue-after-invalid-order rule without creating a loop that silently swallows real bugs.

### D-033: Reusable full-line input and one-based selection rules

Decision: all console commands share one consistent full-line input model and distinguish UI-shape mistakes from understood operations rejected by the Engine.

Input ownership and parsing:

- The application owns one `Scanner` and reads console input exclusively as complete lines with `nextLine()`. It never mixes token methods such as `nextInt()` with line input.
- The UI trims outer whitespace where appropriate while preserving meaningful internal spaces, spelling, and capitalization in paths and labels, consistent with D-014.
- Expected user-parsing failures are caught specifically. Examples include `NumberFormatException` for invalid or overflowing integers and `InvalidPathException` for a path string that cannot form a `Path`. The UI does not broadly catch `RuntimeException`.
- Blank input, non-numeric input, overflowing integers, and out-of-range numeric selections produce a clear UI-owned message and repeat the current secondary prompt.
- A purchase quantity must parse as an `int` and be positive before the UI calls the Engine. The Engine repeats authoritative quantity and overflow validation.
- End-of-input at any prompt follows D-032 and exits normally.
- An `EngineOperationException` means the request was understood but rejected. It abandons the current command, produces an appropriate UI message, and returns to the main menu rather than repeating a partially completed prompt sequence.

Selection translation:

- Every displayed selectable list is numbered from 1 through its current size.
- For an event list, the entered number is a displayed position, not an event ID. The UI validates the range, indexes the current DTO list at `selection - 1`, reads that DTO's actual XML event ID, and sends the actual ID to the Engine.
- The UI never assumes that event IDs are contiguous or equal to displayed positions.
- Event options retain XML order and are displayed as option 1 and option 2. Their one-based selection number is the same `optionNumber` passed to the Engine. Users do not select an option by typing its description.

Path prompts:

- A path is read as one complete line so internal spaces require no token splitting or quotation syntax.
- A blank or syntactically invalid path repeats the current path prompt.
- Once a `Path` is constructed, existence, readability, XML or saved-state validity, and other authoritative file rules belong to the Engine. An Engine rejection ends the command and returns to the main menu.

Mid-command cancellation:

- Exercise 1 reserves no `0`, `back`, or `cancel` input. Commands are short, and malformed UI input can be corrected by repeating the current prompt.
- There is therefore no separate cancellation branch to explain, implement, or test. Explicit exit remains main-menu command 8, and end-of-input remains the external graceful-exit path.

Alternatives not selected:

- Abandoning a complete command after every malformed secondary input was rejected because it forces the user to repeat already valid selections for a simple typing mistake.
- Adding a universal `0`, `back`, or textual `cancel` action was rejected as unnecessary Exercise 1 grammar that complicates every prompt and weakens the otherwise consistent one-based selection model.

Motivation: one input model prevents the common Java newline bug caused by mixing token and line reads. Prompt-local correction is friendly for text-shape mistakes, while returning to the main menu after an Engine rejection gives commands one clear transactional boundary. Explicit position-to-ID translation prevents a displayed serial number from being mistaken for a possibly non-contiguous XML event ID.

### D-034: Per-command event collections and ordering

Decision:

| Command                              | Events displayed            | Events selectable            |
| ------------------------------------ | --------------------------- | ---------------------------- |
| Display all events                   | Every loaded event          | None; the command is display-only |
| Display one event's trading status   | Every loaded event          | Open or closed events        |
| Purchase shares                      | Open events only            | Open events only             |
| Close an event                       | Open events only            | Open events only             |

Collection ownership and ordering:

- The UI calls the approved `listEvents()` capability and receives immutable event summaries. No presentation-specific Engine capabilities such as `listOpenEventsForPurchase()` or `listOpenEventsForClosing()` are added.
- For purchase and close presentation, the UI filters summaries whose public status is `OPEN`. The Engine independently repeats authoritative lifecycle validation when the operation is attempted.
- Every full or filtered event collection preserves original XML/load order. Events are not reordered by ID, name, status, or recent use.
- Each displayed collection receives fresh one-based serial numbers and follows D-033 when translating a selected position into the summary's actual event ID.

Empty-collection behavior:

- A valid loaded system always retains its events, including closed events, so display-all and detailed-status remain available after every event closes.
- If the open-event subset is empty, purchase prints `No open events are available for purchasing shares.` and close prints `No open events are available to close.` Each command returns normally to the main menu without asking for an event number or calling a mutating Engine operation.
- An empty open-event subset is a valid application state, not an `EngineOperationException`.
- No loaded system is a different condition: the initial `listEvents()` call reports `NO_SYSTEM_LOADED` under D-031.

Alternatives not selected:

- Adding separate Engine list methods for each console use case was rejected because the approved summary list already contains the status needed for presentation filtering, and the extra capabilities would encode console-specific concerns in the Engine contract.
- Showing closed events in purchase or close and waiting for the Engine to reject them was rejected because the handout explicitly requires those selection lists to contain active events only.
- Sorting filtered lists independently was rejected because stable load order makes serial selections predictable and already follows the approved `LinkedHashMap` ownership model.

Motivation: presentation eligibility and authoritative operation validity are related but different. The UI should show the user the handout-required choices, while the Engine must still defend its lifecycle rules. Preserving one order across full and filtered views makes lists understandable, and distinguishing no loaded system from a loaded system with no open events produces accurate recovery guidance.

### D-035: Load, event overview, and detailed-status command flows

Decision: commands 1 through 3 use the following focused conversations. Exact event and financial field layout remains for the output-model checkpoint.

Command 1, load system from XML:

```text
select command 1
-> prompt for the complete XML path
-> construct Path under D-033
-> call loadEventsFromXml(path)
-> report success and returned event count
-> return to main menu
```

- Exact path prompt: `Enter the full path to the XML file:`
- Success output consists of `System loaded successfully.` followed by `Loaded events: N`.
- A successful load atomically replaces the current system. There is no additional replacement-confirmation prompt.
- A failed load follows the approved structured-error flow and preserves the previous valid system.
- Loading does not automatically print the loaded events. Command 2 owns that distinct operation.

Command 2, display all events:

```text
select command 2
-> call listEvents()
-> print Events:
-> render every MarketEventSummary in load order
-> return to main menu
```

- The command has no secondary input.
- It uses the summary DTO collection directly and does not call `getEventDetails()` separately for every event.
- No loaded system follows `NO_SYSTEM_LOADED`; a valid loaded system contains at least one event.

Command 3, display one event's trading status:

```text
select command 3
-> call listEvents()
-> render the same numbered summaries used by command 2
-> prompt for one displayed event number
-> translate the displayed position to the actual event ID under D-033
-> call getEventDetails(actualEventId)
-> render the complete MarketEventDetails
-> return to main menu
```

- Exact selection prompt: `Select an event by number:`
- Both open and closed events are selectable under D-034.
- The selection list reuses the command-2 summary renderer so the same event is not described differently between overview and selection screens.
- Any `EngineOperationException` ends the command and returns to the main menu under D-032.

Alternatives not selected:

- Automatically displaying every event after a successful load was rejected because load and display are separate handout commands and the extra output would duplicate command 2.
- Asking the user for an actual event ID was rejected because the handout requires selection from a one-based displayed list.
- Calling `getEventDetails()` for every event in command 2 was rejected because the compact summary contract already owns event-list information and full details belong to command 3.

Motivation: the three commands form a clear progression without duplicated responsibility: load establishes valid system state, display-all provides an overview, and detailed status inspects one immutable snapshot. Reusing the same summary rendering and explicit position-to-ID translation keeps the conversation consistent while retaining typed Engine capabilities.

### D-036: Purchase command conversation

Decision:

```text
select command 4
-> call listEvents()
-> filter OPEN summaries under D-034
-> if none exist, print the approved empty-open-set message and return
-> render numbered open-event summaries
-> prompt for an event and translate its position to the actual ID
-> call getEventDetails(actualEventId)
-> render the current pre-purchase event status
-> prompt for option number 1 or 2
-> prompt for a positive share quantity
-> call purchaseShares(eventId, optionNumber, quantity)
-> receive one atomic PurchaseReceipt
-> render the purchase summary
-> render the resulting MarketEventDetails carried by the receipt
-> return to main menu
```

Exact prompts:

- `Select an event by number:`
- `Select an option by number:`
- `Enter the number of shares to purchase:`

Result and consistency rules:

- The detailed pre-purchase status numbers the two options so the user can connect the selection to the visible XML-provided labels, current prices, and quantities.
- Successful output begins with `Purchase completed successfully.`, then shows a purchase summary containing base share cost, purchase commission when relevant, and total amount paid, followed by an `Updated event status:` rendering.
- The updated status comes from the `MarketEventDetails` already carried by the approved `PurchaseReceipt`. The UI does not call `getEventDetails()` again after the purchase.
- UI-shape mistakes repeat the current prompt under D-033. The Engine repeats authoritative event, status, option, quantity, overflow, calculation, and atomicity validation.
- An `EngineOperationException` commits no quantity, account, commission, or history change and returns the UI to the main menu under D-032.
- A successful purchase creates exactly one history entry as part of the same Engine commit.

No advance quote or confirmation:

- The console does not add a separate price-quote capability or confirmation stage before purchase. The approved atomic purchase operation calculates and returns the exact completed cost.
- A separate quote would expand the Engine contract and could become stale in later multi-client exercises. It is not required for Exercise 1.

Alternatives not selected:

- Querying `getEventDetails()` again after purchase was rejected because the receipt already contains the operation's coherent resulting snapshot. A later query is redundant now and could observe a newer state in a future concurrent system.
- Adding an advance quote and confirmation was rejected because it introduces an unrequired capability and another interaction grammar without improving the correctness of the atomic purchase.
- Showing closed events and relying only on Engine rejection was rejected by D-034 because the handout requires active-event choices.

Motivation: the pre-purchase snapshot lets the user understand the event before choosing, while the receipt proves exactly what the one atomic operation charged and committed. Returning the resulting details inside that same receipt keeps the transaction summary and displayed post-purchase state internally consistent.

### D-037: Close-event command conversation

Decision:

```text
select command 5
-> call listEvents()
-> filter OPEN summaries under D-034
-> if none exist, print the approved empty-open-set message and return
-> render numbered open-event summaries
-> prompt for an event and translate its position to the actual ID
-> call getEventDetails(actualEventId)
-> render the current pre-close event status
-> prompt for winning option number 1 or 2
-> call closeEvent(eventId, winningOptionNumber)
-> receive the atomic resulting MarketEventDetails
-> report successful closure
-> render the final event status from the returned details
-> return to main menu
```

Exact prompts and result heading:

- `Select an event by number:`
- `Select the winning option by number:`
- Successful output begins with `Event closed successfully.` followed by `Final event status:` and the returned details.

Winner-selection semantics:

- The selected number declares which of the chosen event's two XML-ordered outcomes actually won. It is not another purchase or a choice of which participant receives money.
- Option 1 means the first XML option won, and option 2 means the second XML option won. The pre-close details display both numbered labels and quantities before the prompt.
- All shares previously purchased in the declared winning option participate in the aggregate payout under D-018. Shares in the losing option pay zero.
- The Engine performs the approved close settlement atomically, records the winner, changes the status to `CLOSED`, preserves the final account balance and history, and prevents later purchase or closure.

No additional confirmation:

- Selecting the winning option is the final explicit action. The console does not add another confirmation prompt.
- This stays consistent with D-033, which reserves no `yes`, `no`, `back`, or `cancel` grammar, and avoids an unrequired interaction branch.

Result and failure rules:

- The UI renders the `MarketEventDetails` returned by `closeEvent()`. It does not query `getEventDetails()` again after closure.
- The resulting details show the event as `CLOSED` and include the winning option and final public state required by the output checkpoint.
- The Engine rechecks loaded state, event identity, open status, and winning option. A rejected close changes no winner, lifecycle, account, commission, or history state and returns the UI to the main menu.
- A successfully closed event remains visible through commands 2 and 3.

Alternatives not selected:

- Adding a separate confirmation after winner selection was rejected because the handout does not require it and it would introduce another input grammar after D-033.
- Adding a separate `CloseResult` remains rejected under D-011 because the resulting `MarketEventDetails` supplies the final public state required by this console flow.
- Querying details again after close was rejected because the operation already returns its coherent resulting snapshot.
- Showing closed events in the selection list was rejected by D-034 because they cannot be closed again.

Motivation: the pre-close snapshot makes the two possible outcomes explicit before the irreversible declaration. Passing the selected XML-ordered option number to one atomic Engine operation keeps winner selection, payout accounting, commission, final balance, and lifecycle transition together, while the returned details present exactly that committed result.

### D-038: Save, restore, and exit command conversations

Decision: commands 6 through 8 use the following console conversations. This decision covers UI behavior only. Section 9 remains responsible for the persistence format, extension, overwrite policy, atomic file writing, versioning, and compatibility validation.

Command 6, save current system state:

```text
select command 6
-> prompt for a complete base path and file name without an extension
-> construct Path under D-033
-> call saveState(path)
-> report successful save
-> return to main menu
```

- Exact prompt: `Enter the full path and file name to save, without an extension:`
- Exact success message: `System state saved successfully.`
- Save requires a loaded system and follows the normal Engine-authority path from D-031. The UI does not track or pre-block loaded state.
- The Engine owns the persistence operation. A save failure does not modify the running system and returns the UI to the main menu.
- `saveState` returns `void`; no result wrapper is added solely for a success message.

Command 7, restore saved system state:

```text
select command 7
-> prompt for the saved state's complete base path and file name without an extension
-> construct Path under D-033
-> call restoreState(path)
-> atomically replace live state only after complete validation
-> report successful restore and returned event count
-> return to main menu
```

- Exact prompt: `Enter the full path and file name to restore, without an extension:`
- Success output consists of `System state restored successfully.` followed by `Restored events: N`.
- Restore remains available with or without a currently loaded system and is distinct from loading original XML.
- There is no replacement-confirmation prompt.
- A successful restore replaces the complete system. A failed restore preserves the previous valid state.
- Restore does not automatically display events afterward; command 2 owns event listing.

Base-path and extension ownership:

- The UI passes the entered base `Path` to the Engine without appending or stripping an extension.
- The future Engine persistence layer owns the actual extension and all representation rules. Whether user input that already contains an extension is rejected, accepted, or normalized remains a Section 9 decision.

Command 8, exit:

```text
select command 8
-> print Goodbye.
-> terminate normally without an Engine call
```

- Exit has no confirmation and performs no automatic save.
- End-of-input remains a separate graceful termination path with `Input closed. Exiting.` under D-032.

Alternatives not selected:

- Combining XML load and saved-state restore was rejected because they use different trusted formats and validation paths.
- Letting the UI choose or append the persistence extension was rejected because storage representation belongs to the Engine and should not be repeated in future UIs.
- Automatically saving on exit was rejected because no approved destination exists and the application must not silently overwrite a file.
- Automatically displaying all events after restore was rejected because restore and display are separate commands.

Motivation: the console owns the path conversation, while the Engine owns what a saved state means. This keeps persistence mechanics out of the UI, preserves atomic replacement, and gives a future JavaFX caller the same save and restore capabilities without duplicating file-format knowledge.

### D-039: Compact event summary DTO and reusable list rendering

Decision: `MarketEventSummary` is a compact immutable event-list projection with these semantic fields:

```text
int eventId
String name
String description
int commissionPercentage
CommissionMode commissionMode
List<String> optionLabels
EventStatus status
```

DTO rules:

- `optionLabels` is an immutable ordered list containing exactly two XML-provided labels. List index 0 is displayed option 1 and index 1 is displayed option 2.
- The one-based serial position used by a particular console list is not stored in the DTO. The UI assigns it while rendering the current full or filtered collection.
- `CommissionMode` exposes `ON_PURCHASE` and `ON_CLOSE`, and `EventStatus` exposes `OPEN` and `CLOSED`, as typed public DTO-module enums.
- The summary excludes current prices, share quantities, event-account balance, collected commission, history, winner, and LMSR parameter `b`. Those values belong to the detailed projection or remain Engine-private.

Exact event-block rendering:

```text
1. Event ID: 17
   Name: World Cup Final
   Description: Argentina versus Spain
   Commission percentage: 5%
   Commission method: On purchase
   Options:
     1. Argentina
     2. Spain
   Status: OPEN
```

- Event blocks are separated by one blank line.
- Commands 2 and 3 use the heading `Events:`. Purchase and close use `Open events:`.
- The same renderer handles full and filtered summary collections.
- The UI maps `ON_PURCHASE` to `On purchase` and `ON_CLOSE` to `On close`. It displays statuses as `OPEN` or `CLOSED`.
- The Engine and DTO do not contain console headings, indentation, serial positions, or human-readable enum strings.

Alternatives not selected:

- A fixed-width console table was rejected because names and descriptions have variable lengths and would require fragile width assumptions or truncation.
- Including complete `MarketOptionSnapshot` values in each summary was rejected because prices and quantities are not needed by the overview and would make the compact list projection unnecessarily detailed.
- Using separate `optionOneLabel` and `optionTwoLabel` fields was rejected because an immutable ordered list follows the already approved two-option model without parallel fields.
- Returning preformatted event strings from the Engine was rejected because formatting belongs exclusively to the UI.

Motivation: the summary contains exactly the handout-required event-list information and nothing tied to one console layout. The immutable ordered labels preserve option identity, while labeled UI blocks remain readable for long content and reusable across overview and selection screens.

### D-040: Detailed event and option snapshots

Decision: `MarketOptionSnapshot` is an immutable public projection with these semantic fields:

```text
int optionNumber
String label
int shareQuantity
double currentPrice
```

- `optionNumber` is 1 or 2, and the immutable two-element option list retains XML order.
- `shareQuantity` is the total quantity purchased for that option.
- `currentPrice` is derived by the Engine from the event's complete current quantities and `b`. The DTO carries the full-precision result; the UI owns display rounding.

`MarketEventDetails` is a self-contained immutable projection with these semantic fields:

```text
int eventId
String name
String description
int commissionPercentage
CommissionMode commissionMode
EventStatus status
List<MarketOptionSnapshot> options
double eventAccountBalance
double totalCommissionCollected
List<TradeHistoryEntry> purchaseHistory
OptionalInt winningOptionNumber
```

Detailed DTO rules:

- The two option snapshots, purchase-history list, and all nested values are immutable. History is returned newest first.
- `winningOptionNumber` is `OptionalInt.empty()` for an open event and `OptionalInt.of(1)` or `OptionalInt.of(2)` for a closed event.
- `OptionalInt` is used instead of a zero sentinel, nullable `Integer`, or duplicated winning-option snapshot. Absence is explicit, option numbering remains strictly one-based, and the present number identifies one of the existing immutable snapshots.
- The details are self-contained rather than embedding `MarketEventSummary`, which would duplicate option labels between the summary and detailed snapshots.
- Details exclude mutable domain objects, preformatted strings, persistence information, initial subsidy, and LMSR parameter `b`. The current account balance is the handout-required public account state.

Detailed rendering shape:

```text
Event status:
Event ID: 17
Name: World Cup Final
Description: Argentina versus Spain
Commission percentage: 5%
Commission method: On purchase
Status: OPEN

Options:
  1. Argentina
     Current price: 0.73
     Shares purchased: 100
  2. Spain
     Current price: 0.27
     Shares purchased: 0

Event account balance: 134.43
Total commission collected: 3.10

Purchase history, newest first:
[history rows from D-041]
```

- For a closed event, the UI adds `Winning option: N - label`, using the present optional number and the corresponding option snapshot.
- For an open event, the winning-option line is omitted because no winner exists.
- If history is empty, the UI prints `Purchase history: No purchases have been made.`
- Exact numeric rendering remains for the numeric-format checkpoint.

Alternatives not selected:

- A zero winner sentinel was rejected because zero is not a valid one-based option and would encode absence implicitly.
- Nullable `Integer` was rejected because `OptionalInt` requires the caller to handle absence explicitly.
- A duplicated winning `MarketOptionSnapshot` was rejected because the option number already identifies one snapshot in the immutable list.
- Nesting `MarketEventSummary` inside details was rejected because its option-label list would duplicate the detailed option labels and complicate access without adding information.

Motivation: the detailed snapshot contains every value required to render current or final event state without exposing live Engine objects. Explicit option numbers and winner absence prevent positional ambiguity, while a self-contained immutable result gives command 3, purchase, close, and future JavaFX callers one coherent view model.

### D-041: Purchase receipt and retained transaction history

Decision: `PurchaseReceipt` is an immutable result of the just-completed purchase with these semantic fields:

```text
int optionNumber
int purchasedShareQuantity
double baseShareCost
double purchaseCommission
double totalPaid
MarketEventDetails resultingEventDetails
```

Receipt rules:

- `optionNumber` identifies the purchased option inside the resulting details, so the receipt does not duplicate the event ID, event name, or option label.
- `totalPaid` equals `baseShareCost + purchaseCommission`.
- For an `ON_CLOSE` event, purchase commission is `0.0` and total paid equals base share cost.
- The resulting details are the same-operation immutable snapshot approved in D-036, not a later query.

Purchase-summary rendering:

```text
Purchase completed successfully.

Purchase summary:
Option: 1 - Argentina
Shares purchased: 100
Base share cost: 62.01
Purchase commission: 3.10
Total paid: 65.11

Updated event status:
[resulting event details]
```

- The purchase-commission line appears for `ON_PURCHASE`, including a zero-percent policy, and is omitted for `ON_CLOSE` because no fee was charged during the purchase.
- The UI resolves the option label through `optionNumber` and the receipt's resulting option snapshots.

`TradeHistoryEntry` is an immutable record of one successful earlier purchase with these semantic fields, as already required by D-018:

```text
int optionNumber
String optionLabel
int shareQuantity
double baseShareCost
double purchaseCommission
double totalPaid
```

History rules:

- The historical label is retained with the option number so the entry remains a complete statement of the choice that was purchased.
- The entry does not contain a complete event snapshot, event ID, timestamp, user identity, account identity, or formatted output.
- A successful purchase creates exactly one internal record and one corresponding public entry when details are requested. A failed purchase creates none.
- Closing creates no purchase-history entry and does not erase existing entries.
- Save and restore must preserve the complete internal history; public details expose a newly created immutable newest-first list.

History rendering:

```text
Purchase history, newest first:

1. Option: 1 - Argentina
   Shares purchased: 100
   Base share cost: 62.01
   Purchase commission: 3.10
   Total paid: 65.11
```

- For an `ON_CLOSE` event, each history row omits the purchase-commission line because no commission was charged on those purchases.
- The list order and displayed row number communicate recency. Exercise 1 invents no timestamps or clock policy.

Alternatives not selected:

- Reusing `PurchaseReceipt` as the history type was rejected because its nested resulting event details would make every history row unnecessarily large.
- Retaining only option, quantity, and total paid was rejected because D-018 already preserves base cost and purchase commission, and exposing the breakdown makes the financial result auditable.
- Adding timestamps was rejected because the handout does not require them and they introduce clock and formatting concerns.
- Returning preformatted receipt or history strings was rejected because presentation belongs to the UI.

Motivation: the current receipt and retained history describe related but different scopes. The receipt includes the exact resulting event snapshot for one atomic response, while a history entry stores only the stable facts of one purchase. Both preserve the financial breakdown without coupling Engine data to console formatting.

### D-042: One presentation-only numeric format

Decision: the UI renders every financial `double` with exactly two digits after the decimal point using `Locale.US`, equivalent to `String.format(Locale.US, "%.2f", value)`.

Formatting scope:

- Current option prices, base share costs, purchase commission, total paid, event-account balances, total commission collected, and any later displayed closing financial values use the shared two-decimal formatter.
- IDs, serial positions, option numbers, quantities, commission percentages, event counts, and `b` remain ordinary `int` values without decimal digits or grouping separators.
- Commission percentages append `%`. Abstract money values use no currency symbol because the handout defines no real currency.
- `Locale.US` guarantees a decimal point independent of the machine locale.

Examples:

```text
0.5               -> 0.50
0.731058578630005 -> 0.73
3.10057253479139  -> 3.10
65.1120232306191  -> 65.11
-12.345           -> -12.35
0.0000001         -> 0.00
```

Edge rules:

- A rendered `-0.00` is normalized to `0.00` as a presentation-only cleanup. Other negative values retain their sign.
- `NaN` or either infinity must never cross the Engine boundary because calculations are validated before commit. If a non-finite value reaches formatting, it is a programming defect rather than normal user output.
- DTOs retain full-precision values. The Engine never rounds before updating quantities, balances, commission, later prices, closing settlement, history, or saved state.

Alternatives not selected:

- A variable `0.##` style was not selected. It would satisfy the maximum-two-digits wording, but fixed two-decimal output makes prices, costs, balances, receipts, and history easier to compare and matches the accepted `0.00` display for tiny values.
- Rounding inside the Engine was rejected because repeated purchases and closing would accumulate presentation error in authoritative state.
- Locale-dependent formatting was rejected because the same program must not print decimal commas on one machine and decimal points on another.

Motivation: financial calculations and human display serve different purposes. Full precision protects later calculations, while one fixed UI formatter produces stable, testable console output and satisfies the handout's two-decimal limit without leaking presentation decisions into the Engine or DTO module.

### D-043: Non-XML Engine error taxonomy

Decision: extend the six XML codes from D-028 with a small semantic set for market operations and saved-state persistence. Exact detail and context explain individual rule failures; the enum does not mirror every validation branch.

Market-operation codes:

| Code                           | Meaning                                                                                                                        |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| `NO_SYSTEM_LOADED`             | An operation requires a valid loaded or restored system.                                                                       |
| `EVENT_NOT_FOUND`              | The supplied actual event ID does not exist.                                                                                    |
| `EVENT_NOT_OPEN`               | Purchase or close was requested for an event that is already closed.                                                           |
| `INVALID_OPTION`               | The supplied purchase or winning-option number is not 1 or 2.                                                                  |
| `INVALID_QUANTITY`             | Purchase quantity is non-positive or cannot be added to the current quantity without `int` overflow.                          |
| `FINANCIAL_CALCULATION_FAILED` | A required cost, price, commission, payout, or balance calculation is non-finite or otherwise invalid.                        |

- Purchase from a closed event and repeated close both use `EVENT_NOT_OPEN`, with operation-specific detail.
- Non-positive quantity and quantity-addition overflow both use `INVALID_QUANTITY`, with detail and recovery guidance distinguishing them.
- Non-finite financial results use a separate category because they are not ordinary text or range parsing failures.

Saved-state codes:

| Code                       | Meaning                                                                                                                     |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `INVALID_STATE_PATH`       | The supplied base path is null, invalid for the operation, or resolves to an unacceptable target such as a directory.      |
| `STATE_FILE_NOT_FOUND`     | Restore cannot find the resolved saved-state file.                                                                          |
| `STATE_FILE_ACCESS_FAILED` | Save cannot write or restore cannot read the resolved file.                                                                 |
| `SAVED_STATE_INVALID`      | The file exists but is corrupt, incomplete, incompatible, or violates required Engine invariants.                          |

- Section 9 will decide the format, extension, version marker, and compatibility rules. Their expected failures map into these stable categories.
- Saved-state failures remain distinct from the XML-specific path, structure, and data codes because XML load and state restore use different trusted formats and recovery actions.

Additional structured context may include the path, actual event ID, option number, purchase quantity, current event status, clear detail, and recovery hint. Exact optional-field Java representation remains for the later class-detail checkpoint.

The following do not receive Engine codes:

- blank, non-numeric, or out-of-range console input;
- syntactically impossible text before a `Path` exists;
- a loaded system with no open events;
- `NullPointerException`, assertion failure, or another programming defect.

The UI owns the first two, no open events is valid state, and programming defects remain visible under D-032.

Alternatives not selected:

- One enum value per business rule was rejected because it would duplicate validation branches and expand whenever detail wording changes.
- One generic `OPERATION_FAILED` code was rejected because callers could not distinguish loading first, choosing an open event, correcting an option, reducing a quantity, or repairing a saved-state file.
- Passing raw low-level exception types to the UI was rejected because it would couple presentation to Engine implementation details.

Motivation: these categories describe different recovery actions rather than every internal cause. A small stable enum supports console and future JavaFX messaging, while structured detail preserves precision and the common checked exception keeps caller handling simple.

### D-044: Exact menu, parsing messages, and Engine-error presentation

Decision: the stable menu is rendered exactly as follows:

```text
=== Guess Market ===
1. Load system from XML
2. Display all events [requires a loaded system]
3. Display one event's trading status [requires a loaded system]
4. Purchase shares [requires a loaded system]
5. Close an event [requires a loaded system]
6. Save current system state [requires a loaded system]
7. Restore saved system state
8. Exit
Select a command:
```

- The menu appears at startup and after every completed or recoverable command. One blank line separates previous output from the next menu.
- The UI uses no colors and never clears the screen.
- Reusable UI-parsing messages are:

```text
Invalid menu choice. Enter a number from 1 to 8.
Invalid selection. Enter a number from 1 to N.
Invalid option. Enter 1 or 2.
Invalid quantity. Enter a positive whole number.
Invalid path. Enter a valid full path.
```

Engine failures use a consistent two-part presentation:

```text
Error: [what happened]
Recovery: [what the user should do]
```

Base mappings:

| Code                           | User-facing meaning and recovery                                                                                                      |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| `NO_SYSTEM_LOADED`             | No system is loaded. Load XML or restore saved state first.                                                                           |
| `EVENT_NOT_FOUND`              | Event ID `{id}` does not exist. Choose an event from the displayed list.                                                              |
| `EVENT_NOT_OPEN`               | Event ID `{id}` is closed. Choose an open event.                                                                                       |
| `INVALID_OPTION`               | Option `{number}` is invalid. Choose option 1 or 2.                                                                                    |
| `INVALID_QUANTITY`             | The purchase quantity is invalid. Start the purchase again with a smaller positive whole number.                                     |
| `FINANCIAL_CALCULATION_FAILED` | The operation exceeded the supported financial range. Review event data and, for purchases, try a smaller quantity.                  |
| `INVALID_XML_PATH`             | The XML path is invalid. Choose a regular `.xml` file.                                                                                 |
| `XML_FILE_NOT_FOUND`           | The XML file was not found at `{path}`. Check the path.                                                                                |
| `XML_FILE_ACCESS_FAILED`       | The XML file could not be read. Check access and try again.                                                                            |
| `XML_STRUCTURE_INVALID`        | The file is not valid Exercise 1 XML. Correct its XML or schema structure.                                                            |
| `XML_DATA_INVALID`             | The XML contains invalid market data. Include safe event or field detail and tell the user to correct it.                            |
| `ENGINE_CONFIGURATION_ERROR`   | Internal XML configuration is unavailable. Verify Engine packaging and JAXB dependencies.                                            |
| `INVALID_STATE_PATH`           | The saved-state path is invalid. Enter a valid base path and file name.                                                               |
| `STATE_FILE_NOT_FOUND`         | The saved-state file was not found. Check the path.                                                                                    |
| `STATE_FILE_ACCESS_FAILED`     | The saved-state file could not be read or written. Check access and try again.                                                        |
| `SAVED_STATE_INVALID`          | The saved-state file is invalid or incompatible. Choose a compatible valid file.                                                     |

- Available safe path, event, option, field, line, column, detail, and recovery context is included when it improves the message.
- Missing optional context is omitted rather than rendered as `null`.
- Raw low-level messages and stack traces are never part of normal user output.

### D-045: Testable console UI composition

Decision: the future UI module uses four focused classes. Exact packages and physical locations remain Section 10 decisions.

- `ConsoleMain` owns startup wiring. It creates `GuessMarketEngineImpl` once, constructs production input and output dependencies, and passes only a `GuessMarketEngine` reference onward.
- `GuessMarketConsoleApp` owns `run()`, the menu loop, a simple numeric `switch`, private command handlers, and the central `EngineOperationException` command boundary.
- `ConsoleInput` owns full-line reading and reusable integer, selection, quantity, and path parsing. It knows nothing about Engine, DTO, XML, or domain rules.
- `ConsoleRenderer` owns menus, prompts, summaries, details, receipts, history, numeric formatting, and error-code-to-message translation. It knows DTO values and `EngineErrorCode` but no Engine domain or JAXB classes.

Production uses `System.in` and `System.out`. Tests may inject scripted input and captured output. The application borrows these dependencies and does not close externally supplied streams. Exercise 1 does not add a Command pattern, reflection-based dispatch, or another UI framework for eight fixed commands.

### D-046: Section 8 console acceptance scenarios

Decision: the later test architecture must cover these behaviors, while exact test classes and framework remain Section 11 decisions:

1. Blank, non-numeric, zero, and out-of-range main-menu choices recover to a fresh menu.
2. Commands 2 through 6 before loading report `NO_SYSTEM_LOADED` and continue.
3. Invalid secondary input repeats only the current prompt; end-of-input exits normally.
4. Event lists preserve load order and map one-based positions to non-contiguous actual IDs.
5. Closed events remain selectable for details; purchase and close display open events only.
6. A loaded system with no open events receives the valid empty-set messages.
7. Successful purchase prints its receipt and same-operation resulting details and creates exactly one history entry.
8. Failed purchase changes no quantity, account, commission, or history state.
9. Successful close records the chosen winner, returns final details, and leaves the closed event inspectable.
10. Failed close changes no lifecycle, winner, account, commission, or history state.
11. Save, restore, explicit exit, and end-of-input use their distinct flows and messages.
12. Every `EngineErrorCode` maps to useful English output without exposing raw exceptions.
13. Two-decimal US formatting covers positive, negative, tiny, and negative-zero values.
14. An unexpected runtime defect from a fake Engine is not swallowed by normal recovery.
15. At least one complete scripted happy path returns to the menu after every command and exits normally.

Focused assertions are preferred over one enormous brittle output transcript. The exact test framework, class names, fixtures, integration boundaries, and full requirement-to-test matrix remain Section 11 decisions.

Motivation for the closeout batch: exact copy makes recovery testable, four small UI roles keep input and presentation independent from Engine behavior, and an observable scenario list prevents a visually plausible console from passing review while missing command-order or atomic-recovery requirements. Nitzan approved D-044 through D-046 together as a related-small-decisions batch on 2026-08-12.

## Approved bonus persistence boundary

### D-047: Dedicated serializable saved-state snapshot

Decision: use Java object serialization for a dedicated Engine-owned saved-state snapshot graph.

- Persistence-only snapshot classes represent exactly the state that the Engine chooses to save.
- The snapshot graph is written and read with the JDK object-stream mechanism.
- Live `GuessMarketEngineImpl`, domain objects, DTOs, and JAXB-generated objects are not serialized directly.
- Restore reads a snapshot into candidate data, validates it, reconstructs a complete application-owned domain candidate, and replaces live state only after every step succeeds.
- The persistence representation remains private to the Engine module and introduces no new library or second JAXB mapping.

Direct serialization of the live Engine and domain graph was not selected because it would make the file depend on incidental implementation fields and could recreate domain state without the normal construction path. A custom text, binary, or second XML format was not selected because it adds format and parsing work that is unnecessary for this small course bonus.

Motivation: the lecturer recommends Java object serialization as the simpler bonus direction. A dedicated snapshot follows that direction without making every live domain class part of the storage contract. It also gives restore one explicit place to check compatibility and invariants before atomic replacement. Nitzan approved this major representation decision on 2026-08-12.

### D-048: Saved-state extension and base-path resolution

Decision:

- The persistence extension is `.ser`, following the conventional Java serialization filename.
- The UI continues to pass the complete user-entered base `Path` without modifying it.
- The Engine appends `.ser` to the final filename.
- If the final filename already ends with `.ser`, using a case-insensitive check, the Engine uses it without appending the extension again.
- Another dot in the base filename is permitted. For example, `market.v1` resolves to `market.v1.ser`.
- The Engine performs the authoritative path validation. The UI remains unaware of the extension rule.

Motivation: one Engine-owned resolver prevents the console and any future UI from duplicating storage knowledge. Accepting an already supplied `.ser` suffix is a harmless recovery convenience while the normal prompt still asks for a path without an extension.

### D-049: Serialization and semantic format versions

Decision:

- Every custom serializable persistence class declares `serialVersionUID = 1L`.
- The top-level saved snapshot also stores an explicit integer `formatVersion` whose initial and only accepted value is `1`.
- Restore accepts only the exact current format version. Exercise 1 does not add migration between saved-state versions.
- A wrong root type, Java serialization class incompatibility, or unsupported semantic format version produces `SAVED_STATE_INVALID`.

`serialVersionUID` detects Java class-shape incompatibility. The explicit format version separately describes the meaning of the application-owned snapshot. Neither mechanism promises a permanent interchange format.

### D-050: Complete saved-state coverage

Decision: the top-level snapshot stores `formatVersion` and the complete nonempty ordered event collection. Each event snapshot stores the authoritative values needed to reconstruct its current state:

- event ID, name, and description;
- commission mode and percentage;
- LMSR parameter `b`;
- both option labels and current purchased quantities in XML order;
- open or closed lifecycle status and the winning option when closed;
- current event-account balance and total commission collected;
- complete chronological purchase history, with each entry's option number, retained label, quantity, base share cost, purchase commission, and total paid.

The snapshot does not store current prices or initial subsidy because the Engine deterministically recalculates them from quantities and `b`. It also excludes DTOs, JAXB-generated objects, calculators, console objects, streams, paths, temporary files, and a duplicated loaded-state flag. The required nonempty event collection represents a loaded system.

All financial values retained as state or history remain full-precision finite `double` values. Saving never applies UI rounding.

Motivation for the batch: the extension follows the selected Java mechanism, two distinct version checks make incompatibility explicit, and the saved field set preserves every authoritative runtime change without serializing presentation or reconstructible technical objects. Nitzan approved D-048 through D-050 together on 2026-08-12.

### D-051: Safe save, overwrite, and completed-file replacement

Decision:

1. Confirm that a system is loaded, resolve the `.ser` target, and create plus validate the complete snapshot in memory before opening a destination.
2. Require the parent directory to exist. The Engine does not silently create directories.
3. Permit overwrite of an existing regular saved-state file without a second confirmation prompt.
4. Serialize with `ObjectOutputStream` into a unique temporary sibling file and close the stream successfully before replacement.
5. Move the completed temporary file over the target with `ATOMIC_MOVE` and `REPLACE_EXISTING` when the filesystem supports it. If atomic movement is unavailable, fall back to replacing the target with the already completed temporary file.
6. Clean temporary files after success and on failure when cleanup remains possible.

A save failure does not change running Engine state. The Engine never intentionally streams directly into the final target, so an incomplete serialized stream is not deliberately exposed as the saved state. Expected write or final-replacement failures use `STATE_FILE_ACCESS_FAILED`.

Motivation: a user may deliberately reuse a save name, but direct serialization over the old file could destroy the last usable state midway through a failed write. A completed temporary sibling separates file production from final replacement with little additional complexity.

### D-052: Filtered, validated, and atomic restore

Decision: restore follows this order:

```text
resolve and validate path
-> open input with an ObjectInputFilter
-> read the expected top-level snapshot
-> validate representation and every saved invariant
-> reconstruct the complete ordered domain candidate
-> compare reconstructed state with the snapshot
-> replace the live collection once
```

- The object-input filter permits only the dedicated persistence types and required basic JDK value, array, and collection types. It also applies bounded object-depth, reference-count, array-length, and collection-size limits.
- The root type, Java and semantic versions, nonempty event count, unique positive IDs, text, commission rules, positive `b`, exactly two valid options, nonnegative quantities, lifecycle and winner consistency, finite full-precision financial values, account fields, and complete history are validated.
- Candidate reconstruction starts from normal event construction, replays purchases in chronological order, and closes the event with the saved winner when required.
- Reconstructed quantities, history, account balance, total commission, lifecycle, and winner must agree with the snapshot. Saved current prices and initial subsidy are neither expected nor trusted because they are derived.
- The candidate is a fresh ordered collection. No live event is mutated during reading, filtering, validation, replay, or comparison.
- Only a completely successful candidate replaces the live event collection through one reference assignment.

Filter rejection, corruption, incompatibility, a wrong root, or any snapshot or reconstructed-state inconsistency produces `SAVED_STATE_INVALID`. Expected read failures use the previously approved path, not-found, or access codes. Every rejected restore preserves the previous live system.

Motivation: Java deserialization can recreate fields without invoking the normal domain constructors. Filtering narrows which objects may be created, while reconstruction through normal operations and complete candidate validation restore the domain's invariants before the Engine trusts the file.

### D-053: Persistence failure mapping and acceptance scenarios

Decision: persistence uses the approved D-043 categories as follows:

- null or structurally unacceptable base or resolved paths use `INVALID_STATE_PATH`;
- a missing restore target uses `STATE_FILE_NOT_FOUND`;
- expected read, write, permission, temporary-file, or final-replacement failures use `STATE_FILE_ACCESS_FAILED`;
- corrupt, truncated, filtered, incompatible, wrong-root, unsupported-version, or invariant-invalid contents use `SAVED_STATE_INVALID`;
- unexpected programming defects are not swallowed or relabeled as ordinary user failures.

The later test architecture must cover:

1. Saving and restoring a system containing both open and closed events through a completely new Engine instance.
2. Preservation of event order, definitions, policies, quantities, full-precision accounting, winners, and complete chronological history.
3. Continued purchase and close operations after restore, plus preservation of closed-event restrictions.
4. Successful overwrite of an existing save with a later complete state.
5. Paths containing spaces, base filenames containing dots, and avoidance of a doubled `.ser.ser` suffix.
6. Save failure without any running-state mutation or deliberately published partial target.
7. Missing, corrupt, truncated, wrong-root, unsupported-version, filtered, and tampered saved-state files.
8. Preservation of the previous live system after every failed restore.
9. Cleanup of temporary files after successful and failed save attempts when cleanup is possible.

Motivation for the closeout batch: completed-file replacement prevents ordinary interrupted writes from publishing partial state, filtered candidate reconstruction addresses the important Java-deserialization risks identified in the course guidance, and restart-level acceptance scenarios prove that the bonus restores actual runtime progress rather than merely loading the original XML again. Nitzan approved D-051 through D-053 together on 2026-08-12. Section 9 is complete.

## Approved packages, files, build, and artifacts

### D-054: Constrained hybrid with command-line build authority

Decision:

- IntelliJ remains the development environment and will model the approved UI, Engine, and DTO build modules.
- A simple repository-owned Java 25 command-line build is authoritative for clean compilation, creation of the three application JARs, and final package verification.
- The authoritative build must be runnable from repository-controlled relative paths and must not depend on already compiled IntelliJ output.
- The authoritative build will compile from a clean output location in dependency order: DTO, then Engine, then UI.
- Maven and Gradle are not introduced for Exercise 1. The later exact build script, commands, paths, output names, manifest, and launcher remain separate Section 10 decisions.
- IntelliJ artifact definitions may be configured later as a course-aligned manual path or cross-check after the mandatory JAR recording is reviewed. They do not override the authoritative clean build.
- If the project works in IntelliJ but the authoritative clean build fails, the build is considered broken.

IntelliJ-artifact-only authority was not selected because its manual IDE state is harder to reproduce and can hide a failure that appears outside IntelliJ. Command-line-only development was not selected because IntelliJ modules and the lecturer's IntelliJ workflow remain useful learning and development tools. Maven and Gradle were not selected because they add an unrelated build framework and dependency model to a small assignment that already supplies its JAXB JARs directly.

Motivation: the final submission must run as separate JARs outside IntelliJ, while the course explicitly teaches IntelliJ modules, dependencies, and artifacts. The constrained hybrid keeps both benefits but gives only one path final authority. It also makes clean-directory regression checks possible without turning the build system into a second project.

Consequences for the remaining Section 10 decisions:

1. Module directories and source roots must be stable, repository-relative, and understandable to both IntelliJ and the command-line build.
2. Package topology and public visibility must preserve the approved compile order and dependency direction without relying on IDE-only access.
3. Generated JAXB source, the trusted XSD resource, and the five runtime dependency JARs must have explicit locations that the clean build can consume.
4. Later IntelliJ artifact definitions, manifest entries, classpath, and launcher must agree with the same three application JAR boundaries and runtime dependencies.
5. Final staging and verification must use outputs produced from a clean authoritative build, not arbitrary IDE output folders.

Nitzan approved this major build-authority decision on 2026-08-13 after separately reviewing IntelliJ-artifact-only, direct JDK command-line, constrained hybrid, and Maven or Gradle approaches. This new D-054 is limited to build authority and does not revive the earlier stopped D-054 through D-056 package and layout proposal.

### D-055: Conventional module-local directories and source roots

Decision:

- The three application modules live under `assignments/guess-market/modules`.
- Their directory and IntelliJ module names are `guessmarket-dto`, `guessmarket-engine`, and `guessmarket-ui`.
- Each module uses `src/main/java` as its production Java source root and `src/test/java` as its module-local test Java source root.
- A module uses `src/main/resources` and `src/test/resources` only when it owns production or test resources. Empty resource roots are not created merely for symmetry.
- The Engine will need a production resource root for its trusted XSD. Exact resource paths remain part of the later constrained-location batch.
- Exact test classes, framework choice, cross-module integration-test placement, and test execution commands remain Section 11 decisions. D-055 fixes only the module-local source-root convention.
- No `module-info.java` files are introduced. These are IntelliJ and build modules, not Java Platform Module System modules.
- The UI module uses the durable name `guessmarket-ui`, rather than `guessmarket-console-ui`, because the same codebase will replace the console presentation with JavaFX in Exercise 2.

Approved future structure:

```text
assignments/guess-market/
  modules/
    guessmarket-dto/
      src/main/java/
      src/test/java/
    guessmarket-engine/
      src/main/java/
      src/main/resources/
      src/test/java/
      src/test/resources/
    guessmarket-ui/
      src/main/java/
      src/test/java/
      src/test/resources/
```

Only roots that contain approved implementation or test content will be created. The tree above defines ownership and convention; it does not authorize creating the physical modules yet.

A minimal `module/src` layout was not selected because production code, resources, and tests would require additional custom rules, making the clean command-line build less obvious. Centralized project-level source folders were not selected because they weaken visible module ownership and make independent IntelliJ module compilation less natural.

Motivation: D-054 requires one layout that both IntelliJ and a clean repository-owned build can understand without depending on IDE output. Conventional module-local roots keep production classes out of test output, keep resources out of Java package directories, and give each JAR a visible source owner. The stable `guessmarket-ui` name also avoids an unnecessary module rename when JavaFX replaces the Exercise 1 console.

Consequences for the next decisions:

1. Every Java package will begin beneath exactly one module's `src/main/java` root.
2. Public Engine API types must live in Engine packages that UI can compile against, while internal Engine packages remain inaccessible by design where possible.
3. DTO packages must be public and JDK-only because both Engine and UI compile against `guessmarket-dto`.
4. Generated JAXB classes will later map beneath `guessmarket-engine/src/main/java`, and the trusted XSD will map beneath `guessmarket-engine/src/main/resources`.
5. Module-local unit and use-case tests belong beneath the owning module's test roots, while final packaged-system verification remains a later test-architecture decision.

Nitzan approved this major physical-layout decision on 2026-08-13 after reviewing conventional module-local roots, minimal course-style roots, and centralized source folders. He also clarified that every remaining major decision must be explained more fully before options are presented.

### D-056: Cohesive package topology with a narrow supported caller boundary

Decision:

- All public DTO value types live in `guessmarket.dto`: `MarketEventSummary`, `MarketEventDetails`, `MarketOptionSnapshot`, `PurchaseReceipt`, `TradeHistoryEntry`, `EventStatus`, and `CommissionMode`.
- The supported Engine caller contract lives in `guessmarket.engine`: `GuessMarketEngine`, `EngineOperationException`, and `EngineErrorCode`.
- `GuessMarketEngineImpl` also lives in `guessmarket.engine` and is public because `ConsoleMain` must construct it once at the application composition point. After construction, UI code stores and uses the `GuessMarketEngine` interface.
- Engine-owned domain, LMSR, account, internal-history, and persistence-snapshot types remain cohesively in `guessmarket.engine` unless implementation reveals a concrete need to separate them later. They stay private or package-private wherever cross-package collaboration does not require wider Java visibility.
- `MarketEvent` and `LmsrCalculator` receive only the technical public visibility needed by the approved mapper collaboration across `guessmarket.engine.xml` and `guessmarket.engine`. Only the constructors or methods required by that collaboration may be public. This technical visibility does not make them part of the supported UI API.
- `MarketOption` and other Engine implementation helpers remain package-private when the approved collaborations permit it.
- Handwritten XML integration classes live in `guessmarket.engine.xml`. `XmlMarketLoader` is accessible to `GuessMarketEngineImpl`; `JaxbMarketMapper` is package-private because only the loader calls it.
- XJC-owned classes live only in `guessmarket.engine.xml.generated`. No generated type crosses the Engine boundary or appears in a public Engine or DTO signature.
- Console classes live in `guessmarket.ui.console`. `ConsoleMain` is public as the entry point. `GuessMarketConsoleApp`, `ConsoleInput`, and `ConsoleRenderer` remain package-private unless a later named requirement proves that broader visibility is necessary.

Approved future package structure:

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

Supported production UI imports are deliberately narrow:

- UI code may import `GuessMarketEngine`, `EngineOperationException`, `EngineErrorCode`, and the public DTO types.
- `ConsoleMain` may additionally import `GuessMarketEngineImpl` solely to construct the application.
- UI code must not import `MarketEvent`, `MarketOption`, `LmsrCalculator`, account helpers, internal history, persistence snapshots, handwritten XML adapters, or JAXB-generated classes.

Because D-055 intentionally rejected `module-info.java`, Java does not provide a module export declaration that can enforce this supported boundary. The design therefore enforces it through the narrowest workable class and member visibility, public method signatures containing only Engine-contract, DTO, and JDK types, targeted import review, and later architecture tests. A Java type being public for a necessary cross-package collaboration does not grant UI permission to depend on it.

A dedicated `guessmarket.engine.api` package was not selected because, without JPMS exports, the extra namespace would label the API but would not enforce it. It would also separate a small interface and two failure types from a cohesive Engine package without solving a current problem. Fine-grained packages such as `.domain`, `.lmsr`, `.persistence`, and `.impl` were not selected because Java subpackages do not share package-private access. That split would force more internal constructors and methods to become public or require extra bridge and factory classes before the assignment has a need for them.

Motivation: packages should make the approved three-module dependency direction easy to understand, protect implementation collaboration where Java permits it, and prevent the UI from treating Engine internals as its contract. This topology provides meaningful boundaries without turning every responsibility into a separate namespace or confusing technical Java visibility with supported architectural API.

Consequences for the remaining Section 10 decisions:

1. Generated JAXB source has one exact package-derived destination beneath the Engine production source root.
2. The trusted XSD can use an Engine-owned classpath resource path that mirrors the handwritten XML package.
3. The authoritative build can compile DTO, Engine, and UI in order with explicit and reviewable classpaths.
4. Later JAR inspection must confirm DTO types are in the DTO JAR, Engine contract and implementation types are in the Engine JAR, console types are in the UI JAR, and generated types never leak into UI signatures.
5. Exercise 2 may replace the `guessmarket.ui.console` presentation while continuing to call the same supported Engine and DTO boundary.

Nitzan approved D-056 on 2026-08-13 after reviewing the motivation, the package mental model, class placement, visibility consequences, and why the rejected package splits were not genuine selectable recommendations. He also established that future checkpoints must present multiple options only when more than one path is genuinely recommendable.

### D-057: JAXB-derived files and development dependency paths

Decision: all JAXB-related copies have one explicit role and derive from the immutable canonical course files or intact supplied distribution.

Generated Java source:

- The retained generated package lives at `modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/generated`.
- Generation follows D-025: copy the canonical XSD temporarily beside `tools/jaxb-ri-4.0.5/xjc-run.bat`, run `xjc-run.bat -p guessmarket.engine.xml.generated GM-EX1-Schema.xsd`, and let XJC create the complete package tree beside the wrapper.
- Copy the complete generated package tree into the Engine production source root, verify its package declarations and clean compilation, retain the generated source in the project, and never edit it manually.
- After the retained copy is verified, remove the temporary XSD and generation staging tree. They are reproducible development artifacts, not sources of truth.

Trusted runtime XSD:

- Copy the canonical XSD to `modules/guessmarket-engine/src/main/resources/guessmarket/engine/xml/GM-EX1-Schema.xsd` during implementation and verify that its SHA-256 still matches the canonical file.
- The Engine JAR must contain the classpath entry `guessmarket/engine/xml/GM-EX1-Schema.xsd`.
- `XmlMarketLoader` uses this Engine-controlled classpath resource. It does not read the schema from the repository's `provided` tree at runtime and does not trust a schema location supplied by an input XML file.

Derived test resources:

- Hash-preserving copies of `single.xml`, `multiple.xml`, `error-2.xml`, and `error-3.xml` live beneath `modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/fixtures/supplied`.
- Separately named custom XML fixtures live beneath the sibling `fixtures/custom` directory.
- Tests do not modify supplied or custom resource files in place. A test that needs a writable, truncated, or otherwise mutated file first copies the resource to a temporary test directory.
- The trusted XSD is not duplicated beneath test resources. Engine tests exercise the actual production XSD resource.

Development dependency paths:

- IntelliJ and the authoritative command-line build reference these five runtime JARs directly from `tools/jaxb-ri-4.0.5/mod`: `jakarta.activation-api.jar`, `angus-activation.jar`, `jakarta.xml.bind-api.jar`, `jaxb-core.jar`, and `jaxb-impl.jar`.
- One IntelliJ runtime library containing those five JARs is associated only with `guessmarket-engine`.
- The runtime JARs are not duplicated inside a source module. UI and DTO have no direct JAXB dependency.
- `jaxb-xjc.jar` remains a generation tool used by the supplied wrapper. `jaxb-jxc.jar` remains preserved but unused. Neither belongs on the application runtime classpath.
- The later final-package decision will copy the five runtime JARs into a self-contained distribution location. D-057 does not approve that final `lib` path, manifest, launcher syntax, or staging layout.

Direct generation into production source was not selected because a failed generation could leave a partial source tree and because the approved course wrapper naturally stages output beside itself. Loading the canonical XSD from `provided` at runtime was not selected because a packaged application must not depend on a repository-only path. Directly testing against or modifying the canonical fixtures was not selected because course evidence must remain immutable and module tests should be self-contained. Module-local vendor-JAR duplicates and all-seven-JAR runtime classpaths were not selected because they create dependency drift and incorrectly treat compiler tools as runtime requirements.

Motivation: D-025, D-026, D-030, and D-054 through D-056 already determine ownership. D-057 turns those ownership decisions into stable repository-relative paths that both IntelliJ and the authoritative clean build can consume. It preserves a single canonical course source, one intact vendor distribution, a portable Engine JAR, and test resources that cannot corrupt the supplied evidence.

Consequences for the remaining Section 10 decisions:

1. The authoritative build compiles retained generated Java as part of Engine and uses exactly the five runtime JARs on Engine's compilation classpath.
2. The build must explicitly copy `src/main/resources` into Engine output before creating the Engine JAR.
3. Engine JAR verification must confirm the trusted XSD entry and the absence of UI or DTO classes.
4. UI and DTO compilation and JARs remain JAXB-free.
5. The later runtime-package decision must supply the five runtime JARs independently of the development-only `tools` path and make the launcher or manifest use them.

Nitzan approved this complete constrained placement batch as D-057 on 2026-08-13. No directories were created, no canonical files were copied, XJC was not run, and no IntelliJ dependency was configured during design.

### D-058: Parallel recording review with a non-blocking packaging gate

Decision:

- The mandatory `Java Packaging Methods - JAR` recording remains required evidence for the exact manifest, runtime classpath, launcher, dependency-folder, artifact, and final-distribution decisions.
- Reviewing the 61-minute recording does not block the remaining implementation-independent design work. A separate Codex task may watch it now and produce a timestamped evidence report while this task preserves context and continues into Section 11.
- The separate task is evidence-only. It does not approve architecture, edit project files, or convert older 2020 techniques into current requirements.
- This task will later reconcile that report with the current V2 handout, 2026 forum guidance, Java 25, the approved three-module non-fat-JAR architecture, D-054 command-line build authority, and D-057's five-JAR runtime boundary.
- No exact final JAR filenames, manifest entries, launcher command, final `lib` location, distribution tree, or clean staging rules are approved until that reconciliation occurs.
- The unresolved final packaging cluster does not block Section 11 test design, the GitHub decision, complete domain and interaction design review, or preparation of implementation tasks that do not depend on final distribution syntax.
- It does block declaring packaging complete, building the submission ZIP, and performing the final clean-machine grader walkthrough.

Motivation: the recording is mandatory learning evidence, but its unresolved details concern how an already designed application is assembled and launched. The implementation-sensitive boundaries are already fixed: three build modules and application JARs, `ConsoleMain`, dependency direction, Engine-owned resources, generated JAXB source, and five runtime JAXB libraries. Running the slow review in parallel avoids consuming this task's design context while retaining a hard gate before any final distribution choice.

Nitzan approved this timing and coordination decision on 2026-08-13. He may start a separate task using the evidence-review prompt supplied in the conversation and return its report to this task for reconciliation.

## Approved test strategy and acceptance criteria

### D-059: JUnit framework and module-local test placement

Decision:

- Use JUnit Jupiter from the JUnit 6 line for automated Java tests.
- Keep one repository-owned test-only runner at `tools/testing/junit-platform-console-standalone-6.1.1.jar`.
- The standalone JAR is used to compile and run tests from the authoritative command line and to keep test dependency setup simple. IntelliJ runs the same JUnit tests during development.
- The JUnit JAR is not an application dependency. It is excluded from production compilation, all three application JARs, the final runtime classpath, and the submission distribution.
- DTO tests live beneath `modules/guessmarket-dto/src/test/java/guessmarket/dto`.
- Engine tests live beneath `modules/guessmarket-engine/src/test/java/guessmarket/engine`, with XML-focused tests beneath the package-aligned `guessmarket/engine/xml` test directory.
- UI tests live beneath `modules/guessmarket-ui/src/test/java/guessmarket/ui/console`.
- A test may use the same Java package as the production class when focused package-private collaboration genuinely needs direct testing. Production visibility is never widened only to make a test possible.
- Most Engine behavior is tested through `GuessMarketEngine`. Focused calculator and domain tests may exercise package-private collaborators directly inside `guessmarket.engine`.
- UI tests use a small handwritten fake `GuessMarketEngine` to control successful values, expected checked failures, and unexpected runtime defects. Mockito is not introduced for Exercise 1.
- Tests do not use reflection to inspect private fields. They verify public results, returned DTOs, approved package-private units, persisted round trips, and observable state before and after an operation.
- Console tests assert focused prompts, messages, formatting, recovery, and command flow. One complete scripted happy path is retained, but one enormous exact transcript is not the primary strategy.
- Packaged-system checks remain a separate final layer because they must execute the produced JARs, launcher, resources, and dependency layout rather than only classes in test output. Their exact commands remain pending the packaging-recording reconciliation.

Test layers fixed by this decision:

1. DTO value and immutability tests.
2. Pure LMSR mathematics tests.
3. Focused domain transition and invariant tests.
4. Engine public use-case and atomicity tests through the interface.
5. XML schema, mapping, validation, and resource integration tests.
6. Persistence round-trip, rejection, and atomic-recovery tests.
7. Console input, rendering, and application-flow tests with a handwritten fake Engine.
8. Later packaged-system checks against the exact produced distribution.

Manual-only testing was not selected because deterministic LMSR calculations, failed-operation atomicity, XML error classification, persistence rejection, and console recovery all need repeatable regression checks. A custom JDK-only harness was not selected because it would recreate assertion helpers, test discovery, lifecycle, reporting, and failure exit codes that JUnit already provides. Mockito and private-field reflection were not selected because this small architecture already has an Engine interface and observable value boundaries, so those tools would add complexity or couple tests to implementation details without a corresponding benefit.

Motivation: D-055 already gives every module a test source root, D-056 gives tests stable package boundaries, and D-045 makes console dependencies injectable. D-059 turns those seams into one consistent automated-testing model while keeping the production graph unchanged: UI depends on Engine and DTO, Engine depends on DTO, and DTO depends only on the JDK. JUnit belongs to the private development toolchain, not to that application graph.

Consequences for the next Section 11 decision:

1. The next checkpoint no longer needs to choose a framework, mocking library, or centralized test module.
2. It must name the focused test classes, assign every approved behavior to the lowest useful test layer, and identify the smaller set of cross-layer and packaged checks.
3. The authoritative build will later need test compilation and execution commands after production compilation, but those commands cannot place JUnit in application artifacts.
4. The full requirement-to-test matrix must distinguish automated JUnit coverage, command-line architecture checks, packaged-system checks, and final manual review.

Nitzan approved D-059 on 2026-08-13 after reviewing the purpose of a test framework, the module-local placement model, the proposed test layers, and why manual-only or custom-harness approaches would add risk or duplicate infrastructure. No test dependency was downloaded, no test directories were created, and no test or production code was written during design.

### D-060: Behavior-centered test-suite topology

Decision: use eleven focused test classes and one UI test helper. A behavior is tested at the lowest layer that can explain a failure clearly, with a smaller number of broader tests proving collaboration across boundaries.

DTO module:

- `DtoContractTest` covers all approved DTO types. It verifies value preservation, immutable and defensive collection boundaries, optional-winner representation, and rejection of invalid construction when the approved DTO contract requires it.

Engine module in `guessmarket.engine`:

- `LmsrCalculatorTest` covers the approved LMSR formulas, the worked example, initial subsidy, stable calculations, extreme quantities, monotonic prices and costs, and invalid mathematical inputs.
- `MarketEventTest` covers the domain state machine: valid purchases, close behavior, quantities, commission, account balance, winner, chronological history, derived prices, and no partial mutation after rejected operations.
- `GuessMarketEngineUseCaseTest` constructs `GuessMarketEngineImpl` but stores and exercises it through `GuessMarketEngine`. It covers loaded-state rules, ordered listing, details, open-event filtering, event and option selection, returned DTOs, successful and failed purchase and close use cases, checked error contracts, and state preservation.
- `GuessMarketEngineXmlLoadTest` covers successful first and repeated loads through the public Engine contract, public XML error mapping, load-order preservation, and atomic retention of the previous live system after every failed load.
- `GuessMarketEnginePersistenceTest` covers save, overwrite, restore through a completely new Engine instance, complete state and history preservation, continued operation after restore, path rules, corrupt and incompatible data, tampered invariants, temporary-file behavior, and preservation of the live system after every failed restore.

Engine module in `guessmarket.engine.xml`:

- `JaxbMarketMapperTest` isolates conversion and business validation from file mechanics. It covers positive unique IDs, commission bounds, exactly two nonblank and distinct option names, positive `b`, trimming rules, preserved case and order, complete initial state, and precise invalid-data context.
- `XmlMarketLoaderTest` isolates path, file, schema, JAXB, and trusted-resource behavior. It covers null, blank, missing, inaccessible, malformed, schema-invalid, and valid files; use of the packaged trusted XSD; and correct low-level-to-Engine failure classification without leaking raw exceptions.

UI module in `guessmarket.ui.console`:

- `ConsoleInputTest` covers full-line parsing, blank and whitespace-only input, non-numeric input, integer overflow, zero, negatives, out-of-range menu and selection values, invalid quantities, paths containing spaces, prompt-local retries, and end-of-input at the main menu or a secondary prompt.
- `ConsoleRendererTest` covers the stable menu, empty-set responses, summaries, complete details, purchase receipts, history, lifecycle and winner display, all approved `EngineErrorCode` responses, omission of absent optional context, absence of raw exception text, and fixed two-decimal `Locale.US` formatting for positive, negative, tiny, and negative-zero values. It also proves that ON_CLOSE receipt and history output omit `Purchase commission`, while ON_PURCHASE at 0 percent retains the exact `Purchase commission: 0.00` line.
- `GuessMarketConsoleAppTest` covers all eight command conversations with scripted input and a handwritten fake Engine. It verifies commands before loading, menu recovery, event-position-to-ID translation, open and closed filtering, no-open-event behavior, successful and failed purchase and close flows, save and restore flows, explicit exit, graceful end-of-input, one complete happy path, and the rule that an unexpected runtime defect remains visible rather than being swallowed.
- `FakeGuessMarketEngine` is a test helper, not a test class. It records calls and returns configured DTOs, checked failures, or unexpected runtime failures so UI behavior can be verified independently of Engine internals.

Console coverage rule:

- Every approved Section 8 console acceptance scenario and every newly identified console edge case receives an automated assertion in one or more of the three console test classes unless the behavior inherently requires the final packaged process.
- Input tests prove whether a value is accepted, rejected, or retried. Renderer tests prove the response text and formatting. Application tests prove the complete conversation, correct Engine call, recovery scope, and subsequent menu state.
- Important scenarios may intentionally appear at two layers when they protect different responsibilities. For example, invalid purchase quantity is tested as an input retry when the text cannot be parsed and as an Engine failure response when a parsed value violates domain state.
- `ConsoleMain` does not receive a dedicated unit test because its only responsibility is production wiring. Construction, startup, main-class selection, standard streams, and real process exit are verified by later packaged-system checks.

One giant end-to-end suite was not selected because a failure would not identify whether parsing, rendering, UI orchestration, Engine behavior, XML, or packaging was responsible. A mechanical one-test-class-per-production-class approach was not selected because it would produce low-value tests for getters and private helpers while splitting meaningful behaviors such as a purchase across several artificial suites.

Motivation: D-059 fixed the framework and test boundaries. D-060 now gives every approved risk one clear primary owner and makes console correctness a first-class automated concern, not a final manual demonstration. The suite follows the production architecture while remaining behavior-centered: pure math first, state transitions next, public Engine use cases after that, then UI conversations, with packaged execution as the final external proof.

Consequences for the final Section 11 decision:

1. The remaining requirement-to-test matrix can map each requirement to these fixed suites rather than reopening class organization.
2. Console matrix rows must identify the input, response, Engine-call, recovery, and follow-up-menu assertion where applicable.
3. Architecture, JAR-content, launcher, classpath, clean-folder, and operating-system path checks remain outside ordinary unit tests and must be listed as command-line or packaged-system checks.
4. Implementation tasks can introduce one focused test class alongside the production slice it protects instead of postponing console and integration coverage until the end.

Nitzan approved D-060 on 2026-08-13 and explicitly confirmed that console edge cases and correct responses must be automated comprehensively. No test helper, test directory, dependency, or code was created during design.

### D-061: Requirement-to-test matrix and acceptance gates

Decision: every approved requirement must have an identified proof. A requirement is not considered verified merely because a related test class exists. The matrix distinguishes automated JUnit tests, authoritative-build and architecture checks, packaged-system checks, and the final manual grader walkthrough.

Coverage interpretation:

- The suite covers every specified scenario, every identified boundary and invalid-input category, every approved `EngineErrorCode`, every meaningful state transition, and every additional regression discovered during implementation or review.
- No finite suite can prove every imaginable input. Completeness means complete coverage of the approved contract and identified equivalence, boundary, state, failure, and recovery categories.
- Important risks may have more than one proof when different layers protect different responsibilities. This is intentional defense in depth, not accidental test duplication.

Requirement-to-test matrix:

| Requirement area | Primary proof | Additional proof |
| --- | --- | --- |
| DTO values and immutability | `DtoContractTest` | DTO compilation with JDK-only dependencies |
| LMSR formulas and approved worked example | `LmsrCalculatorTest` | Parameterized normal, boundary, and extreme values |
| Initial subsidy and initial event state | `LmsrCalculatorTest`; `JaxbMarketMapperTest` | Engine XML-load integration |
| Purchase quantities, costs, and resulting prices | `MarketEventTest` | `GuessMarketEngineUseCaseTest` |
| Commission and per-event account balance | `MarketEventTest` | Purchase receipt and event-details assertions |
| Successful-purchase history | `MarketEventTest` | Engine DTO and persistence round-trip assertions |
| Purchase validation and failure atomicity | `MarketEventTest`; `GuessMarketEngineUseCaseTest` | Console failure conversation |
| Close behavior and winner selection | `MarketEventTest`; `GuessMarketEngineUseCaseTest` | Console close conversation |
| Positive or negative final balance | Domain and Engine tests | Renderer formatting |
| Closed-event restrictions and retained history | Engine use-case tests | Console filtering and persistence round trip |
| Loaded-state requirements | `GuessMarketEngineUseCaseTest` | Console commands before loading |
| Event order, filtering, and actual-ID selection | Engine use-case tests | Console displayed-position-to-ID assertions |
| XML path, file, schema, JAXB, and trusted-XSD behavior | `XmlMarketLoaderTest` | Public Engine error mapping and Engine JAR resource inspection |
| XML business validation and initial candidate construction | `JaxbMarketMapperTest` | Supplied and custom fixtures through Engine loading |
| Failed-load atomicity and repeated loading | `GuessMarketEngineXmlLoadTest` | Before-and-after public state assertions |
| Save, overwrite, suffix, and path behavior | `GuessMarketEnginePersistenceTest` | Temporary and final file inspection |
| Restore into a completely new Engine | `GuessMarketEnginePersistenceTest` | Continued purchase and close after restore |
| Corrupt, incompatible, filtered, and tampered state | `GuessMarketEnginePersistenceTest` | Previous-live-state preservation assertions |
| Console main and secondary input boundaries | `ConsoleInputTest` | Parameterized invalid-input categories |
| Console messages, semantic fields, and numeric formatting | `ConsoleRendererTest` | Focused application-flow output assertions |
| All eight console command conversations | `GuessMarketConsoleAppTest` | One complete scripted happy path |
| Expected Engine failures and recovery scope | Application and renderer tests | Follow-up prompt or menu assertion |
| Explicit exit and end-of-input | Input and application tests | Packaged-process exit check |
| Unexpected runtime defects | `GuessMarketConsoleAppTest` | Defect must remain visible and fail the test |
| UI-to-Engine and module dependency direction | Authoritative compilation and source-import review | JAR-content inspection |
| Correct application JAR ownership and trusted XSD entry | Authoritative build verification | JAR-content inspection |
| Absence of JUnit, test helpers, and misplaced classes from application artifacts | JAR-content inspection | Clean packaged-distribution inspection |
| Launcher, runtime classpath, working-directory independence, and paths containing spaces | Packaged-system checks | Final manual grader walkthrough |
| README, submission contents, clean extraction, startup, use, and exit | Exact-artifact manual walkthrough | Submission ZIP inventory and hash record |

Console acceptance categories:

1. Blank, whitespace-only, non-numeric, overflowing, negative, zero, and out-of-range input.
2. Invalid main-menu choices and invalid secondary-prompt values, with only the current prompt repeated.
3. End-of-input at the menu and during a command.
4. Every Engine-backed command before a system is loaded.
5. Non-contiguous event IDs and correct displayed-position-to-actual-ID translation.
6. Closed events available for details but excluded from purchase and close selection.
7. A loaded system with no open events.
8. Successful and failed purchase conversations, including receipt rendering and no unnecessary details re-query.
9. Successful and failed close conversations, including rendering the returned final details and no unnecessary details re-query.
10. Save, restore, explicit exit, and end-of-input as distinct flows.
11. Every approved Engine error response, with available safe context and no rendered `null`, raw low-level message, or stack trace.
12. Positive, negative, tiny, and negative-zero numeric formatting.
13. Unexpected programming defects remaining visible rather than being swallowed as ordinary user errors.
14. One complete successful scripted session that returns to the menu after each completed command and exits normally.

Acceptance gates:

1. Every matrix row has an implemented proof before Exercise 1 is called complete.
2. All required JUnit tests pass from the authoritative clean command-line build.
3. No required test is disabled, conditionally hidden, or silently skipped. Any temporary exclusion must be explicit, explained, and cleared before completion.
4. Module-boundary, import, resource, and JAR-content checks pass.
5. The final packaged application passes clean-folder and real-process checks after Section 10 packaging is finalized.
6. The exact intended submission artifact passes the final manual grader walkthrough.
7. Every reproducible defect found after a missing case receives a permanent automated regression test when technically possible.

No line-coverage percentage and no JaCoCo dependency are introduced for Exercise 1. A percentage can reward executing lines without proving the important contract. The approved matrix instead requires direct evidence for each behavior and external check that matters.

Motivation: D-059 and D-060 define how tests are written and organized. D-061 defines when their evidence is sufficient. It prevents a green but incomplete unit suite from hiding broken module boundaries, missing resources, an unusable launcher, or an incorrect submission package. It also makes the user's console requirement explicit: specified and identified edge cases must prove both the response and the recovery behavior.

Nitzan approved D-061 on 2026-08-13. Section 11 is complete. D-062 through D-066 later fixed the artifact, classpath, launcher, staging, and exact packaged-system verification boundary needed by these gates. No implementation or test artifact was created during design.

## Resumed final Section 10 packaging

### Evidence reconciliation

The complete 1:01:49 packaging recording uploaded on 2020-08-08 was reviewed through local transcription and targeted frame inspection, and its stable technical claims were checked against Java 25. That evidence has no decision authority, but it closes the mandatory viewing gap.

Directly retained lessons:

- separate non-fat application JARs;
- one executable application entry point;
- a deliberate runtime dependency graph and matching relative physical layout;
- classpath resource loading for resources inside JARs;
- a Windows batch launcher;
- testing the exact assembled package outside IntelliJ.

Adapted lessons:

- the old two-application-JAR example becomes the approved UI, Engine, and DTO JAR graph;
- the old source and output layout is replaced by D-055's conventional module-local roots;
- IntelliJ Artifacts remains a course-aligned cross-check, while D-054's repository-owned Java 25 command-line build remains authoritative;
- the five JAXB runtime JARs extend the recording's dependency example, while XJC and JXC remain excluded from runtime;
- working-directory safety and path quoting are added because the recording assumes a prepared command prompt and does not teach those cases.

Rejected as current authority:

- old IntelliJ screen details and output paths;
- the old two-module count;
- old submission-source-folder instructions superseded by the current V2 handout and GitHub requirement;
- double-click file-association behavior;
- any implication that IDE success proves the submitted package.

### D-062: Executable UI JAR with complete manifest runtime graph

Decision:

- The UI JAR is the only executable application JAR.
- Its manifest contains `Main-Class: guessmarket.ui.console.ConsoleMain`.
- Its manifest `Class-Path` lists the Engine JAR, DTO JAR, and all five JAXB runtime JARs through their final relative distribution paths.
- The UI manifest lists the complete runtime graph directly. It does not rely on transitive `Class-Path` entries in Engine or DTO manifests.
- Engine and DTO are library JARs. They have no `Main-Class` and no custom runtime `Class-Path`.
- The launcher uses the `java -jar` form. It does not combine `-jar` with `-cp`, because Java 25 ignores other user classpath settings when `-jar` is used.
- The single Java launch command has this approved shape, with the exact UI filename fixed by the next placement batch:

```bat
@java -jar "%~dp0<UI-JAR>"
```

- `%~dp0` identifies the launcher batch file's own drive and directory. The complete UI JAR path is quoted, so launching remains safe from another current directory and from a distribution path containing spaces.
- Manifest `Class-Path` entries use space-separated relative URLs resolved against the containing UI JAR. Their exact filenames, order, and continuation-line formatting are fixed and verified after the next distribution-layout decision.
- The launcher does not change the caller's working directory. Its own JAR and runtime dependency discovery is location-safe, while user-entered relative XML or persistence paths retain normal process-working-directory meaning.

The explicit `java -cp` launcher remains technically valid in Java 25 but was not selected. It would make the runtime graph visible in the batch file and could use `lib/*`, but it would make the UI JAR non-executable through `java -jar`, rely more heavily on Windows-specific classpath syntax, and be less aligned with the lecturer's preferred and current-handout illustrative flow.

Motivation: the runtime graph is fixed and small, so an explicit UI manifest is understandable and auditable. The selected combination gives each layer one job: the UI manifest declares how the packaged Java application is assembled, and the batch file reliably locates and launches that application from Windows. Current Java 25 documentation confirms both the `Main-Class` requirement and manifest-relative `Class-Path` model.

Consequences for the next packaging decisions:

1. The UI JAR must sit at a stable location from which every manifest dependency has a simple relative path.
2. The next batch must fix the exact application JAR filenames, launcher filename, final `lib` contents, and distribution root.
3. The authoritative build must generate and inspect the UI manifest, including correct continuation formatting for its long `Class-Path` value.
4. Packaged checks must launch through the batch file from the distribution directory, from a different current directory, and beneath a path containing spaces.
5. A missing or extra runtime dependency, accidental XJC or JXC inclusion, incorrect manifest path, or hidden development classpath is a packaging failure.

Nitzan approved D-062 on 2026-08-13 after reviewing the 2020 evidence, current V2 handout, and current Java 25 behavior. No manifest, batch launcher, build output, module, or source file was created during design.

### D-063: Exact submission distribution layout and filenames

Decision:

- D-063 describes the contents visible at the root of the extracted submission ZIP. It does not describe the GitHub repository tree.
- The extracted ZIP root contains exactly these grader-facing entries:
  - `run.bat`
  - `README.pdf`
  - `guessmarket-ui.jar`
  - `lib/`
- The `lib/` directory contains exactly these seven runtime dependency JARs:
  - `guessmarket-engine.jar`
  - `guessmarket-dto.jar`
  - `jakarta.activation-api.jar`
  - `angus-activation.jar`
  - `jakarta.xml.bind-api.jar`
  - `jaxb-core.jar`
  - `jaxb-impl.jar`
- The ZIP exposes those four root entries directly. It does not add another wrapper directory inside the archive.
- The three application JAR filenames match their approved module identities and remain unversioned so that internal manifest paths stay stable.
- The logical UI manifest entries are:

```text
Main-Class: guessmarket.ui.console.ConsoleMain
Class-Path: lib/guessmarket-engine.jar lib/guessmarket-dto.jar lib/jakarta.activation-api.jar lib/angus-activation.jar lib/jakarta.xml.bind-api.jar lib/jaxb-core.jar lib/jaxb-impl.jar
```

- The logical launcher content is:

```bat
@java -jar "%~dp0guessmarket-ui.jar"
```

- The physical manifest continuation formatting, manifest source path, output paths, staging procedure, archive command, and verification commands remain for the next decision.
- The submission runtime ZIP excludes JUnit, `jaxb-xjc.jar`, `jaxb-jxc.jar`, compiled tests, test fixtures, loose source directories, IntelliJ project metadata, build intermediates, and a loose XSD. The application-owned XSD is packaged as an Engine JAR resource.
- GitHub will contain more than the runtime ZIP, including the project source. Its exact repository contents, generated and ignored files, documentation, build support, name, and visibility remain a later explicit decision. No remote is authorized by D-063.

Motivation:

1. The grader can extract the archive, see the launcher and README immediately, and run the application without an IDE.
2. All non-entry-point runtime dependencies live in one predictable location.
3. The approved relative manifest graph matches the physical distribution exactly.
4. The runtime deliverable remains separate from the broader source and development record required in GitHub.
5. Avoiding versioned application filenames and unnecessary directory layers reduces classpath mistakes without hiding architectural separation.

Nitzan approved D-063 on 2026-08-13. No ZIP, staging directory, JAR, manifest, launcher, README, module, or implementation file was created during design.

### D-064: One authoritative Windows Batch build entry point

Decision:

- The authoritative Exercise 1 build entry point is `build.bat` at the Guess Market project root.
- `build.bat` is a development and verification artifact intended for the source repository. It is not included in the D-063 runtime submission ZIP.
- The separate submitted `run.bat` remains the one-command grader launcher. The two files have different responsibilities:
  - `build.bat` turns source into a clean, tested, verified submission archive;
  - `run.bat` launches the already built application.
- The build uses Windows Batch plus the installed Java 25 JDK tools `javac`, `java`, and `jar`. It does not require PowerShell, Maven, Gradle, Ant, an IDE, a network download, or a machine-level configuration change.
- The script locates the project root through its own `%~dp0` location, quotes paths, keeps variables local with `setlocal`, and stops on the first failed required phase by checking exit codes.
- The script is organized into plainly named phases or labeled subroutines rather than one opaque chain of commands. Required phases may not be silently skipped in the authoritative invocation.
- The clean phase may delete and recreate only the fixed, validated project-owned `build` output directory. It never cleans source, `provided`, `tools`, documentation, or arbitrary caller-supplied paths.
- Java compiler argument files may hold quoted source-file lists so paths containing spaces and Windows command-length limits do not make compilation fragile.
- Java 25's `jar` tool supplies the needed archive operations, including JAR construction and inspection plus final ZIP creation without an unwanted manifest. PowerShell is not needed only for compression.
- IntelliJ remains useful for editing, incremental compilation, debugging, and a later artifact cross-check, but it does not replace the successful `build.bat` result.
- This is the Exercise 1 authority. Later assignments may deliberately revisit the build mechanism if JavaFX, client-server deployment, or dependency complexity materially changes the tradeoff.

Motivation:

1. Nitzan prefers a consistent and visible Windows Batch workflow alongside the required `run.bat` and wants to understand the automation syntax.
2. The current pipeline is bounded enough for Batch: three ordered application modules, one JUnit runner, five JAXB runtime JARs, one staging layout, and one archive.
3. A disciplined script eliminates hidden IntelliJ state and manual-command ordering while retaining direct exposure to `javac`, classpaths, `jar`, and process exit codes.
4. Simple named phases and repeated explicit failure checks are more verbose than PowerShell but remain understandable and diagnosable.

The rejected PowerShell choice would provide cleaner collection, path, and error-handling syntax. It was not selected because Batch can meet the current requirements safely under the constraints above, and the consistency and learning value of `build.bat` matter to Nitzan. Maven, Gradle, manual command lists, and IntelliJ-only packaging remain outside the approved design.

Nitzan conditionally selected the Batch option on 2026-08-13 if it could handle the required work safely and understandably. Local Java 25.0.4 checks confirmed the necessary `javac` and `jar` capabilities, and the course-supplied JAXB wrapper confirmed the existing Batch environment. The condition was satisfied and D-064 was approved. No build script or implementation artifact was created during design.

### D-065: Exact clean build, manifest, staging, and archive pipeline

Decision:

#### Controlled inputs

- `build.bat` lives at the Guess Market project root.
- Repository-owned packaging inputs live at:
  - `packaging/guessmarket-ui.mf`
  - `packaging/run.bat`
- The submission-specific README input lives at `private/submission/assignment-1/README.pdf` and is never treated as a public repository artifact. It contains required personal and contact information, so later Git rules must exclude the `private` tree.
- The source-repository README remains a separate privacy-safe project document.
- The authoritative build fails before packaging if any required input, source root, resource, JUnit JAR, or JAXB runtime JAR is absent.

#### One Java 25 toolchain

- The authoritative build requires `JAVA_HOME` to identify one complete Java 25 JDK and directly invokes:
  - `%JAVA_HOME%\bin\java.exe`
  - `%JAVA_HOME%\bin\javac.exe`
  - `%JAVA_HOME%\bin\jar.exe`
- The build does not combine whichever `java`, `javac`, and `jar` executables happen to appear first independently on `PATH`.
- All three tools must exist beneath the same `JAVA_HOME`, and the preflight phase must verify Java major version 25 before cleaning or compiling.
- Current course evidence selects Oracle JDK 25 with HotSpot for this workspace. In the 2026 Introduction lecture at 02:07:37 through 02:11:28, Aviad recommends Oracle HotSpot, says his examples and the checker use it, directs students to Oracle's download site, and demonstrates Oracle installation. The Introduction slide hyperlink also targets Oracle, and the guided summary records the same conclusion.
- The current machine has Oracle JDK 25.0.4 under `C:\Program Files\Java\jdk-25.0.4` and Eclipse Temurin 25.0.4 installed. Nitzan approved Oracle as the course toolchain after reviewing the evidence. The later environment-setup task should point `JAVA_HOME` to the Oracle JDK before the build is implemented or run.
- Temurin may coexist and does not need to be uninstalled for correctness because the build uses only `JAVA_HOME`. No JDK or environment variable was removed or changed during design.
- The submitted `run.bat` remains portable and invokes `java` from the grader's configured environment. It never contains Nitzan's local JDK installation path.

#### Generated build tree

Every authoritative invocation recreates only the fixed project-owned `build/` tree:

```text
build/
  source-lists/
    guessmarket-dto-main.txt
    guessmarket-engine-main.txt
    guessmarket-ui-main.txt
    guessmarket-dto-test.txt
    guessmarket-engine-test.txt
    guessmarket-ui-test.txt
  classes/
    guessmarket-dto/
    guessmarket-engine/
    guessmarket-ui/
  test-classes/
    guessmarket-dto/
    guessmarket-engine/
    guessmarket-ui/
  reports/
    junit/
  jars/
    guessmarket-dto.jar
    guessmarket-engine.jar
    guessmarket-ui.jar
  staging/
    guess-market-exercise-1/
      run.bat
      README.pdf
      guessmarket-ui.jar
      lib/
        guessmarket-engine.jar
        guessmarket-dto.jar
        jakarta.activation-api.jar
        angus-activation.jar
        jakarta.xml.bind-api.jar
        jaxb-core.jar
        jaxb-impl.jar
  verification/
    extracted-package/
  distributions/
    guess-market-exercise-1.zip
```

- Production classes and test classes have separate output roots so test code cannot enter application JARs.
- `build.bat` may delete and recreate only the resolved `<project-root>\build` directory after confirming that exact target. It never deletes or rewrites `modules`, `packaging`, `private`, `provided`, `tools`, `docs`, or `submissions`.
- Generated source-list argument files contain quoted source paths. `javac` reads them through `@<file>` so paths containing spaces and Windows command-length limits do not make compilation fragile.

#### Ordered compilation and tests

- Every compilation uses Java 25 and UTF-8 through the command shape `javac --release 25 -encoding UTF-8 -d <output> -cp <dependencies> @<source-list>`.
- Production order and direct compilation dependencies are:
  1. DTO against the JDK only.
  2. Engine against compiled DTO plus the five JAXB runtime JARs.
  3. UI against compiled Engine and DTO, without a direct JAXB dependency.
- Test compilation preserves module ownership:
  - DTO tests use compiled DTO plus the JUnit standalone JAR.
  - Engine tests use compiled Engine and DTO, Engine main and test resources, the five JAXB runtime JARs, and JUnit.
  - UI tests use compiled UI, Engine, and DTO, UI test resources, and JUnit.
- All approved suites run together through `junit-platform-console-standalone-6.1.1.jar execute` with the complete test classpath, `--scan-class-path`, `--include-engine junit-jupiter`, `--fail-if-no-tests`, ASCII tree details, and reports under `build/reports/junit`.
- A compilation failure, test failure, undiscovered test suite, missing input, or nonzero required command stops the pipeline before staging.

#### UI manifest

`packaging/guessmarket-ui.mf` is a valid physical source manifest with a final blank line:

```text
Manifest-Version: 1.0
Main-Class: guessmarket.ui.console.ConsoleMain
Class-Path: lib/guessmarket-engine.jar lib/guessmarket-dto.jar
  lib/jakarta.activation-api.jar lib/angus-activation.jar
  lib/jakarta.xml.bind-api.jar lib/jaxb-core.jar lib/jaxb-impl.jar

```

- Each continuation begins with two spaces: one manifest continuation marker plus one logical separator between classpath entries.
- An isolated Java 25.0.4 probe confirmed that `jar` normalizes the physical lines and preserves D-063's exact logical `Class-Path` value. The probe files were removed afterward.
- Vendor-generated fields such as `Created-By` are permitted. Verification compares the required logical `Main-Class` and `Class-Path`, not vendor-specific wrapping or metadata.

#### JARs, staging, and ZIP

- DTO JAR contains DTO production classes only.
- Engine JAR contains Engine production classes plus Engine main resources, including `guessmarket/engine/xml/GM-EX1-Schema.xsd`.
- UI JAR contains UI production classes and the approved custom manifest.
- JAR creation uses Java 25 `jar --create --file`, `--manifest` for UI, and `-C` to place classes and resources at archive-relative roots.
- Clean staging copies only D-063's approved files from controlled inputs, freshly created application JARs, and the five exact JAXB runtime JARs.
- The final archive is created with `jar --create --no-manifest --file build\distributions\guess-market-exercise-1.zip -C build\staging\guess-market-exercise-1 .`.
- `--no-manifest` prevents an unwanted root `META-INF/MANIFEST.MF`, and `-C` exposes D-063's files directly at ZIP root without another wrapper directory.
- `build.bat` produces only a candidate under `build/distributions`. It never automatically writes to `submissions` or Drive. Copying the verified artifact into a durable submission snapshot requires later explicit approval.

Motivation:

1. One clean output tree prevents stale IntelliJ output from influencing the submitted artifacts.
2. Separate production and test output makes application-JAR membership mechanically clear.
3. A single `JAVA_HOME` prevents mixed JDK tools while keeping the repository logic understandable.
4. Repository-owned manifest and launcher inputs are reviewable, while the identity-bearing README remains private.
5. Exact staging makes archive membership intentional instead of relying on broad directory copying.
6. The produced ZIP remains a candidate until the final D-066 evidence gate passes.

Nitzan approved D-065 on 2026-08-13, then confirmed Oracle JDK 25 as the course-aligned local toolchain after reviewing the current lecture transcript, slide hyperlink, and guided summary. No build tree, build script, manifest, launcher, private README, environment change, JDK uninstall, module, test, or implementation artifact was created during design.

### D-066: Eight-gate exact-artifact verification

Decision: Section 10 closes with one strict acceptance model. A candidate is not submission-ready merely because it compiles, passes unit tests, or launches from the build staging directory. It must pass all eight gates below against the exact artifacts produced by the authoritative build.

#### Gate 1: Environment and clean build

- `JAVA_HOME` identifies one complete Oracle Java 25 JDK for this workspace.
- `java.exe`, `javac.exe`, and `jar.exe` are invoked from that same JDK directory and report the required major version.
- Every controlled source, resource, packaging, JUnit, and JAXB input exists before cleaning begins.
- The build removes only the validated project-owned `build` directory and then compiles DTO, Engine, and UI from zero.
- No IntelliJ output directory or previously produced application class or JAR participates in the authoritative result.

#### Gate 2: Complete automated tests

- The authoritative build executes every D-060 suite through JUnit Platform Console Standalone 6.1.1.
- At least one test is discovered, every required suite executes, and the result has zero test failures, container failures, skips, disabled tests, and aborted tests.
- The JUnit process exits with code 0 and writes its reports beneath `build/reports/junit`.
- Console parsing, rendering, conversations, failure messages, recovery scope, end-of-input, and identified edge cases are tested through `ConsoleInputTest`, `ConsoleRendererTest`, and `GuessMarketConsoleAppTest`, not left only to a human walkthrough.

#### Gate 3: Application JAR inspection

- All three application JARs pass `jar --validate`.
- `guessmarket-dto.jar` contains only DTO production classes and ordinary archive metadata. It contains no Engine, UI, tests, JAXB-generated classes, or resources that belong elsewhere.
- `guessmarket-engine.jar` contains Engine production classes, the handwritten and generated XML packages, and the trusted XSD at `guessmarket/engine/xml/GM-EX1-Schema.xsd`. It contains no UI classes, test classes, or unpacked third-party JAXB classes.
- `guessmarket-ui.jar` contains UI production classes and `guessmarket.ui.console.ConsoleMain`. It contains no copied Engine, DTO, JAXB, or test classes.
- The UI manifest has exactly the approved logical `Main-Class` and seven-entry `Class-Path`. Harmless tool-generated metadata such as `Created-By` is allowed.

#### Gate 4: Exact ZIP membership

- The archive contains exactly the D-063 tree: root `run.bat`, `README.pdf`, and `guessmarket-ui.jar`, plus `lib` containing the two library application JARs and five JAXB runtime JARs.
- Verification rejects a wrapper directory, JUnit, XJC or JXC compiler JARs, source, tests, fixtures, IDE metadata, build intermediates, a loose XSD, unexpected files, or missing files.

#### Gate 5: Fresh extraction

- The produced ZIP is extracted into a newly recreated verification directory whose path contains spaces.
- All later packaged checks use that extracted copy, not `build/staging`, loose project files, IntelliJ artifacts, or the developer's current directory.

#### Gate 6: Real packaged-process scenarios

- Run the extracted `run.bat` from inside the package, from a different current working directory, and while the extracted package path contains spaces.
- Verify immediate explicit exit and graceful end-of-input.
- Verify a valid XML path containing spaces.
- Run one complete integration conversation covering load, list, details, purchase, close, save, restore through a new running process where applicable, and exit.
- Verify an invalid XML load followed by recovery and another usable command.
- Expected user errors produce the approved messages and recovery behavior without raw stack traces. Successful scenarios exit with code 0.
- Tiny prompt-parsing variations already covered by JUnit are not duplicated as separate process scripts. This gate proves real launcher, manifest, dependency, resource, working-directory, path, persistence-restart, and process behavior.

#### Gate 7: IntelliJ artifact cross-check

- Before final submission, configure the three separate IntelliJ artifacts once and build them as an independent course-aligned cross-check.
- Compare their logical class and resource ownership plus the UI manifest with the authoritative application JARs, and confirm the approved UI to Engine and DTO, Engine to DTO, DTO to JDK module graph.
- IntelliJ output is never copied into the submission and never becomes build authority. Byte-for-byte equality is not required because tool metadata and entry ordering may differ; logical contents and manifest behavior must agree.

#### Gate 8: Final grader walkthrough

- Start from a fresh copy of the exact final ZIP and follow the README as a grader who knows nothing about the project.
- Launch only `run.bat`, exercise the central required flows, confirm the bonus declaration and GitHub link, and record the final ZIP SHA-256.
- Copy that exact verified artifact to `submissions/assignment-1` and Drive only after Nitzan explicitly approves the candidate.
- An independent clean Windows-machine check is preferred when practical. If it is unavailable, the final verification record must say so rather than claiming it happened.

Motivation:

1. Compilation proves source consistency, but not archive ownership, runtime dependencies, resource loading, launcher behavior, or grader usability.
2. Testing staging alone can hide accidental dependencies on the repository or current working directory. Fresh extraction makes the ZIP itself the subject of verification.
3. Exact membership protects the assignment boundary and prevents private development or test material from leaking into the submission.
4. Process scenarios complement JUnit instead of replacing it: JUnit covers behavioral categories precisely, while the extracted process proves the assembled system.
5. The IntelliJ cross-check respects the lecturer's environment and detects configuration drift, while `build.bat` remains the reproducible source of truth.
6. The final walkthrough and hash bind approval, Drive copy, and submission to one known artifact.

Nitzan approved D-066 on 2026-08-13. Section 10 is complete. No verification script, IntelliJ artifact, build output, submission copy, environment change, or implementation artifact was created during design.

## Approved adversarial-review corrections

The first adversarial review preserved D-001 through D-066 as decision history and found thirteen correction areas. Nitzan authorized source-dictated and mechanical corrections directly and approved the only genuine design choice, the lightweight persistence representation, on 2026-08-15. D-067 through D-071 supersede only the inconsistent portions identified below. All unaffected parts of D-001 through D-066 remain approved.

### D-067: Market-maker net account and final-result presentation

Decision: the per-event account exposed by the Engine is the market maker's net cash result for that event. It is not the gross LMSR cost-function pool and is not initialized with the maximum subsidy.

The authoritative accounting rules are:

```text
initial account balance = 0.0
initial maximum subsidy = C(0, 0) = b * ln(2), derived only

purchase:
  balance += basePurchaseCost
  balance += onPurchaseCommission, when applicable

close:
  grossPayout = winningShareQuantity
  onCloseCommission = grossPayout * percentage / 100, when applicable
  netPayout = grossPayout - onCloseCommission
  balance -= netPayout

finalBalance =
    sum(basePurchaseCosts)
    + totalCommissionCollected
    - grossPayout
```

For an on-close commission, subtracting the net payout already retains the commission. The Engine must not also credit that same commission separately to the balance. `totalCommissionCollected` is updated for reporting, but it is not a second cash movement.

`b * ln(2)` remains the calculator-derived maximum subsidy or exposure measure. It is never credited to the event account at XML load or restore and is never debited at close. New XML candidates therefore start with zero balance and zero collected commission. A final negative balance is the subsidy the market maker paid, a positive balance is profit, and exact zero is break-even. The final balance is preserved after close and remains inspectable.

The corrected D-023 example for `b = 100`, 100 option-1 shares, and 5 percent on-purchase commission is:

```text
base cost = 62.0114506958278
commission = 3.10057253479139
balance after purchase = 65.1120232306191

if option 1 wins:
  final balance = -34.8879767693809
  result = subsidy

if option 2 wins:
  final balance = 65.1120232306191
  result = profit
```

A no-purchase close has balance `0.0` and proves the break-even case.

The existing `eventAccountBalance` DTO field remains sufficient. The UI keeps the signed `Event account balance` line. For a closed event it additionally derives one explicit result line from the unrounded value:

```text
balance > 0.0  -> Final market-maker result: Profit <formatted positive balance>
balance < 0.0  -> Final market-maker result: Subsidy <formatted absolute balance>
balance == 0.0 -> Final market-maker result: Break-even 0.00
```

Either signed zero is break-even. No epsilon may relabel a small representable profit or subsidy as break-even merely because two-decimal formatting displays `0.00`. Open-event details show the current account balance but do not call it realized profit or subsidy.

Confirmed evidence: the V2 closing requirement and Aviad Cohen's 2026-08-09 forum response, reviewed live and in Nitzan's supplied screenshots on 2026-08-15, explicitly say not to reset the market-maker account and define the remaining negative or positive balance as subsidy or profit. The supplied LMSR simulator's operator-net formula independently agrees with the zero-initialized net account.

This decision supersedes the account initialization and interpretation portions of D-017, D-019, D-023, D-029, D-040, D-050, D-052, D-060, and D-061. It does not add user accounts, a second cash pool, or any Exercise 2 behavior.

### D-068: Cancellation-safe LMSR delta and honest binary64 boundary

Decision: stable total-cost evaluation remains useful for `C(q1, q2)` and the derived initial maximum subsidy, but a purchase cost must never be calculated by subtracting two independently rounded total costs.

For selected quantity `qs`, other quantity `qo`, positive purchased quantity `d`, and positive `b`:

```text
z = (qs - qo) / b
h = d / b
p = selected option price before purchase

purchaseCost = b * log1p(p * expm1(h))
```

This is algebraically equal to `C(after) - C(before)`. The implementation evaluates it through cancellation-resistant, overflow-resistant, log-domain branches. It must not first materialize an underflowed `p`, an overflowing `expm1(h)`, or the invalid product `0 * Infinity`.

Required helper identities include:

```text
softplus(x) = max(x, 0) + log1p(exp(-abs(x)))
logP = -softplus(-z)

logExpm1(h):
  use log(expm1(h)) where that is safe
  use h + log1p(-exp(-h)) for large h

t = logP + logExpm1(h)
purchaseCost = b * softplus(t)
```

The final small-result branch must retain multiplication by `b` in the log domain when `softplus(t)` itself would underflow before scaling. Quantity differences and combined quantities are formed in `long` before conversion to `double`, so valid `int` quantities do not overflow during intermediate subtraction or addition. Exact implementation branch thresholds remain private calculator details and must be covered by boundary tests.

For example, `b = 1`, state `(100, 0)`, and a one-share purchase of the disfavored second option has a base cost of approximately `6.392138950083687E-44`. Stable total-cost subtraction incorrectly returns `0.0`; the direct delta retains the representable result.

Binary64 policy:

- Full precision means the unrounded Java `double` result, not arbitrary real-number precision.
- A derived observational price may underflow to exactly `0.0` or round to `1.0` at an extreme imbalance. Details remain available, and the state is not invalidated merely by that observational limit.
- Purchase cost is calculated independently of the rounded price. A displayed or represented price of `0.0` may coexist with a representable positive aggregate purchase cost.
- Every successful positive purchase has a finite, strictly positive base cost. If the mathematically positive result remains `+0.0` after the cancellation-safe log-domain calculation because it is below the binary64 range, reject atomically with `FINANCIAL_CALCULATION_FAILED`.
- If a positive commission percentage mathematically requires a positive commission but that amount becomes unrepresentable zero, reject atomically with the same code.
- Do not clamp to `Double.MIN_VALUE`, invent extra precision, allow free shares, or add `BigDecimal` and a separate transcendental implementation.
- Normal binary64 rounding of finite stored arithmetic remains possible. No design statement promises exact real-number accounting over the entire `int` state space.

The recovery guidance for numerical rejection is to choose a different quantity or option, not always a smaller quantity. A larger aggregate purchase can be representable when a one-share purchase is not. Every numerical rejection preserves quantities, account balance, commission, lifecycle, and history.

Required proof includes normal and large values, the corrected worked example, highly favored and disfavored purchases, the representable `E-44` case, a price that underflows while a larger aggregate delta remains representable, an unrepresentable positive delta, `int` boundary arithmetic, non-finite rejection, and complete failure atomicity. Simulator comparisons remain required for overlapping representable cases.

This decision supersedes the universal representability and total-cost-subtraction portions of D-008 and D-021 through D-024, plus their affected uses in D-036, D-041, D-043, D-050, D-052, D-060, and D-061. It preserves the handout's LMSR formula and the use of Java `double`.

### D-069: Source-bounded XML IDs and text validation

Decision: business validation enforces only the current XSD, explicit Exercise 1 requirements, and current lecturer clarification. It does not invent positivity, nonblank-text, or label-uniqueness rules.

Corrected mapping matrix:

| XML value or structure | Authoritative rule |
| --- | --- |
| `id` | Accept the full `xs:int` and Java `int` range, including zero, negatives, `Integer.MIN_VALUE`, and `Integer.MAX_VALUE`. Require uniqueness across the complete candidate only. Values outside `xs:int` remain schema failures. |
| `GM-event/@name` | Reconstruct the generated `xs:list` tokens with single spaces, apply the approved outer-whitespace normalization, preserve case, and accept an empty result because neither the XSD nor assignment forbids it. |
| `description` | Apply approved outer-whitespace trimming, preserve case and meaningful internal whitespace, and accept an empty or whitespace-only result after trimming. |
| `GM-options` | Require exactly two ordered options for Exercise 1 even though the XSD permits one or two. |
| Each `GM-option` | Trim applicable outer whitespace, preserve case and meaningful internal whitespace, and accept empty, whitespace-only-after-trimming, and duplicate labels. |
| `comision` and `b` | Keep the existing explicit application rules: commission is `0` through `90` inclusive and `b` is positive. |

Presence remains distinct from content. Missing required attributes or elements are XSD failures, while a present empty `xs:string` is allowed. The mapper does not substitute placeholder text, change case, collapse meaningful internal text beyond the XSD list behavior for the name, impose arbitrary length limits, or require unique event names or option labels.

One-based console positions remain independent of real event IDs. The UI translates a displayed position through the current summary list and passes the actual `int` ID to the Engine. The Engine map and public signatures remain unchanged.

Proof must cover valid IDs `0`, negative values, both `int` endpoints, duplicate nonpositive IDs, correct UI translation for noncontiguous and nonpositive IDs, persistence round trips, duplicate option labels, empty and whitespace-only text, exactly two options, preserved order, and missing-versus-empty structure. Custom valid boundary fixtures belong under Engine test resources and must be tracked.

This decision supersedes the positive-ID, nonblank-text, and distinct-option portions of D-015, D-027, D-028, D-052, D-060, and D-061. D-006, D-013, D-026, D-029's candidate replacement, and D-030's fixture provenance remain unchanged.

### D-070: Lightweight direct-domain saved state and honest publication guarantees

Nitzan selected the lightweight direct-domain option on 2026-08-15 after the review found the dedicated snapshot, mapping, replay, and comparison graph disproportionate to the five-point bonus.

Representation decision:

- Java object serialization remains the selected mechanism and `.ser` remains the Engine-owned case-insensitive extension.
- One private Engine-owned `SavedState` root stores `formatVersion = 1` and the complete ordered event collection.
- Only the reachable Engine domain, option, account, commission-policy, lifecycle, and internal-history types deliberately implement `Serializable`. Every custom serializable class declares `private static final long serialVersionUID = 1L`; enum identity follows Java's enum serialization rules.
- `GuessMarketEngineImpl`, public DTOs, JAXB-generated and handwritten XML types, UI types, calculators, loaders, `Path`, streams, and other technical objects are not serialized.
- Current prices and `b * ln(2)` remain derived. The stored account balance follows D-067 and starts from zero. Accepted finite historical costs, commissions, totals, and balances are preserved exactly as their binary64 values rather than recomputed during restore.

Restore boundary:

```text
resolve path
-> install stream-specific object filter
-> deserialize one SavedState into quarantine
-> verify root and format version
-> validate the complete reachable graph and cross-field invariants
-> build a fresh ordered LinkedHashMap keyed by unique full-range int IDs
-> replace the live map reference once
```

Validation includes a nonempty collection, exact allowed runtime types, unique IDs, D-069 text rules, commission and positive-`b` rules, exactly two options, nonnegative quantities, lifecycle and winner consistency, finite financial fields, complete history, commission totals, account arithmetic under D-067, and closed-event payout consistency.

Every mutable owned node and collection must be identity-distinct. Separate events cannot share an `EventAccount`, `MarketOption`, option collection, history collection, or mutable history node, and an event cannot contain an accidental internal alias that would let one later mutation affect another owner. Safe immutable enum or policy sharing may remain allowed. Restore rejects duplicate or cross-event mutable aliases before commit.

For each history entry, base cost and total paid are strictly positive. Purchase commission is nonnegative, equals zero for `ON_CLOSE` or zero percent, and is strictly positive for `ON_PURCHASE` with a positive percentage under D-068. Validation checks the retained option number and label against the owning event, `totalPaid = baseCost + purchaseCommission`, chronological checked quantity totals, total commission, and D-067 account arithmetic. Financial comparisons use the documented binary64 tolerance and the original chronological accumulation order rather than exact real-number equality or reordered sums.

Validation does not reenact each purchase or recalculate historical LMSR costs. The `.ser` file is trusted-local bonus storage, not an authenticated or hostile-input interchange format, and the design does not claim to detect every coordinated malicious alteration.

The object filter is a strict allowlist for the exact selected custom graph and its required basic JDK value, collection, and array types. Every other nonprimitive class, and every array with a disallowed component type, is rejected. It sets `maxdepth = 32`. It does not claim a general collection-size limit, and it does not impose fixed `maxrefs`, `maxbytes`, or `maxarray` values because the assignment provides no maximum event count, history length, or save size. Semantic validation after deserialization remains mandatory. Filter rejection, corruption, incompatibility, a wrong root, an unsupported version, or an invariant-invalid graph produces `SAVED_STATE_INVALID` and preserves the previous live system.

Save publication is corrected as follows:

1. Validate and serialize the complete state to a unique temporary sibling, then close it successfully.
2. First call `Files.move(temp, target, ATOMIC_MOVE)` with `ATOMIC_MOVE` alone.
3. If that call fails and the completed temporary file still exists, retry with `Files.move(temp, target, REPLACE_EXISTING)`.
4. If the first move throws and the temporary file no longer exists, do not retry and do not infer success. Report `STATE_FILE_ACCESS_FAILED`, preserve running Engine state, and describe the disk state as unknown. Only a normally returned move call proves publication success.
5. A successful fallback replacement is explicitly non-atomic.
6. If fallback fails, report `STATE_FILE_ACCESS_FAILED`, preserve running Engine state, and clean the temporary file when possible.
7. Do not promise that a previous target survives a failed move. Java specifies that the file state may be undefined after a failed non-atomic move, and an exceptional atomic-move outcome is not treated as proven success.

Tests must cover first save, overwrite, both publication paths where the filesystem permits observation, failed non-atomic replacement without false old-file guarantees, strict filtering, full semantic validation, complete state and history through a new Engine instance, continued operation, full-range IDs, corrected account results, numerical-boundary history, and one-reference live-state replacement. Format version remains `1` because implementation and saved artifacts do not yet exist.

This decision supersedes D-047's persistence-only snapshot graph, D-050's duplicated snapshot field mapping, D-051's combined move guarantee, and D-052's dedicated-type allowlist, normal-construction candidate model, purchase replay, and reconstructed-state comparison. It preserves D-048's path behavior, D-049's dual versioning intent, D-052's one-reference atomic live-map replacement and rejection preservation, D-053's error categories, the Engine-private storage boundary, the three-module graph, public API, JAR topology, and exact D-063 ZIP membership.

### D-071: Corrected proof ownership and final acceptance evidence

Decision: the eleven D-060 test classes remain sufficient, but proof ownership and the acceptance gates are corrected and expanded.

UI and Engine ownership:

- `GuessMarketConsoleAppTest` is the primary proof for OPEN-only presentation, fresh one-based display numbering, displayed-position-to-actual-ID translation, and no-open-event conversations.
- Engine tests prove XML/load order, public status exposure, actual-ID lookup and mutation, and authoritative rejection of operations on closed events.
- The Engine does not gain a console-specific filtering or displayed-position API.

The D-061 matrix is supplemented by these explicit rows:

| Requirement | Primary proof | Additional proof |
| --- | --- | --- |
| Corrected market-maker account, both commission timings, and profit, subsidy, break-even rendering | `MarketEventTest`; `ConsoleRendererTest` | Engine use-case, persistence, and console conversation assertions |
| Cancellation-safe LMSR and binary64 boundaries | `LmsrCalculatorTest` | Domain atomicity and supplied-simulator oracle cases |
| Full valid event-ID domain and source-bounded XML text rules | `JaxbMarketMapperTest`; Engine use-case tests | Console translation and persistence round trip |
| OPEN-only selection and position-to-ID translation | `GuessMarketConsoleAppTest` | Engine status and actual-ID rejection assertions |
| English-only user-facing text and required case-insensitive suffix handling | Console, loader, and persistence tests | Packaged transcript and source review |
| No colors, ANSI dependence, or screen clearing | `ConsoleRendererTest`; UI source review | Packaged multi-command conversation |
| Engine independence from console APIs | Engine compilation without UI plus import review | Engine tests with no console objects |
| Naming, packages, modifiers, visibility, generics, imports, comments, and method cohesion | Focused handwritten-source review | Clean compilation and architecture inspection |
| Resource closure on success and failure | Loader and persistence reopen, overwrite, move, and delete tests | Try-with-resources source review |
| Intentional charset behavior | XML/parser and build configuration review | Relevant load and packaged-path checks |
| LMSR comparison with supplied simulator | `LmsrCalculatorTest` recorded oracle cases | Manual value reconciliation record |
| Multiple-purchase newest-first history | `MarketEventTest` | DTO, renderer, and persistence assertions |
| Case-insensitive `.xml` and `.ser` suffixes | Loader and persistence tests | Packaged path scenario |
| GitHub link, meaningful history, tracked sources, and ignore rules | Final repository inspection | README and ZIP inspection |
| Generated JAXB source, custom XML fixtures, and build inputs are tracked | Git tracked-file and ignore inspection | Clean clone or equivalent build evidence |
| Private README and runtime `.ser` files are excluded from GitHub and exact ZIP membership | Git and ZIP inspection | Final privacy walkthrough |
| README content and 1-to-3-page length | Manual PDF inspection | Final grader walkthrough |
| Exact D-063 ZIP and extracted runtime | Archive inventory and fresh-extraction process checks | Final ZIP hash |
| Clean Windows 10 compatibility | Independent clean Windows 10 run of the exact final ZIP | Recorded environment and artifact hash |

JUnit proof is strengthened without adding another test class or application dependency:

1. The pinned JUnit Platform Console Standalone 6.1.1 run keeps `--fail-if-no-tests`, reports, disabled ANSI output, and deterministic ASCII tree details, with combined output captured beneath `build/reports/junit`.
2. A nonzero launcher exit fails the build immediately.
3. Missing reports or captured output fail the build.
4. A mandatory Batch-compatible post-test phase checks the pinned summary and fails unless test and container failures, skips, disabled tests, and aborts are all zero.
5. The same phase verifies all eleven exact D-060 test-class names and at least one executed test case for each. The parser is verified against the installed 6.1.1 output and report shape, including deliberate missing, disabled, aborted, and failing probes, before it becomes authoritative.
6. Staging cannot begin until this verifier succeeds. CI must invoke the authoritative `build.bat` rather than bypassing this phase with a separate weaker test command.

Gate 8 requires, rather than merely prefers, a successful run of the exact final ZIP on a clean Windows 10 machine. If that evidence cannot be obtained, the candidate carries an explicit unresolved submission risk and is not called submission-ready. A GitHub Actions Windows runner may supplement the evidence but cannot replace the required clean Windows 10 proof unless its actual environment is shown to meet that requirement.

The exact final extracted ZIP remains the runtime verification target. IntelliJ artifacts remain an independent logical cross-check only and never replace the authoritative build or extracted-package proof.

This decision supersedes only the contradictory proof ownership in D-060 and D-061, the incomplete proof matrix, D-065's insufficient exit-code inference, and D-066 Gate 8's preferred wording. It does not add a twelfth test class, a new module, a CI dependency, or an implementation artifact.

### D-072: Clean Sections console presentation

Decision: Stage 5 uses the approved Clean Sections console direction. Nitzan selected it after comparing three realistic browser mockups on 2026-08-17. The choice improves grader-facing hierarchy and scanning while preserving the eight-command behavior, four-class UI boundary, deterministic English output, and modest Exercise 1 implementation scope.

The exact main menu is:

```text
============================================================
                       GUESS MARKET
============================================================

DATA
  1. Load events from XML
  2. Display all events
  3. View an event's trading status

TRADING
  4. Purchase shares
  5. Close an event

STATE AND SESSION
  6. Save current state
  7. Restore saved state
  8. Exit

Commands 2 through 6 require a loaded system.

Choose a command [1-8]:
```

Presentation grammar:

- A top-level output block begins with a 60-character hyphen separator, an uppercase title, and another 60-character hyphen separator.
- Approved top-level titles are `EVENTS`, `OPEN EVENTS`, `EVENT DETAILS`, `PURCHASE SUMMARY`, `UPDATED EVENT STATUS`, and `FINAL EVENT STATUS` where their corresponding content is present.
- `OPTIONS`, `ACCOUNT`, and `PURCHASE HISTORY` are uppercase subsection titles inside an event-details block. They do not receive their own surrounding separators, matching the approved mockup's restrained hierarchy.
- Event summary blocks retain the labeled, variable-length structure from D-039 beneath `EVENTS` or `OPEN EVENTS`. They are not converted into a fixed-width table, truncated, or padded to the longest supplied value.
- Detailed event output begins with `EVENT DETAILS`, then displays `ID`, `Name`, `Description`, `Status`, and one combined `Commission` line before the `OPTIONS`, `ACCOUNT`, and `PURCHASE HISTORY` sections. An open event omits a winner. A closed event adds `Winner: N - label` after `Status`.
- Each option uses its own numbered line, followed by one indented line containing `Price` and `Shares purchased`. The option labels remain exact XML-provided text.
- `ACCOUNT` shows `Event account balance` and `Total commission collected`. A closed event also shows D-067's exact `Final market-maker result` line, derived from the unrounded final-balance sign.
- `PURCHASE HISTORY` prints `No purchases have been made.` for an empty history. Nonempty newest-first rows retain the complete D-041 financial breakdown.
- Existing success sentences, the two-line `Error` and `Recovery` mapping, empty-open-set messages, end-of-input message, and two-decimal `Locale.US` rules remain unchanged unless this decision gives a more specific label or prompt.

Canonical open-event details use this shape with real supplied data:

```text
------------------------------------------------------------
EVENT DETAILS
------------------------------------------------------------
ID: 2
Name: World Cap Winner
Description: Who do you think will win the world cap ?
Status: OPEN
Commission: 15% on close

OPTIONS
  1. Argentina
     Price: 0.50   Shares purchased: 0
  2. Spain
     Price: 0.50   Shares purchased: 0

ACCOUNT
  Event account balance: 0.00
  Total commission collected: 0.00

PURCHASE HISTORY
  No purchases have been made.
```

The renderer preserves the exact supplied description, including its punctuation and spacing. `ON_PURCHASE` renders as `N% on purchase`; `ON_CLOSE` renders as `N% on close`. Closed details add `Winner: N - label` after `Status` and the exact D-067 `Final market-maker result` line inside `ACCOUNT`.

The exact revised interactive prompts are:

```text
Choose a command [1-8]:
Choose an event [1-N]:
Choose an option [1-2]:
Choose the winning option [1-2]:
Enter shares to purchase (positive whole number):
Enter the full path to the XML file:
Enter the full path and file name to save, without an extension:
Enter the full path and file name to restore, without an extension:
```

`N` is the current displayed collection size. D-044's invalid-input messages remain exact and continue to repeat only the current prompt. Engine rejections still abandon the command and return to a fresh menu.

Restraint and portability rules:

- Use plain ASCII separators and ordinary spaces only. Do not add Unicode box drawing, emoji, icons, colors, ANSI sequences, cursor movement, or screen clearing.
- Do not surround the full application or every event with a box. That more decorative Command Center direction was reviewed and not selected.
- Do not add a console-width query, dynamic wrapping algorithm, fixed-width event table, confirmation screen, cancellation grammar, progress animation, or JavaFX behavior.
- Reprint the complete menu after each completed or recoverable command, separated from the prior output by one blank line, as already required by D-044.

Verification ownership:

- `ConsoleRendererTest` asserts the complete menu literal, every section heading, 60-character separators, open and closed detail shapes, summaries, receipt and history composition, and the absence of ANSI and clear-screen sequences.
- `ConsoleInputTest` asserts the revised prompts together with the existing parsing and prompt-local retry rules.
- `GuessMarketConsoleAppTest` asserts at least one complete multi-command transcript that returns to the Clean Sections menu after every completed or recoverable command.
- The Stage 5 smoke test and later packaged-process transcript include a long supplied description and real XML option labels to confirm readable variable-length output without truncation.

This decision supersedes D-044's exact menu literal, the conflicting prompt literals in D-035 through D-038, and the conflicting rendering shapes in D-039 through D-041. It does not change command numbering, command availability, Engine calls, DTO fields, selection translation, ordering, recovery boundaries, financial formatting, public APIs, or the deferred JavaFX boundary.

### Resolution index for historical forward references

The original wording remains historical. The following later decisions close statements that still used future or pending language:

| Historical location | Current resolution |
| --- | --- |
| D-015 XML field matrix | D-027 resolved the matrix; D-069 corrects its unsupported ID and text restrictions. |
| D-026 physical JAXB and packaging paths | D-057 and D-063 through D-066 resolved them. |
| D-028 exception getter and package details | D-044 and D-056 fixed semantic ownership; exact getter spellings remain implementation detail. |
| D-029 absent-winner representation | D-040 selected `OptionalInt`. |
| D-043 persistence details | D-047 through D-053 resolved the original design; D-070 now controls the reviewed lightweight representation. |
| D-045 class placement | D-056 resolved it. |
| D-046 test framework and matrix | D-059 through D-061 resolved them; D-071 supplies the reviewed corrections. |
| D-054 exact build and packaging commands | D-062 through D-066 resolved them; D-071 strengthens test verification. |
| D-055 production and test ownership | D-057 and D-059 through D-061 resolved them. |
| D-057 manifest, launcher, staging, and package verification | D-063 through D-066 resolved them. |
| D-058 recording and packaging gate | The recording evidence report and D-062 through D-066 resolved it. |
| D-059 packaged commands and acceptance gates | D-066 resolved them; D-071 corrects the final evidence boundary. |

Nitzan approved the only choice-bearing correction, D-070's direct-domain representation, on 2026-08-15. The remaining D-067 through D-071 content follows current assignment or lecturer evidence or mechanically resolves contradictions found by the approved adversarial review. No Java source, build script, generated class, test, IDE configuration, JAR, ZIP, environment change, GitHub remote, staging action, or commit was created by this correction pass.

Nitzan separately approved D-072's Clean Sections console presentation on 2026-08-17 after reviewing Clean Sections, Command Center, and Minimal Transcript mockups. D-072 is a post-review presentation decision and does not reopen the reviewed Engine or DTO architecture.

## Deliberately deferred Exercise 2 and Exercise 3 features

- JavaFX screens, controls, tasks, and progress indicators.
- Users, personal balances, blocking, and market-maker permissions.
- Order Book orders, matching, bid, ask, minting, spread, and mid-price.
- Additional lifecycle states.
- Tomcat, servlets, HTTP clients, polling, Gson, and WAR packaging.
- Exercise 3 name-based identity changes.
- Concurrent-client synchronization.

The approved Exercise 1 boundaries should make later refactoring reasonable, but none of these features belong in the current implementation.

## Historical pre-implementation gates

Sections 10 and 11, the consolidated written-design review, the D-067 through D-071 correction pass, and the detailed implementation plan were completed before Java implementation began. The original gates were:

1. Create the detailed implementation plan from the reviewed design and obtain Nitzan's approval of that plan.
2. Keep the separate GitHub repository name and visibility decision synchronized with the parallel GitHub-preparation stream. No remote may be created without explicit approval, and the private submission README and runtime `.ser` files must never enter the public repository.
3. Confirm that the implementation plan preserves the corrected proof ownership, tracks generated JAXB source and custom XML fixtures, invokes the authoritative `build.bat` in any future CI, and does not treat CI as the required clean Windows 10 proof.
4. Begin implementation only after the plan is approved.

## 2026-08-12 session closeout

Historical snapshot only: this subsection records the boundary as it existed on 2026-08-12. It does not override the current D-072 checkpoint.

- The durable design boundary is D-053. Sections 7, 8, and 9 are complete.
- No Java source tree, IntelliJ module, artifact configuration, generated JAXB source, build script, GitHub remote, or implementation was created.
- Section 10 was explained but not started. Exact module names, directories, source-root convention, package topology, public API placement, resources, artifact contents, JAR names, manifest, classpath, launcher, and staging remain unapproved.
- The local XJC, external-dependency, and IntelliJ artifact guides were reread. They confirm package-aligned XJC output, dependency association with the relevant module, and empty-JAR artifact configuration, but they do not decide the complete project layout for us.
- The mandatory 61-minute JAR packaging recording still requires direct review before manifest, launcher, classpath, and final packaging decisions are approved.
- Important Section 10 choices must be handled individually. Only constrained consequences may be grouped after the controlling decision is approved.

The intended Section 10 decision order for the next task is:

1. Authoritative build approach: IntelliJ artifacts, command-line build, a constrained hybrid, or another justified choice.
2. Module directories and source-root convention.
3. Package topology and public Engine API visibility.
4. Constrained generated-source, runtime-resource, and dependency locations.
5. Artifact, manifest, classpath, and launcher model after the mandatory recording is reviewed.
6. Exact filenames, output directories, clean staging, and package-verification rules.

Historical note: this was the 2026-08-12 closeout before Section 10 began. D-054 through D-066 later completed Section 10, and D-059 through D-061 completed Section 11. It is retained only as decision-history context and does not define the current checkpoint.

## V2 handout and forum reconciliation

The local `Guess Market - v2.docx` is the current working handout. Its SHA-256 is `1B68C8DB208F4EE924F0F63DEC5C0BF86FFD75805109225B416C80E327A0CB50`.

The 2026-08-12 comparison against the previous handout and the then-supplied forum screenshots found:

- The substantive Exercise 1 addition is the close-event account rule: after commissions and winner payout, preserve the remaining event or market-maker balance rather than resetting it. The final value may be positive or negative.
- The handout's display rule still allows at most two digits after the decimal point. The lecturer clarified that a tiny nonzero option price displaying as `0.00` is acceptable in the extreme quantity example.
- Exercise 1 still has no personal user accounts, so closing performs aggregate payout accounting only.
- Other V2 changes concern later exercises and do not expand the current implementation scope.

D-008, D-017, D-018, and D-019 record the original reconciliation. The 2026-08-15 adversarial review then used Aviad Cohen's exact forum reply, reviewed live and in Nitzan's newly supplied screenshots, to resolve the account ambiguity. D-067 now controls the zero-initialized market-maker net balance and its final profit, subsidy, or break-even presentation. D-068 controls the honest numerical boundary. D-002 and D-011 continue to encode the separately approved DTO-module decision.

## Current source-integrity checkpoint

The user downloaded a clean replacement set containing the XSD and four XML files. On 2026-08-11:

- all five files were well-formed XML;
- `single.xml`, `multiple.xml`, `error-2.xml`, and `error-3.xml` all passed XSD validation;
- `error-2.xml` contains a duplicate event ID, which requires application validation;
- `error-3.xml` contains commission `115`, which requires application validation against the handout's `0..90` rule.

On 2026-08-12, Nitzan confirmed that these were direct re-downloads from the same official course source. D-030 made them the canonical immutable files directly under `provided/assignment-1/test-files`, replacing the damaged captures and removing the redundant `new_raw` directory. This source closeout does not approve XJC generation or implementation.

Current SHA-256 evidence:

| File                | SHA-256                                                            |
| ------------------- | ------------------------------------------------------------------ |
| `error-2.xml`       | `C3F505D65095D1F3D1D6DB7D280043F4B11CB420CB32DA65CAE74B8DC8FBCAA0` |
| `error-3.xml`       | `0F98D0FBFCB89AEDE339B380ADBEDF2A700F7BAAD8D903CABDCBAFD920707E90` |
| `GM-EX1-Schema.xsd` | `EE3B60ED4A4806F571FD6EED769D0DF10C6461B99C5CECEB4AA78C4A370E17B1` |
| `multiple.xml`      | `F4BC8C80DBABA54795836A0E312925031FEAC3920A6BFA88E61E2A0E6E16CFB5` |
| `single.xml`        | `7FB3172321E0EE1CCECFC908BB47215F51E5793CB149EF77598E182D0CB07455` |

## Exact next checkpoint

The written design is approved through D-072. D-001 through D-066 remain intact as history, D-067 through D-071 contain the adversarial-review corrections, and D-072 controls the Stage 5 console presentation without changing Engine or DTO behavior.

Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c`. Stage 5 has implemented D-072 Tasks 11 and 12, passed independent review on `codex/e1-console-ui`, and has been successfully rebased directly onto `9a8c87c`. Do not modify the completed Stage 4 history. Stage 5 remains unmerged and unpushed while awaiting Nitzan's Stage 5 review and acceptance.
