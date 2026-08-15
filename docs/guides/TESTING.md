# Testing Strategy

## Status

The complete testing strategy is approved. The Stage 1 test foundation, `DtoContractTest`, and the public-contract portion of `GuessMarketEngineUseCaseTest` are implemented.

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

`GuessMarketEngineUseCaseTest` begins in Stage 1 with four tests for the public interface, exact error-code set, checked exception shape, and structured optional context. Stage 4 extends this same approved class with complete Engine operation behavior rather than creating another test class.

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

IntelliJ artifacts are checked once as a secondary comparison. Future GitHub Actions will invoke `build.bat` on Windows. Neither replaces the mandatory clean-Windows run of the exact final ZIP.

### Manual evidence

The final candidate receives a README-led grader walkthrough, final privacy review, exact ZIP SHA-256 record, and explicit approval before submission.

## Important boundaries

- XML schema validity and application business validity are tested separately.
- Valid full-range `xs:int` IDs, duplicate IDs, empty text, duplicate labels, exactly two ordered options, commission boundaries, and paths with spaces receive tracked fixtures.
- Numeric tests cover normal, cancellation-sensitive, extreme-imbalance, representable tiny-positive, and unrepresentable required-positive cases.
- Failed load, purchase, close, save, and restore operations preserve the required live state.
- Runtime `.ser` files and build output remain untracked.
