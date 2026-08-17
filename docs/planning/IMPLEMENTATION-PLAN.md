# Guess Market Exercise 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete Java 25 Guess Market Exercise 1 console application, including the save-and-restore bonus, three non-fat JARs, automated evidence, and the exact verified submission ZIP.

**Architecture:** The implementation uses three one-directional modules. UI depends on Engine and DTO, Engine depends on DTO, and DTO depends only on the JDK. JAXB, LMSR, market state, XML loading, and Java serialization stay inside Engine, while the console owns only input, output, selection translation, and recovery conversations.

**Tech Stack:** Oracle JDK 25, Java 25 language and JDK APIs, JAXB RI 4.0.5, JUnit Platform Console Standalone 6.1.1 with JUnit Jupiter, Windows Batch, IntelliJ IDEA, Git, and GitHub.

**Status:** Approved by Nitzan on 2026-08-15 for staged execution through seven human review checkpoints. Stages 1 through 5 are reviewed, accepted, and merged. Stage 5 Tasks 11 and 12 plus approved D-073 Task 12A merged into `main` as `d8ef980` after the strict Java 25 all-eleven-suite gate passed all 145 tests and real-process audits covered the complete workflow, pre-load recovery, and exact EOF at the return pause. Stage 6 Task 13 is implemented on `codex/e1-packaging` and awaits final Stage 6 review; it is not accepted or merged.

## Global Constraints

- Exercise 1 only, including the 5-point save-and-restore bonus. Do not implement Exercise 2 or Exercise 3 features.
- Use Java 25 and compile with `javac --release 25 -encoding UTF-8`.
- Use exactly three application modules and JARs: `guessmarket-dto`, `guessmarket-engine`, and `guessmarket-ui`.
- Preserve the dependency graph `UI -> Engine + DTO`, `Engine -> DTO`, and `DTO -> JDK only`.
- Do not create `module-info.java` files and do not introduce Maven, Gradle, Mockito, JaCoCo, JavaFX, or a fat JAR.
- Keep JAXB/XJC inside Engine. Only the five approved JAXB runtime JARs enter the runtime classpath. XJC and JXC remain development tools.
- Use the public seven-method `GuessMarketEngine` contract exactly as approved.
- Use full-range Java `int` event IDs, including zero, negatives, `Integer.MIN_VALUE`, and `Integer.MAX_VALUE`.
- Initialize each market-maker event account to `0.0`. Treat `b * ln(2)` only as a derived maximum-subsidy measure.
- Use the D-068 cancellation-safe direct LMSR purchase delta. Never calculate a purchase delta by subtracting two independently rounded total costs.
- Keep calculations as unrounded `double` values. Only `ConsoleRenderer` formats financial output to two decimal places with `Locale.US`.
- Implement D-072's exact Clean Sections menu, prompt ranges, section grammar, and ASCII-only portability rules. Do not substitute the rejected Command Center or Minimal Transcript directions.
- Every failed load, purchase, close, or restore preserves the previous valid Engine state.
- Save and restore use one versioned direct-domain `SavedState` graph, a strict exact-class filter with `maxdepth = 32`, full semantic validation, and one live-map reference replacement.
- Use all eleven exact approved test class names. Every class must execute at least one test with zero failures, skips, disabled tests, and aborts.
- `build.bat` is authoritative. IntelliJ and future CI are independent cross-checks only.
- The final ZIP must match D-063 exactly and must pass all D-066 and D-071 gates, including a successful clean Windows 10 run.
- Do not track `provided/`, `private/`, actual submissions, build output, runtime `.ser` files, credentials, or identity-bearing artifacts.
- Do not copy any candidate to `submissions` or Drive, tag a submission, or submit anything without Nitzan's explicit approval.
- Use simple student-appropriate Java. Keep fields private, use the narrowest workable visibility, avoid generic state-changing setters, and explain meaningful new concepts in the walkthrough.

---

## 1. Authority and precedence

Use these files in this order during implementation:

1. `docs/design/EXERCISE-1-DESIGN.md`, with D-067 through D-072 superseding only the conflicting portions of earlier decisions.
2. `docs/design/EXERCISE-1-DESIGN-BRIEF.md` for the concise system map.
3. `docs/reviews/DESIGN-REVIEW-RESOLUTIONS.md` for the corrected risk boundary.
4. This implementation plan for task order, exact file ownership, tests, and checkpoints.
5. `docs/guides/IMPLEMENTATION-WALKTHROUGH.md` for the continuously updated learning and method record.

If implementation evidence exposes a conflict with the approved design, stop that task, switch to Diagnose mode, record the exact conflict, and ask Nitzan before changing a material contract.

## 2. Working cadence and stage review

Related steps inside a stage may be implemented in one uninterrupted batch. Do not stop after every getter or private helper. Still use the following cycle inside every task:

```text
focused failing test
-> confirm the expected failure
-> minimal coherent implementation
-> focused passing test
-> stage regression tests
-> update walkthrough
-> inspect diff and repository boundary
```

Stop for Nitzan at each stage gate, not after each method. At the gate, show:

- what behavior now works;
- the important methods and state transitions;
- focused and regression test evidence;
- any implementation choice not mechanically fixed by the design;
- the updated walkthrough section;
- the proposed milestone commit and pull request scope.

No stage is merged into `main` until its gate is accepted.

## 3. Effort estimate

These ranges estimate active implementation, testing, review preparation, and documentation work. They are durations, not a clock schedule. Nitzan's explanation and approval time, unexpected defect investigation, GitHub review latency, and access to a clean Windows 10 machine can extend the elapsed calendar time.

| Stage | Deliverable | Active effort |
| --- | --- | ---: |
| 1 | Project foundation, vendored JUnit evidence, DTO values, Engine public contract | 3 to 5 hours |
| 2 | Cancellation-safe LMSR and complete market domain transitions | 5 to 8 hours |
| 3 | XJC generation, trusted XSD, XML mapper, loader, and boundary fixtures | 4 to 7 hours |
| 4 | Direct-domain persistence, Engine orchestration, and public use-case integration | 7 to 11 hours |
| 5 | Console input, rendering, all eight command conversations, and UI tests | 5 to 8 hours |
| 6 | Authoritative build, JUnit proof verifier, three JARs, manifest, launcher, and ZIP | 6 to 9 hours |
| 7 | Exact-artifact verification, IntelliJ cross-check, private README, Windows 10 evidence, and grader walkthrough | 4 to 7 hours of active work, plus external machine availability |
| **Total** | Complete implementation through a submission-ready evidence decision | **34 to 55 active hours**, plus approval and external waiting time |

The largest uncertainty is not ordinary Java typing. It is the combined numerical-boundary testing, hostile or invalid saved-state validation, Batch proof parsing, and exact packaged-process verification.

## 4. Planned file map

### DTO module

Create under `modules/guessmarket-dto`:

```text
src/main/java/guessmarket/dto/CommissionMode.java
src/main/java/guessmarket/dto/EventStatus.java
src/main/java/guessmarket/dto/MarketEventSummary.java
src/main/java/guessmarket/dto/MarketOptionSnapshot.java
src/main/java/guessmarket/dto/TradeHistoryEntry.java
src/main/java/guessmarket/dto/MarketEventDetails.java
src/main/java/guessmarket/dto/PurchaseReceipt.java
src/test/java/guessmarket/dto/DtoContractTest.java
```

### Engine module

Create under `modules/guessmarket-engine`:

```text
src/main/java/guessmarket/engine/GuessMarketEngine.java
src/main/java/guessmarket/engine/EngineErrorCode.java
src/main/java/guessmarket/engine/EngineOperationException.java
src/main/java/guessmarket/engine/GuessMarketEngineImpl.java
src/main/java/guessmarket/engine/LmsrCalculator.java
src/main/java/guessmarket/engine/CommissionPolicy.java
src/main/java/guessmarket/engine/EventAccount.java
src/main/java/guessmarket/engine/MarketOption.java
src/main/java/guessmarket/engine/TradeRecord.java
src/main/java/guessmarket/engine/MarketEvent.java
src/main/java/guessmarket/engine/SavedState.java
src/main/java/guessmarket/engine/SavedStateValidator.java
src/main/java/guessmarket/engine/SavedStateStore.java
src/main/java/guessmarket/engine/xml/JaxbMarketMapper.java
src/main/java/guessmarket/engine/xml/XmlMarketLoader.java
src/main/java/guessmarket/engine/xml/generated/
src/main/resources/guessmarket/engine/xml/GM-EX1-Schema.xsd
src/test/java/guessmarket/engine/LmsrCalculatorTest.java
src/test/java/guessmarket/engine/MarketEventTest.java
src/test/java/guessmarket/engine/GuessMarketEngineUseCaseTest.java
src/test/java/guessmarket/engine/GuessMarketEngineXmlLoadTest.java
src/test/java/guessmarket/engine/GuessMarketEnginePersistenceTest.java
src/test/java/guessmarket/engine/xml/JaxbMarketMapperTest.java
src/test/java/guessmarket/engine/xml/XmlMarketLoaderTest.java
src/test/resources/guessmarket/engine/xml/fixtures/supplied/[four immutable XML copies]
src/test/resources/guessmarket/engine/xml/fixtures/custom/[tracked boundary fixtures]
```

The XJC task records the exact generated filenames after the approved wrapper creates them. Those files are retained and tracked but never edited manually.

### UI module

Create under `modules/guessmarket-ui`:

```text
src/main/java/guessmarket/ui/console/ConsoleMain.java
src/main/java/guessmarket/ui/console/GuessMarketConsoleApp.java
src/main/java/guessmarket/ui/console/ConsoleInput.java
src/main/java/guessmarket/ui/console/ConsoleRenderer.java
src/test/java/guessmarket/ui/console/ConsoleInputTest.java
src/test/java/guessmarket/ui/console/ConsoleRendererTest.java
src/test/java/guessmarket/ui/console/GuessMarketConsoleAppTest.java
src/test/java/guessmarket/ui/console/FakeGuessMarketEngine.java
```

### Build, packaging, documentation, and private input

```text
build.bat
packaging/guessmarket-ui.mf
packaging/run.bat
private/submission/assignment-1/README.pdf
docs/guides/IMPLEMENTATION-WALKTHROUGH.md
docs/guides/BUILD-AND-RUN.md
docs/guides/TESTING.md
docs/guides/JAXB-AND-XML.md
README.md
PROJECT_HANDOFF.md
```

`private/submission/assignment-1/README.pdf` exists locally and remains ignored. Its approved private-document review is complete; Stage 7 reopens it only for the final README-led grader walkthrough and exact-artifact acceptance.

## 5. Stage 1: Foundation, DTOs, and supported Engine contract

Milestone branch: `codex/e1-project-foundation`

### Task 1: Establish the testable three-module foundation

**Files:**

- Create with the first owned source or test file: the three approved module-local source roots from Section 4.
- Create: `tools/testing/junit-platform-console-standalone-6.1.1.jar`
- Create: `tools/testing/LICENSE.txt`
- Create: `tools/testing/NOTICE.txt`
- Modify: `docs/guides/TESTING.md`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: Oracle JDK 25 and the repository's approved dependency policy.
- Produces: a locally vendored JUnit 6.1.1 test tool with recorded source, SHA-256, license evidence, and no production dependency.

- [x] **Step 1: Verify the repository boundary before creating files**

Run:

```powershell
git status --short --branch
git check-ignore -v provided private build sample.ser
```

Expected: the branch is the milestone branch, the worktree has no unexplained changes, and every private or generated sample path is ignored by the intended rule.

- [x] **Step 2: Acquire and verify JUnit Platform Console Standalone 6.1.1**

Use the exact Maven Central artifact `org.junit.platform:junit-platform-console-standalone:6.1.1`, preserve its matching license and notice material, calculate SHA-256, and record the source URL, version, hash, and test-only role in `docs/guides/TESTING.md`.

Run:

```powershell
& "$env:JAVA_HOME\bin\jar.exe" --describe-module --file tools\testing\junit-platform-console-standalone-6.1.1.jar
Get-FileHash -Algorithm SHA256 tools\testing\junit-platform-console-standalone-6.1.1.jar
```

Expected: module description succeeds, the version is 6.1.1, and the recorded hash matches the file.

- [x] **Step 3: Confirm the dependency is test-only**

Run:

```powershell
git status --short
```

Expected: only the approved JUnit JAR, its legal files, and documentation changes appear. No production module contains a copied JUnit binary.

- [x] **Step 4: Update the walkthrough foundation entry**

Record why JUnit is a development tool, why it is excluded from the runtime ZIP, and the verified SHA-256. Do not mark any Java method implemented in this task.

- [x] **Step 5: Commit the foundation evidence**

```powershell
git add tools/testing docs/guides/TESTING.md docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "build: add verified JUnit test tool"
```

### Task 2: Implement immutable DTO contracts test-first

**Files:**

- Create: all seven DTO production files listed in Section 4.
- Create: `modules/guessmarket-dto/src/test/java/guessmarket/dto/DtoContractTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: JDK collection, `Objects`, and `OptionalInt` APIs only.
- Produces: the immutable public values used by both Engine and UI.

The exact public value contracts are:

```java
public enum CommissionMode { ON_PURCHASE, ON_CLOSE }
public enum EventStatus { OPEN, CLOSED }

public MarketEventSummary(
        int eventId,
        String name,
        String description,
        int commissionPercentage,
        CommissionMode commissionMode,
        List<String> optionLabels,
        EventStatus status)

public MarketOptionSnapshot(
        int optionNumber,
        String label,
        int shareQuantity,
        double currentPrice)

public TradeHistoryEntry(
        int optionNumber,
        String optionLabel,
        int shareQuantity,
        double baseShareCost,
        double purchaseCommission,
        double totalPaid)

public MarketEventDetails(
        int eventId,
        String name,
        String description,
        int commissionPercentage,
        CommissionMode commissionMode,
        EventStatus status,
        List<MarketOptionSnapshot> options,
        double eventAccountBalance,
        double totalCommissionCollected,
        List<TradeHistoryEntry> purchaseHistory,
        OptionalInt winningOptionNumber)

public PurchaseReceipt(
        int optionNumber,
        int purchasedShareQuantity,
        double baseShareCost,
        double purchaseCommission,
        double totalPaid,
        MarketEventDetails resultingEventDetails)
```

Every class exposes conventional `get...()` accessors, makes defensive copies with `List.copyOf`, and returns the immutable stored lists. DTO validation rejects null nested values, wrong option-list sizes, option numbers outside 1 or 2, negative quantities, non-finite financial values, an open event with a winner, and a closed event without a valid winner. Empty XML-provided strings and duplicate labels remain valid.

- [x] **Step 1: Write `DtoContractTest` with named contract cases**

Include separate tests for:

```text
summaryPreservesFullRangeEventIdAndEmptyText
summaryDefensivelyCopiesTwoOrderedLabels
optionSnapshotRejectsInvalidOptionNumberQuantityAndPrice
historyEntryPreservesFinancialBreakdown
detailsDefensivelyCopiesOptionsAndNewestFirstHistory
detailsRequiresWinnerOnlyForClosedEvent
purchaseReceiptKeepsSameOperationDetails
allReturnedCollectionsRejectMutation
```

- [x] **Step 2: Compile the tests first and confirm the expected failure**

Expected: compilation fails because the DTO types do not exist.

- [x] **Step 3: Implement the seven immutable DTO types**

Use explicit final classes with private final fields. Do not use console formatting, domain calculations, file APIs, JAXB types, or mutable collection exposure.

- [x] **Step 4: Run focused DTO tests**

Run JUnit selecting `guessmarket.dto.DtoContractTest`.

Expected: all DTO tests pass with no skipped, disabled, or aborted test.

- [x] **Step 5: Update the walkthrough**

Add one row for every DTO constructor and getter group. Explain defensive copying, immutable snapshots, and why a DTO is not a domain object.

- [x] **Step 6: Commit the DTO slice**

```powershell
git add modules/guessmarket-dto docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: add immutable market DTO contracts"
```

### Task 3: Define the complete supported Engine API and checked failure contract

**Files:**

- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/GuessMarketEngine.java`
- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/EngineErrorCode.java`
- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/EngineOperationException.java`
- Start early, then extend in Task 10: `modules/guessmarket-engine/src/test/java/guessmarket/engine/GuessMarketEngineUseCaseTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: DTO values and JDK `Path`, `List`, `Optional`, and `OptionalInt`.
- Produces: the only supported UI-to-Engine operation and failure boundary.

Create the interface exactly as follows:

```java
public interface GuessMarketEngine {
    int loadEventsFromXml(Path path) throws EngineOperationException;
    List<MarketEventSummary> listEvents() throws EngineOperationException;
    MarketEventDetails getEventDetails(int eventId) throws EngineOperationException;
    PurchaseReceipt purchaseShares(int eventId, int optionNumber, int quantity)
            throws EngineOperationException;
    MarketEventDetails closeEvent(int eventId, int winningOptionNumber)
            throws EngineOperationException;
    void saveState(Path path) throws EngineOperationException;
    int restoreState(Path path) throws EngineOperationException;
}
```

Create `EngineErrorCode` with exactly these values:

```java
INVALID_XML_PATH,
XML_FILE_NOT_FOUND,
XML_FILE_ACCESS_FAILED,
XML_STRUCTURE_INVALID,
XML_DATA_INVALID,
ENGINE_CONFIGURATION_ERROR,
NO_SYSTEM_LOADED,
EVENT_NOT_FOUND,
EVENT_NOT_OPEN,
INVALID_OPTION,
INVALID_QUANTITY,
FINANCIAL_CALCULATION_FAILED,
INVALID_STATE_PATH,
STATE_FILE_NOT_FOUND,
STATE_FILE_ACCESS_FAILED,
SAVED_STATE_INVALID
```

`EngineOperationException` stores `code`, `detail`, `recoveryHint`, optional `Path`, XML event number, event ID, field name, option number, quantity, current event status, line, and column. Use typed getters based on `Optional`, `OptionalInt`, and DTO `EventStatus`. Provide one concise constructor for failures with no context and one complete constructor for Engine and XML collaborators. Retain a low-level cause only for developer diagnosis.

- [x] **Step 1: Write the public contract cases in `GuessMarketEngineUseCaseTest`**

Cover the seven interface operations, the exact error-code set, and both structured exception constructors. Compile the test first and confirm that it fails because the three Engine contract types do not exist. This starts one of the eleven approved final test classes early instead of adding a temporary twelfth class.

- [x] **Step 2: Create the three approved contract types**

Keep the interface and error enum public. Keep exception construction and getters typed, with no formatted console message or dependency on an implementation class.

- [x] **Step 3: Compile DTO, the three Engine contract files, and focused contract tests**

Expected: compilation succeeds with Engine depending on compiled DTO and no JAXB or UI classpath.

- [x] **Step 4: Inspect public signatures**

Run `javap -public` on all three compiled types.

Expected: the seven operations and structured checked failure types match this task, and no Engine implementation, JAXB, console, or persistence type appears in a supported signature.

- [x] **Step 5: Update the walkthrough**

Explain interface-based programming, one checked exception category, structured context, and why the UI must not parse `getMessage()`.

- [x] **Step 6: Run the Stage 1 gate**

Verify DTO tests, JDK-only DTO compilation, Engine-to-DTO compilation, import direction, Git status, and walkthrough completeness. Then open one milestone pull request for Nitzan's review.

## 6. Stage 2: LMSR and complete domain behavior

Milestone branch: `codex/e1-lmsr-domain`

### Task 4: Implement cancellation-safe LMSR mathematics

**Files:**

- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/LmsrCalculator.java`
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/LmsrCalculatorTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: positive `b`, nonnegative current quantities, and positive purchase quantity.
- Produces: package-private static total-cost, price, maximum-subsidy, and direct-purchase-cost calculations.

Use these exact method signatures:

```java
static double totalCost(int quantityOne, int quantityTwo, int b)
static double priceForOption(int selectedQuantity, int otherQuantity, int b)
static double purchaseCost(
        int selectedQuantity,
        int otherQuantity,
        int purchaseQuantity,
        int b)
static double maximumSubsidy(int b)
```

The critical direct-delta implementation follows this structure:

```java
long difference = (long) selectedQuantity - otherQuantity;
double z = difference / (double) b;
double h = purchaseQuantity / (double) b;
double t;
if (difference < 0) {
    long combinedDifference = difference + purchaseQuantity;
    t = combinedDifference / (double) b
            - softplus(z)
            + logOneMinusExpOfNegative(h);
} else {
    double logP = -softplus(-z);
    double logExpm1 = logExpm1(h);
    t = logP + logExpm1;
}

double cost;
if (t < -37.0) {
    cost = Math.exp(Math.log(b) + t);
} else {
    cost = b * softplus(t);
}
```

`softplus(x)` uses `max(x, 0) + log1p(exp(-abs(x)))`. For every negative quantity difference, the combined branch forms `difference + purchaseQuantity` in `long` before conversion and uses the exact identity `t = (difference + purchaseQuantity) / b - softplus(z) + log(1 - exp(-h))`. `logOneMinusExpOfNegative(h)` evaluates the final correction as `log(-expm1(-h))` when `h <= ln(2)` and `log1p(-exp(-h))` otherwise. For a nonnegative difference, the ordinary branch evaluates `t = logP + logExpm1(h)`, where `logExpm1(h)` uses `log(expm1(h))` before the large-value boundary and `h + log(1 - exp(-h))` after it. Invalid arguments, non-finite outputs, and a required-positive purchase cost that becomes zero fail before any domain mutation.

- [x] **Step 1: Write `LmsrCalculatorTest` first**

Include the approved initial price, price sum, monotonicity, larger-`b`, maximum-subsidy, `b = 100` worked example, simulator oracle values, very large quantities, representable `6.392138950083687E-44` delta, price underflow with representable aggregate delta, unrepresentable positive delta, `int` boundary arithmetic, and invalid input cases.

- [x] **Step 2: Run the focused test and confirm failures**

Expected: the class or methods are missing.

- [x] **Step 3: Implement the minimal final calculator**

Keep the class final and stateless. Do not import DTO, XML, persistence, console, or commission types.

- [x] **Step 4: Run focused tests and simulator reconciliation**

Expected: every named numerical category passes within its documented tolerance, and no case returns `NaN`, infinity, or free positive shares.

- [x] **Step 5: Update the walkthrough and commit**

Explain log-sum-exp, softmax, cancellation, underflow, and why the direct delta is separate from display price.

```powershell
git add modules/guessmarket-engine/src/main/java/guessmarket/engine/LmsrCalculator.java modules/guessmarket-engine/src/test/java/guessmarket/engine/LmsrCalculatorTest.java docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: add stable LMSR calculations"
```

### Task 5: Implement market state, accounting, history, and atomic transitions

**Files:**

- Create: `CommissionPolicy.java`, `EventAccount.java`, `MarketOption.java`, `TradeRecord.java`, and `MarketEvent.java` in `guessmarket.engine`.
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/MarketEventTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: DTO `CommissionMode` and `EventStatus`, `LmsrCalculator`, validated XML definitions, and one-based option numbers.
- Produces: one self-validating event aggregate with atomic purchase and close operations.

Use this construction and transition boundary:

```java
public MarketEvent(
        int eventId,
        String name,
        String description,
        CommissionMode commissionMode,
        int commissionPercentage,
        int b,
        String firstOptionLabel,
        String secondOptionLabel)

TradeRecord purchase(int optionNumber, int quantity)
        throws EngineOperationException

void close(int winningOptionNumber)
        throws EngineOperationException
```

`MarketEvent` is public only because `JaxbMarketMapper` must construct it from a sibling package. Its supported caller boundary remains internal to Engine. Domain observation methods use the narrowest visibility needed by `GuessMarketEngineImpl` and persistence validation.

Purchase must calculate the new quantity, base cost, any on-purchase commission, total paid, new balance, new commission total, both resulting prices, and the new `TradeRecord` before changing a field. Only after all checks succeed may it commit quantity, account, and history.

Close must calculate gross payout, on-close commission, net payout, new balance, and new commission total before committing winner and `CLOSED`. For on-close commission, subtract only the net payout and do not credit the same commission twice.

- [x] **Step 1: Write `MarketEventTest` first**

Cover both commission modes, 0 and 90 percent boundaries, multiple purchases, newest-first public conversion order, quantity overflow, closed-event rejection, repeated close, profit, subsidy, exact break-even, no-purchase close, disfavored tiny purchase, numerical rejection, and before-after equality for every rejected operation.

- [x] **Step 2: Confirm the focused test fails for missing domain types**

- [x] **Step 3: Implement the five domain types**

Every custom serializable class required by D-070 declares `private static final long serialVersionUID = 1L`. The reachable DTO enums use Java's normal enum serialization identity. Do not make `GuessMarketEngineImpl`, `LmsrCalculator`, DTO classes, XML types, or technical file types serializable.

- [x] **Step 4: Run `LmsrCalculatorTest` and `MarketEventTest` together**

Expected: both pass and rejected operations leave every observable domain value unchanged.

- [x] **Step 5: Update the walkthrough and commit**

Record every meaningful constructor and transition method, the state it owns, its preconditions, its atomic commit point, and the test that proves it.

```powershell
git add modules/guessmarket-engine docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: implement market event transitions"
```

- [x] **Step 6: Run the Stage 2 gate**

Review numerical evidence, accounting examples, serializable-type boundary, domain visibility, full regression tests, and the updated walkthrough before merging the milestone.

## 7. Stage 3: JAXB generation and atomic XML candidate loading

Milestone branch: `codex/e1-engine-xml`

### Task 6: Derive and verify Engine XML resources and XJC source

**Files:**

- Create: the trusted XSD production resource path from Section 4.
- Create: four supplied fixture copies under `fixtures/supplied`.
- Create: tracked custom fixtures under `fixtures/custom` for every D-069 boundary.
- Generate: `modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/generated/*`
- Modify: `docs/guides/JAXB-AND-XML.md`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: immutable canonical files under local `provided/assignment-1/test-files` and the intact approved XJC wrapper.
- Produces: hash-verified derived resources and retained generated Java in `guessmarket.engine.xml.generated`.

- [x] **Step 1: Recheck canonical hashes before copying**

Calculate SHA-256 for the canonical XSD and all four XML fixtures and compare them with `provided/assignment-1/test-files/README.md`.

- [x] **Step 2: Copy derived resources without changing canonical bytes**

Recalculate hashes at every destination. Supplied copies must match their canonical sources byte-for-byte.

- [x] **Step 3: Create custom D-069 fixtures**

Include valid zero, negative, minimum, and maximum IDs; duplicate nonpositive IDs; empty description and option labels; duplicate labels; one-option invalid business data; commission 0 and 90; positive `b`; paths or filenames containing spaces; and missing-versus-empty schema cases. Use clear filenames that state the boundary.

- [x] **Step 4: Run XJC through the exact course wrapper**

From `tools/jaxb-ri-4.0.5`, use:

```bat
xjc-run.bat -p guessmarket.engine.xml.generated GM-EX1-Schema.xsd
```

Copy the complete generated package tree into the Engine production source root, inspect every package declaration, compile with the five runtime JAXB JARs, and then remove only the verified temporary XSD and staging tree beside the wrapper.

- [x] **Step 5: Record generated ownership**

List the exact generated filenames, generation command, source XSD hash, JAXB version, and the rule that generated files are never hand-edited.

- [x] **Step 6: Commit generated and resource inputs**

```powershell
git add modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/generated modules/guessmarket-engine/src/main/resources modules/guessmarket-engine/src/test/resources docs/guides/JAXB-AND-XML.md docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "build: add verified JAXB generated sources"
```

### Task 7: Implement business mapping into a complete ordered domain candidate

**Files:**

- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/JaxbMarketMapper.java`
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/xml/JaxbMarketMapperTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: the generated `GuessMarket` root graph.
- Produces: a new `LinkedHashMap<Integer, MarketEvent>` in XML order.

Use this mapper boundary:

```java
final class JaxbMarketMapper {
    LinkedHashMap<Integer, MarketEvent> map(GuessMarket root)
            throws EngineOperationException;
}
```

The mapper joins `xs:list` name tokens with one space, trims description and option outer whitespace, accepts empty text and duplicate option labels, requires exactly two options, accepts all unique `int` IDs, maps `on-purchase` and `on-close`, requires commission `0..90`, requires positive `b`, and constructs every event with zero quantities, zero balance, no winner, and empty history.

- [x] **Step 1: Write `JaxbMarketMapperTest` first**

Cover every supplied business-invalid fixture and every custom D-069 boundary with precise event, field, and option context assertions.

- [x] **Step 2: Confirm mapping tests fail**

- [x] **Step 3: Implement the mapper without file or JAXB-context mechanics**

Do not cache or retain the candidate. Do not mutate live Engine state. Do not invent nonblank, positivity, case-normalization, unique-label, or length rules.

- [x] **Step 4: Run mapper, DTO, math, and domain regression tests**

- [x] **Step 5: Update the walkthrough and commit**

```powershell
git add modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/JaxbMarketMapper.java modules/guessmarket-engine/src/test/java/guessmarket/engine/xml/JaxbMarketMapperTest.java docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: map JAXB events into domain state"
```

### Task 8: Implement trusted-schema file loading and error translation

**Files:**

- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/XmlMarketLoader.java`
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/xml/XmlMarketLoaderTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: a caller-supplied XML `Path`, the packaged trusted XSD, JAXB runtime, and `JaxbMarketMapper`.
- Produces: a fully validated candidate map or one structured `EngineOperationException`.

Use this technical boundary:

```java
public final class XmlMarketLoader {
    public LinkedHashMap<Integer, MarketEvent> load(Path path)
            throws EngineOperationException;
}
```

Validate null, case-insensitive `.xml`, existence, regular-file status, and readability. Load the XSD only from classpath resource `guessmarket/engine/xml/GM-EX1-Schema.xsd`. Configure schema parsing against the trusted resource, close streams with try-with-resources, retain safe line and column details when available, and translate only expected I/O, schema, JAXB, and mapping failures. Do not catch all `Exception` or `RuntimeException`.

- [x] **Step 1: Write `XmlMarketLoaderTest` first**

Cover null, directory, wrong suffix, uppercase suffix, missing, inaccessible where the OS permits, malformed, schema-invalid, business-invalid, valid path with spaces, trusted-resource loading, safe cause retention, resource closure, and programming-defect visibility.

- [x] **Step 2: Confirm loader tests fail**

- [x] **Step 3: Implement loader mechanics and six XML error categories**

- [x] **Step 4: Run all six current test classes**

Expected: `DtoContractTest`, `GuessMarketEngineUseCaseTest`, `LmsrCalculatorTest`, `MarketEventTest`, `JaxbMarketMapperTest`, and `XmlMarketLoaderTest` pass together. Complete Engine integration behavior is added in Stage 4.

- [x] **Step 5: Update the walkthrough, inspect imports, and commit**

```powershell
git add modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/XmlMarketLoader.java modules/guessmarket-engine/src/test/java/guessmarket/engine/xml/XmlMarketLoaderTest.java docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: load markets with trusted XML schema"
```

- [x] **Step 6: Run the Stage 3 gate**

Verify hashes, generated-source tracking, five-JAR compilation, trusted-resource use, full-range IDs, text rules, error context, state isolation, and walkthrough entries before merging.

## 8. Stage 4: Persistence and complete Engine orchestration

Milestone branch: `codex/e1-engine-core-persistence`

### Task 9: Implement direct-domain saved state with quarantined validation

**Files:**

- Create: `SavedState.java`, `SavedStateValidator.java`, and `SavedStateStore.java` in `guessmarket.engine`.
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/GuessMarketEnginePersistenceTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: a nonempty ordered collection of valid `MarketEvent` objects and an Engine-owned base `Path`.
- Produces: a completed `.ser` file or a fresh validated `LinkedHashMap<Integer, MarketEvent>`.

Use these package-private boundaries:

```java
final class SavedState implements Serializable {
    static final int CURRENT_FORMAT_VERSION = 1;
}

final class SavedStateStore {
    void save(Path basePath, Collection<MarketEvent> events)
            throws EngineOperationException;

    LinkedHashMap<Integer, MarketEvent> restore(Path basePath)
            throws EngineOperationException;

    static Path resolveStatePath(Path basePath)
            throws EngineOperationException;
}

final class SavedStateValidator {
    LinkedHashMap<Integer, MarketEvent> validate(SavedState candidate)
            throws EngineOperationException;
}
```

The store appends `.ser` case-insensitively, requires an existing parent directory, writes a unique temporary sibling, closes it, first calls `Files.move(temp, target, ATOMIC_MOVE)`, and only when the temporary file remains retries with `REPLACE_EXISTING`. A package-private injected move operation may be used to prove both branches without changing production behavior.

The restore filter allows only `SavedState`, the exact domain graph, required JDK collection or array types, strings, and enums, with `maxdepth = 32`. Validation proves nonempty ordered events, exact runtime types, distinct mutable identity, unique full-range IDs, text and option rules, nonnegative quantities, lifecycle and winner consistency, finite financial history, chronological quantity sums, commission totals, D-067 account arithmetic, and closed payout consistency. It never replays purchases or recomputes historical accepted costs.

- [x] **Step 1: Write store and validator persistence tests first**

Cover suffix rules, paths with dots and spaces, first save, overwrite, both publication paths, cleanup, missing and corrupt files, wrong root, wrong version, filtered class, empty state, duplicate object or ID, semantic inconsistency reachable through a serialized candidate, full-range IDs, and numerical-boundary history. Task 10 adds the public Engine round trip, prior-live-state preservation, and continued-operation cases to the same approved test class after `GuessMarketEngineImpl` exists.

- [x] **Step 2: Confirm focused persistence tests fail**

- [x] **Step 3: Implement state root, strict filter, validator, and store**

Do not serialize DTOs, Engine implementation, XML classes, calculators, paths, or streams. Do not claim a failed move preserves an old disk target.

- [x] **Step 4: Run persistence and domain tests together**

Expected: round trips preserve exact stored binary64 values and all semantic rejection cases preserve the caller's previous live map when tested through the Engine in Task 10.

- [x] **Step 5: Update the walkthrough and commit**

```powershell
git add modules/guessmarket-engine docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: add validated saved state persistence"
```

### Task 10: Implement the full concrete Engine and public integration tests

**Files:**

- Create: `modules/guessmarket-engine/src/main/java/guessmarket/engine/GuessMarketEngineImpl.java`
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/GuessMarketEngineUseCaseTest.java`
- Create: `modules/guessmarket-engine/src/test/java/guessmarket/engine/GuessMarketEngineXmlLoadTest.java`
- Complete: `modules/guessmarket-engine/src/test/java/guessmarket/engine/GuessMarketEnginePersistenceTest.java`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: `XmlMarketLoader`, `SavedStateStore`, domain objects, and DTO constructors.
- Produces: the complete seven-method `GuessMarketEngine` implementation.

`GuessMarketEngineImpl` owns:

```java
private Map<Integer, MarketEvent> events = new LinkedHashMap<>();
```

It has one public no-argument constructor for `ConsoleMain` and package-private constructors for tests that inject XML, persistence, or initial candidate collaborators without widening the supported public API.

Implement all seven methods with these commit points:

```text
loadEventsFromXml: candidate = loader.load(path); events = candidate
restoreState: candidate = stateStore.restore(path); events = candidate
purchaseShares: locate event; record = event.purchase(...); build receipt and same-operation details
closeEvent: locate event; event.close(...); build final details
listEvents/getEventDetails: build fresh immutable DTO snapshots from current state
saveState: validate loaded state; stateStore.save(path, events.values())
```

Empty `events` means no loaded system. Full and filtered console positioning never enters this class. DTO conversion creates newest-first history values on demand and exposes no domain or JAXB object.

- [x] **Step 1: Write public use-case and XML-load tests first, then complete public persistence cases**

Exercise the implementation through a `GuessMarketEngine` reference. Cover no-loaded-state rules, ordered listing, full-range actual-ID lookup, details, successful and failed purchase, successful and failed close, checked error context, atomic replacement after load and restore, a save and restore through a completely new Engine instance, continued operations after restore, and prior-state preservation after every failure.

- [x] **Step 2: Confirm the new tests fail because the implementation is missing**

- [x] **Step 3: Implement `GuessMarketEngineImpl` and DTO conversion helpers**

Use `@Override` on all interface methods. Do not print, parse console text, expose a filtering method, or return internal collections.

- [x] **Step 4: Run every Engine and DTO test class**

Expected: eight exact classes now exist and pass: one DTO class plus seven Engine classes.

- [x] **Step 5: Run source-boundary inspection**

Confirm Engine production imports contain no `guessmarket.ui` or `java.util.Scanner`, and public signatures contain only JDK, DTO, and approved Engine contract types.

- [x] **Step 6: Update the walkthrough and commit**

```powershell
git add modules/guessmarket-engine docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: implement complete Guess Market engine"
```

- [x] **Step 7: Run the Stage 4 gate**

Demonstrate XML replacement, purchase, close, save, restart restore, failure atomicity, direct-domain filtering, and actual-ID behavior. Review the complete Engine public boundary and walkthrough before merging.

## 9. Stage 5: Testable console UI and all command conversations

Milestone branch: `codex/e1-console-ui`

### Task 11: Implement reusable console input and rendering

**Files:**

- Create: `ConsoleInput.java` and `ConsoleRenderer.java`.
- Create: `ConsoleInputTest.java` and `ConsoleRendererTest.java`.
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: borrowed `Scanner` and `PrintWriter`, DTOs, `EngineErrorCode`, and `EngineOperationException`.
- Produces: prompt-local parsing and deterministic Clean Sections English presentation.

Keep both classes package-private. `ConsoleInput` reads only complete lines and provides separate methods for one-shot menu choice, repeated range selection, option selection, positive quantity, and full path. A nested checked `EndOfInputException` distinguishes normal input closure from invalid text without adding a fifth production class. Its production prompts are the exact D-072 strings, including `Choose a command [1-8]:`, the dynamic `Choose an event [1-N]:`, both `[1-2]` option prompts, and the positive-whole-number quantity prompt.

`ConsoleRenderer` owns the exact menu, prompts, summary blocks, details, receipts, newest-first history, empty-open-set messages, success messages, `Error` and `Recovery` mapping, and one shared formatter. Its exact menu literal is:

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

Use one shared 60-character hyphen separator for top-level output blocks. Render only the D-072 top-level headings that correspond to present content: `EVENTS`, `OPEN EVENTS`, `EVENT DETAILS`, `PURCHASE SUMMARY`, `UPDATED EVENT STATUS`, and `FINAL EVENT STATUS`. Render `OPTIONS`, `ACCOUNT`, and `PURCHASE HISTORY` as uppercase subsections inside event details without surrounding separators. Preserve variable-length labeled blocks and exact XML option labels without table truncation or console-width logic. The menu and every output byte remain plain ASCII except user or XML text that is preserved as data.

The shared financial formatter remains:

```java
String formatFinancial(double value)
```

The formatter uses `String.format(Locale.US, "%.2f", value)`, converts rendered `-0.00` to `0.00`, and treats non-finite input as a programming defect. Closed details derive `Profit`, `Subsidy`, or `Break-even` from the unrounded sign.

- [x] **Step 1: Write `ConsoleInputTest` and `ConsoleRendererTest` first**

Cover every unchanged D-044 parsing message, every revised D-072 prompt, whitespace, integer overflow, prompt-local retry, path spaces, end-of-input, the complete D-072 menu literal, exact 60-character separators, all approved section headings, full and filtered summary numbering, long variable-length descriptions without truncation, details, winner, both commission modes, profit, subsidy, break-even, multiple history rows, all sixteen error codes, omitted absent context, no raw cause text, no ANSI, Unicode box drawing, or clear-screen sequence, and positive, negative, tiny, signed-zero formatting.

- [x] **Step 2: Confirm focused UI tests fail**

- [x] **Step 3: Implement D-072 input and rendering with injected streams**

Use named constants for the 60-character equals and hyphen separators so menu and block widths cannot drift independently. Keep output methods focused by block, but do not add another production class or a generic layout framework. Do not close borrowed streams. Do not import domain, XML, or persistence classes.

- [x] **Step 4: Run the two focused UI test classes**

- [x] **Step 5: Update the walkthrough and commit**

```powershell
git add modules/guessmarket-ui docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: add console input and rendering"
```

### Task 12: Implement the eight-command app, startup wiring, and fake-driven conversations

**Files:**

- Create: `GuessMarketConsoleApp.java` and `ConsoleMain.java`.
- Create: `GuessMarketConsoleAppTest.java` and `FakeGuessMarketEngine.java`.
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: `GuessMarketEngine`, `ConsoleInput`, `ConsoleRenderer`, DTOs, and structured Engine failures.
- Produces: the complete console application and its only public `main` entry point.

`ConsoleMain` constructs `GuessMarketEngineImpl` exactly once, stores it in a `GuessMarketEngine` variable, creates UTF-8 standard-stream wrappers, and calls `GuessMarketConsoleApp.run()`. It does not close `System.in` or `System.out`.

`GuessMarketConsoleApp.run()` owns the stable loop and a simple numeric `switch`. It catches only `EngineOperationException` at the command boundary and the nested end-of-input signal at the outer input boundary. It does not catch unexpected `RuntimeException` or `Error`.

Purchase and close call `listEvents()`, filter `OPEN` summaries in load order, assign fresh positions, translate the selected position to the actual event ID, and then use that actual ID. Purchase renders the receipt's resulting details without another query. Close renders the returned final details without another query.

- [x] **Step 1: Implement the handwritten fake and write `GuessMarketConsoleAppTest` first**

The fake records every call and can return configured values, throw a configured checked failure, or throw a configured unexpected runtime defect.

Cover all fifteen D-046 scenarios, all D-071 UI-owned filtering and ID-translation proofs, and D-072's menu-return and revised-prompt requirements. The complete happy session across all eight commands must assert that each completed command is separated from a fresh Clean Sections menu by one blank line. Include a recoverable Engine failure that prints the existing `Error` and `Recovery` lines and then returns to that same menu.

- [x] **Step 2: Confirm application tests fail**

- [x] **Step 3: Implement app handlers and `ConsoleMain`**

Use four production UI classes only. Do not add a command framework, reflection dispatch, color, screen clearing, cancellation grammar, automatic save, or confirmation prompt.

- [x] **Step 4: Run all eleven exact JUnit classes**

Expected class names:

```text
DtoContractTest
LmsrCalculatorTest
MarketEventTest
GuessMarketEngineUseCaseTest
GuessMarketEngineXmlLoadTest
GuessMarketEnginePersistenceTest
JaxbMarketMapperTest
XmlMarketLoaderTest
ConsoleInputTest
ConsoleRendererTest
GuessMarketConsoleAppTest
```

- [x] **Step 5: Perform a temporary classpath console smoke test**

Run load, list, details, purchase, close, save, restore, and exit through compiled class directories. Use the supplied `multiple.xml` data so the transcript includes real variable-length descriptions and option labels. Inspect the transcript for the exact grouped menu, named sections, one-blank-line menu return, readable long text without truncation, no ANSI or screen clearing, and unchanged success and recovery meaning. This is development evidence only and does not replace packaged-process proof.

- [x] **Step 6: Update the walkthrough and commit**

```powershell
git add modules/guessmarket-ui docs/guides/IMPLEMENTATION-WALKTHROUGH.md
git commit -m "feat: implement console application flows"
```

- [x] **Step 7: Run the Stage 5 gate**

Review input recovery, the exact D-072 menu and prompts, all named block shapes, same-operation result use, filtering ownership, runtime-defect visibility, ASCII-only decoration, long supplied text, all eleven tests, the smoke transcript, and walkthrough entries before merging.

Historical D-072 completion record: current rebased Task 11 evidence is `bdb5abd` plus correction `3f3eec2`. Current rebased Task 12 evidence is `9fd2d5d` (`feat: implement console application flows`). The strict Java 25 gate passed 137 tests before manual review found D-073. That earlier green result remains valid historical evidence for D-072 but no longer completes the Stage 5 acceptance gate.

### Task 12A: Correct command completion pacing and audit every console conversation

**Files:**

- Modify: `modules/guessmarket-ui/src/main/java/guessmarket/ui/console/ConsoleInput.java`
- Modify: `modules/guessmarket-ui/src/main/java/guessmarket/ui/console/ConsoleRenderer.java`
- Modify: `modules/guessmarket-ui/src/main/java/guessmarket/ui/console/GuessMarketConsoleApp.java`
- Modify: `modules/guessmarket-ui/src/test/java/guessmarket/ui/console/ConsoleInputTest.java`
- Modify: `modules/guessmarket-ui/src/test/java/guessmarket/ui/console/ConsoleRendererTest.java`
- Modify: `modules/guessmarket-ui/src/test/java/guessmarket/ui/console/GuessMarketConsoleAppTest.java`
- Modify after verification: `docs/design/EXERCISE-1-DESIGN-BRIEF.md`
- Modify after verification: `docs/guides/TESTING.md`
- Modify after verification: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`
- Modify after verification: `PROJECT_HANDOFF.md`

**Interfaces:**

- Consumes: D-073, the existing full-line `Scanner` input boundary, the existing `ConsoleInput.EndOfInputException`, D-072 rendering, and checked `EngineOperationException` recovery.
- Produces: package-private `void ConsoleInput.waitForMenuReturn() throws EndOfInputException`, the revised static menu, and a command loop that pauses after every handled command attempt from 1 through 7.
- Preserves: four UI production classes, seven Engine operations, current DTOs, command numbering, filtering and actual-ID translation, exact result blocks, and visibility of unchecked programming defects.

- [x] **Step 1: Write the D-073 input and menu tests**

Add focused `ConsoleInputTest` methods with these exact calls and expectations:

```java
@Test
void returnToMenuAcceptsBlankAndWhitespaceOnlyLines() throws Exception {
    StringWriter blankText = new StringWriter();
    input("\n", blankText).waitForMenuReturn();
    assertEquals("Press Enter to return to the main menu:\n", blankText.toString());

    StringWriter whitespaceText = new StringWriter();
    input("   \n", whitespaceText).waitForMenuReturn();
    assertEquals("Press Enter to return to the main menu:\n", whitespaceText.toString());
}

@Test
void returnToMenuRejectsEveryNonblankLineWithoutTreatingItAsACommand() throws Exception {
    StringWriter text = new StringWriter();
    input("2\nword\n\n", text).waitForMenuReturn();
    assertEquals("""
            Press Enter to return to the main menu:
            Press Enter without typing a command.
            Press Enter to return to the main menu:
            Press Enter without typing a command.
            Press Enter to return to the main menu:
            """, text.toString());
}

@Test
void endOfInputAtReturnToMenuIsTheExistingCheckedSignal() {
    assertThrows(ConsoleInput.EndOfInputException.class,
            () -> input("", new StringWriter()).waitForMenuReturn());
}
```

Revise both menu literals in `ConsoleInputTest` and `ConsoleRendererTest` so they contain one blank line between `8. Exit` and `Choose a command [1-8]:`, and add `assertFalse(output.contains("Commands 2 through 6 require a loaded system."));` to the renderer test.

- [x] **Step 2: Write application-level RED tests for ordering and edge cases**

Update the happy-path script by adding one blank line after each completed command from 1 through 7. Replace the old repeated-menu assertion with an ordering assertion that each command result is followed by the exact pause prompt before the next menu.

Add focused tests equivalent to these exact cases:

```java
@Test
void recoverableEngineFailureWaitsForEnterBeforeRedrawingTheMenu() {
    FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
    engine.failWith(FakeGuessMarketEngine.LIST, failure(EngineErrorCode.NO_SYSTEM_LOADED));

    String output = run(engine, "2\n\n8\n");

    assertTrue(output.contains("Recovery: Load XML or restore saved state first.\n"
            + "Press Enter to return to the main menu:\n\n" + MENU));
}

@Test
void endOfInputAtReturnToMenuExitsWithoutRedrawingTheMenu() {
    FakeGuessMarketEngine engine = new FakeGuessMarketEngine();

    String output = run(engine, "2\n");

    assertEquals(1, occurrences(output, MENU));
    assertTrue(output.endsWith("Press Enter to return to the main menu:\n"
            + "Input closed. Exiting.\n"));
}

@Test
void nonblankReturnInputIsRetriedAndNeverExecutedAsTheNextCommand() {
    FakeGuessMarketEngine engine = new FakeGuessMarketEngine();

    String output = run(engine, "2\n8\n\n8\n");

    assertEquals(List.of("listEvents()"), engine.calls);
    assertTrue(output.contains("Press Enter without typing a command."));
    assertEquals(2, occurrences(output, MENU));
}

@Test
void exitDoesNotDisplayTheReturnToMenuPrompt() {
    String output = run(new FakeGuessMarketEngine(), "8\n");

    assertFalse(output.contains("Press Enter to return to the main menu:"));
    assertTrue(output.endsWith("Goodbye.\n"));
}
```

Keep invalid main-menu input pause-free and update the pre-load, empty-open-event, failed purchase, failed close, details, save, restore, and complete-session scripts so every handled command from 1 through 7 supplies its explicit return Enter. Preserve the existing EOF-at-secondary-prompt and unchecked-defect assertions.

- [x] **Step 3: Compile the three UI suites and prove RED for the approved reasons**

Use a new `build/d073-red` tree. Strictly compile DTO, Engine, UI, and all tests with Oracle Java 25.0.4, `--release 25 -encoding UTF-8 -Xlint:all -Werror`, the JUnit 6.1.1 runner, and the five approved JAXB runtime JARs. Run:

```powershell
$d073RedClasspath = @(
  'build\d073-red\classes\test'
  'build\d073-red\classes\dto'
  'build\d073-red\classes\engine'
  'build\d073-red\classes\ui'
  'modules\guessmarket-engine\src\main\resources'
  'modules\guessmarket-engine\src\test\resources'
  'tools\jaxb-ri-4.0.5\mod\angus-activation.jar'
  'tools\jaxb-ri-4.0.5\mod\jakarta.activation-api.jar'
  'tools\jaxb-ri-4.0.5\mod\jakarta.xml.bind-api.jar'
  'tools\jaxb-ri-4.0.5\mod\jaxb-core.jar'
  'tools\jaxb-ri-4.0.5\mod\jaxb-impl.jar'
)
& 'C:\Program Files\Java\jdk-25.0.4\bin\java.exe' `
  -jar tools\testing\junit-platform-console-standalone-6.1.1.jar execute `
  --class-path ($d073RedClasspath -join [IO.Path]::PathSeparator) `
  --select-class guessmarket.ui.console.ConsoleInputTest `
  --select-class guessmarket.ui.console.ConsoleRendererTest `
  --select-class guessmarket.ui.console.GuessMarketConsoleAppTest `
  --fail-if-no-tests --disable-banner --disable-ansi-colors --details=summary
```

Expected RED causes: `waitForMenuReturn()` is missing, the old static loaded-system sentence remains, and application transcripts redraw the menu without the approved pause. Any unrelated compiler or test failure must be diagnosed before production code changes.

- [x] **Step 4: Implement the minimal input and renderer correction**

Add this package-private method to `ConsoleInput`:

```java
void waitForMenuReturn() throws EndOfInputException {
    while (true) {
        writeLine("Press Enter to return to the main menu:");
        if (readLine().trim().isEmpty()) {
            return;
        }
        writeLine("Press Enter without typing a command.");
    }
}
```

In `ConsoleRenderer.renderMenu()`, delete only:

```java
writeLine("Commands 2 through 6 require a loaded system.");
writeLine("");
```

Keep the existing blank line after `8. Exit`, so the revised menu has exactly one blank line before its choice prompt.

- [x] **Step 5: Implement the minimal application-loop correction**

Keep the existing outer `EndOfInputException` catch. After reading a valid choice, execute command 8 immediately. For each valid choice from 1 through 7, execute the existing handler inside the checked Engine-error boundary, render any checked error, then call `input.waitForMenuReturn()`. Choice 0 from invalid main-menu parsing skips the pause. Do not catch `RuntimeException` or `Error`.

The control shape must remain equivalent to:

```java
int choice = input.readMenuChoice();
if (choice == 8) {
    renderer.renderGoodbye();
    return;
}
if (choice >= 1 && choice <= 7) {
    try {
        executeCommand(choice);
    } catch (EngineOperationException exception) {
        renderer.renderError(exception);
    }
    input.waitForMenuReturn();
}
```

If extracting `executeCommand(int choice)`, keep it private, use the existing numeric switch, and introduce no command abstraction or fifth production class.

- [x] **Step 6: Run focused GREEN and audit all UI conversations**

Recompile into a new `build/d073-green` tree and run the same three exact UI suites. Then inspect every command path for success, checked error, empty result, invalid secondary input, EOF, and unchecked failure. Confirm:

- Commands 1 through 7 pause after handled success, empty result, or checked Engine failure.
- Command 8 never pauses.
- Invalid menu input redraws without a pause.
- Invalid secondary input repeats only its own prompt.
- EOF at the menu, return pause, event selection, option selection, quantity, XML path, save path, and restore path prints one `Input closed. Exiting.` and exits normally.
- Nonblank pause input never becomes a command.
- Runtime defects remain uncaught and visible.
- No UI path adds or infers Engine loaded state.

- [x] **Step 7: Run real Engine and process-level audit scenarios**

Use the supplied `multiple.xml` fixture and compiled real `GuessMarketEngineImpl` to exercise load, list, details, purchase, close, save, restore, pre-load rejection, invalid input, EOF at pauses, and exit. Capture output under `build/d073-manual-audit`, which remains untracked. Verify the command 2 result contains all three event IDs and remains the final visible block until Enter. Search the transcript to prove the static loaded-system note is absent.

If a concrete Engine defect appears, record the exact input, Engine call, expected result, actual result, and state-preservation evidence. Do not change Engine production code under Task 12A without a separate approved correction.

- [x] **Step 8: Run the renewed strict all-eleven-suite gate**

Compile all production and test sources into a fresh `build/d073-final` directory using the strict Java 25 flags. Include `modules/guessmarket-engine/src/main/resources` and `modules/guessmarket-engine/src/test/resources` on the JUnit runtime classpath. Run all eleven test suites with `--scan-class-path`, `--include-engine junit-jupiter`, and `--fail-if-no-tests`. Require zero failures, skips, disabled tests, aborts, warnings from compilation, or missing suite names.

- [x] **Step 9: Update the durable Stage 5 record**

This step was completed for D-073: the design ledger and brief record the new method, menu behavior, EOF contract, regression tests, audit scenarios, exact test count, and the absence of a concrete Engine finding. After Nitzan's renewed approval, Stage 5 merged into `main` as `d8ef980`.

- [x] **Step 10: Verify, commit, and push for renewed pull request review**

Run `git diff --check`, inspect every staged path, confirm `.superpowers/` and every `build/` output remain untracked or ignored, and scan the staged diff for secrets or private absolute paths. Commit only the reviewed source, tests, and durable public documentation:

```powershell
git add modules/guessmarket-ui docs PROJECT_HANDOFF.md
git commit -m "fix: keep console results visible before menu return"
git push origin codex/e1-console-ui
```

Stage 5 was explicitly accepted by Nitzan, merged into `main` as `d8ef980`, and verified again from the merged tree with the strict 145-test Java 25 gate.

Task 12A approval record: Nitzan approved D-073 on 2026-08-17 and authorized its test-first implementation, full UI audit, separate Engine-defect reporting, renewed verification, documentation update, commit, and push to pull request 5.

Task 12A completion record: the RED compile failed with four expected missing-method errors. Focused GREEN passed 33 UI tests, including the final review regression proving direct `AssertionError` visibility. The renewed strict Java 25 all-eleven-suite gate passed all 145 tests with zero failures, skips, disabled tests, or aborts. Real Engine process audits passed the complete supplied-XML workflow, pre-load recovery, and exact EOF at the pause. No concrete Engine defect was found, and no Engine production source changed. The main correction is `9366bea`, the independent-review fix is `d737087`, and Stage 5 was accepted and merged into `main` as `d8ef980`. Task 12A is complete.

## 10. Stage 6: Authoritative build, proof verifier, and exact ZIP

Milestone branch: `codex/e1-packaging`

### Task 13: Implement repository-owned packaging inputs and `build.bat`

**Files:**

- Create: `build.bat`
- Create: `packaging/guessmarket-ui.mf`
- Create: `packaging/run.bat`
- Create locally and keep ignored: `private/submission/assignment-1/README.pdf`
- Modify: `docs/guides/BUILD-AND-RUN.md`
- Modify: `docs/guides/TESTING.md`
- Modify: `README.md`
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

**Interfaces:**

- Consumes: one Oracle Java 25 `JAVA_HOME`, all three module source trees, Engine resources, JUnit 6.1.1, five JAXB runtime JARs, and the private README PDF.
- Produces: clean classes, reports, three JARs, exact staging, fresh extraction, and `build/distributions/guess-market-exercise-1.zip`.

Create the manifest exactly with a final blank line:

```text
Manifest-Version: 1.0
Main-Class: guessmarket.ui.console.ConsoleMain
Class-Path: lib/guessmarket-engine.jar lib/guessmarket-dto.jar
  lib/jakarta.activation-api.jar lib/angus-activation.jar
  lib/jakarta.xml.bind-api.jar lib/jaxb-core.jar lib/jaxb-impl.jar

```

Create the launcher exactly:

```bat
@java -jar "%~dp0guessmarket-ui.jar"
```

`build.bat` must use `setlocal`, resolve the project through `%~dp0`, quote paths, validate the exact project-owned `build` target before cleaning, invoke `java.exe`, `javac.exe`, and `jar.exe` only beneath the same `JAVA_HOME`, verify major version 25, and stop on every required nonzero exit.

Implement plainly named phases in this order:

```text
preflight
clean
source lists
DTO compile
Engine compile and resource copy
UI compile
DTO test compile
Engine test compile
UI test compile
JUnit execution with captured output and XML reports
mandatory JUnit proof verifier
three application JARs
JAR validation and ownership inspection
exact staging
ZIP creation with jar --no-manifest
exact ZIP membership verification
fresh extraction under a path containing spaces
packaged-process checks
success summary
```

The JUnit invocation uses `execute`, `--scan-class-path`, `--include-engine junit-jupiter`, `--fail-if-no-tests`, disabled ANSI output, deterministic ASCII details, reports under `build/reports/junit`, and captured combined output. The post-test verifier fails unless all eleven exact class names executed at least one test and failures, container failures, skips, disabled tests, and aborts are all zero.

- [x] **Step 1: Create and review the private README build input**

Use the `documents` and `pdf` skills during execution. Create the Nitzan-approved 1-to-3-page PDF with run instructions, key classes, important decisions and assumptions, student details, GitHub link, and explicit bonus declaration. Visually verify it, then confirm Git ignores the file before allowing `build.bat` to consume it.

- [x] **Step 2: Add verifier failure probes before trusting it**

Run controlled probes for missing class, zero tests, one failing test, one disabled test, and one aborted test. Each probe must make the verifier fail. Remove probe source and output before the real build.

- [x] **Step 3: Implement and run the clean build through the test phase**

Expected: clean compilation, all eleven suites, valid reports, and successful mandatory verifier before any staging file exists.

- [x] **Step 4: Implement JAR creation and inspection**

Expected: DTO owns DTO only, Engine owns Engine plus generated XML and trusted XSD, UI owns UI plus exact manifest, and no application JAR contains tests, JUnit, or unpacked vendor classes.

- [x] **Step 5: Implement exact staging, ZIP, inventory, extraction, and process checks**

Expected ZIP root: `run.bat`, `README.pdf`, `guessmarket-ui.jar`, and `lib/`. Expected `lib` contents: two application library JARs and five runtime JAXB JARs only.

- [x] **Step 6: Update public guides and walkthrough**

Document exact build and run commands, dependency roles, generated outputs, report locations, and why staging is not the final verification target.

- [x] **Step 7: Commit the build slice**

```powershell
git add build.bat packaging docs/guides README.md
git commit -m "build: add authoritative Exercise 1 pipeline"
```

- [x] **Step 8: Run the Stage 6 gate**

Show a fresh successful `build.bat`, the JUnit proof summary, JAR inventories, manifest, ZIP inventory, extracted-process transcripts, privacy scan, and walkthrough before merging.

## 11. Stage 7: Exact-artifact acceptance and submission decision

Milestone branch: `codex/e1-final-verification`

### Task 14: Complete the eight gates without weakening external evidence

**Files:**

- Create locally and keep ignored: `private/submission/assignment-1/README.pdf`
- Create or update: an implementation verification record under `docs/reviews` with no personal or private content.
- Modify: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`
- Modify: `PROJECT_HANDOFF.md`
- Modify: outer workspace `PROJECT_HANDOFF.md` after the repository checkpoint is verified.

**Interfaces:**

- Consumes: the exact ZIP produced by a clean authoritative build and Nitzan-approved private README contents.
- Produces: an evidence-backed decision that the candidate is submission-ready or has a clearly named unresolved risk.

- [ ] **Step 1: Reopen and visually verify the private 1-to-3-page README PDF**

Confirm that it still contains run instructions, key classes, important decisions and assumptions, student details, GitHub link, and explicit bonus declaration. Verify page count, readability, and that the public repository does not track it.

- [ ] **Step 2: Rerun Gates 1 through 6 from a zero-based clean build**

Use only the exact extracted ZIP for packaged scenarios. Cover launch inside and outside the package directory, path with spaces, explicit exit, end-of-input, valid XML, invalid XML recovery, full integration conversation, persistence across a new process, and no raw stack trace for expected failures.

- [ ] **Step 3: Perform the one-time IntelliJ artifact cross-check**

Configure three separate artifacts and confirm logical class, resource, manifest, and dependency ownership. Do not copy IntelliJ output into the submission.

- [ ] **Step 4: Run the exact final ZIP on a clean Windows 10 machine**

Record the OS evidence, Java version, exact ZIP SHA-256, extraction location, launcher command, central transcript, and result. A CI runner, development machine, or Windows 11 result cannot silently replace this requirement.

- [ ] **Step 5: Perform the README-led grader walkthrough**

Start from another fresh copy of the same hash, follow only the README and `run.bat`, inspect exact contents, verify the documented GitHub link, and record the result.

- [ ] **Step 6: Run final source, privacy, and public-access review**

Inspect tracked files, ignored private paths, runtime `.ser` exclusion, generated source tracking, custom fixture tracking, build inputs, meaningful commits, secrets scan, full diff, and clean status. After this public-safety review passes, stop for Nitzan to change the repository visibility manually from private to public. Then verify that the GitHub link and intended `main` branch content open from a logged-out or otherwise unauthenticated view.

- [ ] **Step 7: Update the walkthrough from planned to completed behavior**

Every public and meaningful package-private method must have a responsibility, stage, test owner, and explanation. Add the final end-to-end runtime flow and remaining limitations.

- [ ] **Step 8: Stop for explicit final approval**

Present the exact ZIP hash and all evidence. Only after Nitzan explicitly approves may the same artifact be copied to `submissions/assignment-1` and Drive, tagged `exercise-1-submission`, or submitted.

## 12. Plan self-review checklist

- [x] Every D-001 through D-072 implementation requirement maps to a task or final gate.
- [x] D-067 zero-based account and profit, subsidy, or break-even output map to domain, renderer, persistence, and final tests.
- [x] D-068 direct delta and binary64 limits map to calculator, domain atomicity, and simulator evidence.
- [x] D-069 full-range IDs and text rules map to custom fixtures, mapper, Engine, UI translation, and persistence.
- [x] D-070 direct-domain graph, filter, alias checks, move behavior, and honest guarantees map to Task 9.
- [x] D-071 UI proof ownership, all eleven classes, mandatory verifier, and clean Windows 10 evidence map to Tasks 10 through 14.
- [x] D-072 Clean Sections menu, prompts, section grammar, portability limits, transcript proof, and rejected-direction boundaries map to Tasks 11 and 12 plus the Stage 5 gate.
- [x] All seven Engine methods and all sixteen error codes have an implementation and automated proof owner.
- [x] No Exercise 2 or Exercise 3 feature entered the file map.
- [x] No unapproved framework, module, fat JAR, public API wrapper, or console-specific Engine method entered the plan.
- [x] The walkthrough is updated in the same implementation slice as each method.
- [x] No candidate is copied, tagged, published, or submitted without the explicit final gate.

## 13. Execution handoff

Nitzan approved this plan on 2026-08-15 and selected staged inline execution with the `executing-plans` skill. Execution stops at each of the seven stage gates for his review. Subagent-driven execution remains available only if Nitzan explicitly requests delegation, and it must preserve the same stage reviews and approval boundaries.
