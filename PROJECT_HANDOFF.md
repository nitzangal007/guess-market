# Guess Market Project Handoff

## Mode

Review. Stage 2 Tasks 4 and 5 are implemented and verified on `codex/e1-lmsr-domain`. The whole-branch review correction and residual numerical correction are complete. Stop at the Stage 2 human checkpoint before any Stage 3 work.

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
- The Stage 2 whole-branch review found two important numerical precision defects and three minor evidence or detail gaps. The corrections extend the combined-long numerical branch to every negative quantity difference, add reachable event accounting proofs for both cancellation boundaries, prove the positive commission-underflow identity, document operation-specific lifecycle and numerical details, and complete `Integer.MIN_VALUE` observation coverage.
- The current focused Stage 2 gate has 16 `LmsrCalculatorTest` cases and 20 `MarketEventTest` cases. The complete fresh regression and boundary gate is recorded in the walkthrough and final fix report.
- XML, persistence, UI, `build.bat`, generated JAXB project source, IntelliJ configuration, packaging inputs, and release artifacts do not exist yet.
- `docs/planning/IMPLEMENTATION-PLAN.md` contains the approved staged implementation plan and effort estimate.
- `docs/guides/IMPLEMENTATION-WALKTHROUGH.md` records every implemented Stage 1 and Stage 2 calculation, constructor, transition, observation, and verification result.

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

Review the completed Stage 2 branch, its final fix report, numerical evidence, domain behavior, and walkthrough. Do not start Stage 3 until Nitzan accepts this checkpoint.
