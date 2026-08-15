# Guess Market Project Handoff

## Mode

Review. Stage 1 is implemented and verified on `codex/e1-project-foundation`. Work is stopped at the first human checkpoint for Nitzan's review before Stage 2.

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
- `DtoContractTest` has eight tests, and the initial public-contract portion of `GuessMarketEngineUseCaseTest` has four tests. All 12 pass with no failures, skips, disabled tests, or aborts.
- JUnit Platform Console Standalone 6.1.1 is vendored only under `tools/testing`; its module identity, license material, notice material, and SHA-256 are verified and documented.
- Stage 1 implementation commits are `219738d`, `2d4e249`, `d628b14`, and review correction `0912d68`.
- The independent code review found no critical issue. Its one important floating-point finding was reproduced, fixed with a documented eight-ULP comparison, and independently rechecked with no remaining critical or important issue.
- Domain behavior, LMSR, XML, persistence, UI, `build.bat`, generated JAXB project source, IntelliJ configuration, packaging inputs, and release artifacts do not exist yet.
- `docs/planning/IMPLEMENTATION-PLAN.md` contains the approved staged implementation plan and effort estimate.
- `docs/guides/IMPLEMENTATION-WALKTHROUGH.md` records every implemented Stage 1 constructor, getter group, Engine operation, and verification result.

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

Review the Stage 1 implementation, tests, public signatures, and walkthrough. Do not begin Stage 2 until Nitzan explicitly accepts this checkpoint.
