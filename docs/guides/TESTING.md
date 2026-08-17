# Testing Strategy

## Status

The complete testing strategy is approved. Stages 1 through 3 are accepted and merged. Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c`. Through Stage 5, source-level verification compiles DTO, Engine, and UI production and test source with Oracle Java 25, `--release 25 -encoding UTF-8 -Xlint:all -Werror`, JUnit Platform Console Standalone 6.1.1, and exactly the five approved JAXB runtime JARs. All eleven approved suites are implemented and passing; the renewed D-073 strict Java 25 gate reports 145 successful tests with zero failures, skips, disabled tests, or aborts. Stage 5 is implemented on `codex/e1-console-ui` directly above `9a8c87c` and ready to update draft pull request 5. It remains unmerged while awaiting Nitzan's renewed Stage 5 review and acceptance. Packaging and exact-artifact checks remain planned Stage 6 and Stage 7 work.

## Vendored test dependency

JUnit is a development-only dependency. It compiles and executes test source, but it is not copied into any production module and will not enter the runtime ZIP.

| Item | Verified value |
| --- | --- |
| Maven coordinate | `org.junit.platform:junit-platform-console-standalone:6.1.1` |
| Source | `https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/6.1.1/junit-platform-console-standalone-6.1.1.jar` |
| Local path | `tools/testing/junit-platform-console-standalone-6.1.1.jar` |
| Size | `2,996,922` bytes |
| SHA-256 | `7B16416E5727C645105C31B533440397F20DF68F6AE850C6BFD0CE1D88DB66C3` |
| JDK inspection | Oracle `jar` 25.0.4 identifies automatic module `org.junit.platform.console.standalone@6.1.1` |
| Legal evidence | `LICENSE.txt` and `NOTICE.txt` are exact copies of the JAR's embedded `META-INF/LICENSE.md` and `META-INF/LICENSE-notice.md` |

The standalone JAR contains JUnit Platform, Jupiter, Vintage, and their bundled support libraries so the repository can compile and execute tests without introducing Maven or Gradle. Only Jupiter is used for the project's tests.

## Test topology

The implementation plan must cover eleven behavior-centered test classes:

1. DTO contracts
2. LMSR calculations
3. Domain state and transitions
4. Engine use cases
5. Engine XML loading
6. JAXB mapping
7. XML loader mechanics
8. Persistence
9. Console input
10. Console rendering
11. Complete console conversations

UI tests use a handwritten fake Engine. Domain collaborators may use focused same-package tests. Production visibility must not be widened for tests, and tests must not inspect private fields through reflection.

`GuessMarketEngineUseCaseTest` now contains eleven tests for the public interface, exact error-code set, checked exception shape, structured optional context, loaded-state rules, ordered listing, details, purchases, closes, and failure atomicity. Stage 4 completed that approved class without creating another test class. The implemented UI suites cover console input, renderer output, and complete application conversations.

The D-073 correction adds eight UI tests without creating another test class. `ConsoleInputTest` has 8 tests, `ConsoleRendererTest` has 10, and `GuessMarketConsoleAppTest` has 15. They cover blank and whitespace-only return pauses, repeated nonblank retry, the revised menu, result-before-pause-before-menu ordering, checked recovery, empty results, invalid main-menu input, immediate exit, direct `RuntimeException` and `AssertionError` visibility, and EOF at the main menu, return pause, event selection, both option prompts, quantity, XML path, save path, and restore path. The focused gate passes all 33 UI tests.

## Evidence layers

### Unit and integration evidence

JUnit Jupiter covers DTO immutability, mathematical behavior, state transitions, Engine atomicity, XML validation, persistence validation, and console behavior.

### Authoritative build evidence

The build must prove that all eleven required classes executed with at least one test each and that the complete report contains zero failures, skips, disabled tests, and aborts. The verifier itself must be tested against deliberately incomplete or failing reports.

### Artifact evidence

The build inspects JAR ownership, manifests, resources, third-party exclusions, and exact ZIP membership.

### Packaged-system evidence

The real launcher and extracted JAR graph must run representative success and recovery conversations from the extracted package, not from development classes.

### Independent environment evidence

Through Stage 5, direct strict `javac` and JUnit Console commands are the verified source-level workflow. The later authoritative `build.bat`, GitHub Actions, IDE comparison, and clean-Windows exact-ZIP run remain separate Stage 6 and Stage 7 gates.

D-073 real-process evidence uses the supplied `multiple.xml` with the real Engine. The complete workflow exits 0, renders all three event IDs, pauses before every menu return, completes purchase, close, save, and restore, and proves the restored event remains closed. A separate pre-load process preserves the structured `NO_SYSTEM_LOADED` recovery and pauses before return. An exact redirected-input process closes standard input at the return pause, exits 0 with empty standard error, prints `Input closed. Exiting.`, and does not redraw the menu. The audit found no concrete Engine defect and changed no Engine production source.

### Manual evidence

The final candidate receives a README-led grader walkthrough, final privacy review, exact ZIP SHA-256 record, and explicit approval before submission.

## Important boundaries

- XML schema validity and application business validity are tested separately.
- Valid full-range `xs:int` IDs, duplicate IDs, empty text, duplicate labels, exactly two ordered options, commission boundaries, and paths with spaces receive tracked fixtures.
- Numeric tests cover normal, cancellation-sensitive, extreme-imbalance, representable tiny-positive, and unrepresentable required-positive cases.
- Failed load, purchase, close, save, and restore operations preserve the required live state.
- Runtime `.ser` files and build output remain untracked.
