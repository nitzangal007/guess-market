# Guess Market Project Handoff

## Mode

Implementation review. Stages 1 through 3 are accepted and merged. Stage 4 Tasks 9 and 10 are independently reviewed, verified, accepted, and squash-merged into `main` as `9a8c87c`. Stage 5 is implemented and independently reviewed on `codex/e1-console-ui`, follows the approved D-072 Clean Sections console presentation, and has been successfully rebased directly onto `9a8c87c`. Stage 5 remains unmerged and unpushed, and now awaits Nitzan's Stage 5 review and acceptance.

## Current state

- This folder is the dedicated Guess Market repository root.
- The private repository is published at `https://github.com/nitzangal007/guess-market` with `main` as its default branch.
- The outer workspace split commit is `df4219e0406d2b0315f739b737f28d126a80d2c2`.
- The standalone root commit is `d006eaf45910d4cdc164d800d07e70d697d0eb90`.
- A fresh private clone reproduced the approved 22-file baseline with no `provided` or `private` directory and only the approved JAXB subset.
- The approved design passed its pre-implementation review with no blocking finding.
- Documentation is organized under `docs/design`, `docs/planning`, `docs/guides`, `docs/reviews`, and `docs/repository`.
- Course originals, private submission files, raw internal review evidence, build output, runtime `.ser` files, and identity-bearing archives remain untracked.
- Stage 1 contains seven immutable DTO production types, the supported seven-method Engine interface, sixteen structured error codes, and one checked Engine exception contract.
- `DtoContractTest` has eight tests, and the initial public-contract portion of `GuessMarketEngineUseCaseTest` has four tests. All 12 remain in the Stage 2 regression gate.
- JUnit Platform Console Standalone 6.1.1 is vendored only under `tools/testing`; its module identity, license material, notice material, and SHA-256 are verified and documented.
- Stage 1 implementation commits are `219738d`, `2d4e249`, `d628b14`, and review correction `0912d68`.
- The independent code review found no critical issue. Its one important floating-point finding was reproduced, fixed with a documented eight-ULP comparison, and independently rechecked with no remaining critical or important issue.
- Nitzan accepted Stage 1 on 2026-08-15. GitHub merged pull request 1 as squash commit `d8d9b0c` because the repository permits squash merges only.
- The Stage 2 baseline was compiled with Oracle Java 25.0.4 and reran all 12 Stage 1 tests successfully before new implementation.
- Task 4 implemented the package-private stateless `LmsrCalculator` in commits `542768d` and `cf14539`. Its direct delta preserves representable tiny values, rejects free positive shares, and now combines large opposing quantity terms in `long` before conversion so cancellation does not erase low-order information.
- Task 5 implemented the serializable event aggregate, accounting, history, immutable DTO conversion, and atomic purchase and close transitions in commits `7502964`, review fix `e0a341a`, and completion record `3daadb1`.
- The original Stage 2 whole-branch review found one Important numerical precision defect, one Important stale-handoff defect, and three Minor evidence or detail gaps. The residual re-review then found a second Important numerical precision defect and later documentation-only Minor corrections. The completed corrections extend the combined-long numerical branch to every negative quantity difference, add reachable event accounting proofs for both cancellation boundaries, prove the positive commission-underflow identity, document operation-specific lifecycle and numerical details, complete `Integer.MIN_VALUE` observation coverage, and align the planning and walkthrough records with the implemented code.
- The current focused Stage 2 gate has 16 `LmsrCalculatorTest` cases and 20 `MarketEventTest` cases. The complete fresh regression and boundary gate is recorded in the walkthrough and final fix report.
- Stage 5 UI production source and its three UI test suites now exist: `ConsoleInputTest`, `ConsoleRendererTest`, and `GuessMarketConsoleAppTest`. The current strict Java 25 all-eleven-suite result is 137 successful tests. `build.bat`, IntelliJ configuration, packaging inputs, and release artifacts remain planned later-stage work.
- Nitzan reviewed and accepted Stage 2 on 2026-08-16. Pull request 2 was squash-merged as `6842dfd`.
- Stage 3 was accepted and squash-merged as full commit `15c8c8f70d21b4a2e8021ef6b7d3dfb88bac2caf`.
- Task 6 added the trusted XSD, supplied and custom fixtures, and eight retained XJC-generated sources. Task 7 added ordered JAXB-to-domain candidate mapping. Task 8 added trusted-schema XML loading and structured error translation.
- The Stage 3 review correction classifies invalid UTF-8 parser decoding as `XML_STRUCTURE_INVALID` while preserving genuine input open and read failures as `XML_FILE_ACCESS_FAILED`.
- The final Stage 3 strict Java 25 gate used exactly the five approved JAXB runtime JARs and all six current suites: 74 tests found, started, and successful, with zero failures, skips, or aborts.
- Stage 4 Task 9 implemented direct-domain persistence in commits `5eb202b` and `6b722ac`.
- Stage 4 Task 10 defined complete public Engine behavior in RED commit `706df9a` and implemented it in `f536bdb`.
- Task 10 review fixes centralize complete DTO equality with exact binary64 comparisons and prove load and restore replacement atomicity for injected `XML_FILE_ACCESS_FAILED`, `ENGINE_CONFIGURATION_ERROR`, and `STATE_FILE_ACCESS_FAILED` failures.
- The final Stage 4 persistence review fix `acf3c36` validates raw saved graph elements before casting, correctly separates stream-opening access failures from serialization-format failures, rejects trailing `null` data as `SAVED_STATE_INVALID`, and records the completed Task 9 and Task 10 plan steps.
- The final independent strict Java 25 gate uses exactly the five approved JAXB runtime JARs and all eight suites: 112 tests found, started, and successful, with zero failures, skips, disabled tests, or aborts. A final persistence re-review found no remaining Critical, Important, or Minor issue.
- Stage 4 is independently verified, accepted, and squash-merged into `main` as `9a8c87c` (`9a8c87c1a291fe58253a55140dafb710c34a5299`).
- Nitzan approved D-072 on 2026-08-17 after comparing Clean Sections, Command Center, and Minimal Transcript console mockups. Clean Sections is the controlling Stage 5 presentation direction.
- Stage 5 Tasks 11 and 12 are implemented and independently reviewed on `codex/e1-console-ui`, successfully rebased directly onto `9a8c87c`. Current rebased Task 11 evidence is `bdb5abd` plus correction `3f3eec2`; current rebased Task 12 evidence is `9fd2d5d` (`feat: implement console application flows`). The current known strict Java 25 all-eleven-suite result is 137 successful tests with zero failures, skips, disabled tests, or aborts. Stage 5 production UI is unchanged by the final renderer-coverage correction.
- D-072 keeps the eight commands and existing Engine behavior, while grouping the menu under `DATA`, `TRADING`, and `STATE AND SESSION` and using named 60-character ASCII output sections.
- `docs/planning/IMPLEMENTATION-PLAN.md` contains the approved staged implementation plan and effort estimate.
- `docs/guides/IMPLEMENTATION-WALKTHROUGH.md` records every implemented Stage 1 through Stage 5 calculation, constructor, transition, observation, console conversation, and verification result.

## Authority

- Complete approved design: `docs/design/EXERCISE-1-DESIGN.md`
- Concise design entry point: `docs/design/EXERCISE-1-DESIGN-BRIEF.md`
- Repository policy: `docs/repository/GITHUB-REPOSITORY-DESIGN.md`
- Documentation map: `docs/README.md`
- Detailed implementation plan: `docs/planning/IMPLEMENTATION-PLAN.md`
- Living implementation walkthrough: `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`

## Fixed implementation boundaries

- Java 25
- Three modules: DTO, Engine, and UI
- UI depends on Engine and DTO; Engine depends on DTO; DTO depends only on the JDK
- JAXB/XJC remains Engine-private integration technology
- One repository-owned Windows Batch build is authoritative for compilation, testing, packaging, and verification
- Three separate non-fat application JARs
- Exact final ZIP verification includes a clean Windows run and manual grader walkthrough

## Exact next action

The Stage 5 branch is successfully rebased directly onto `9a8c87c`. Keep it unmerged and unpushed while Nitzan performs the Stage 5 review and decides whether to accept it.
