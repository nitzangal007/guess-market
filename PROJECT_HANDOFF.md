# Guess Market Project Handoff

## Mode

Repository migration. The Exercise 1 design is approved through D-071 and implementation has not started.

## Current state

- This folder is the dedicated Guess Market repository root.
- The repository remains private during the active Java course.
- The approved design passed its pre-implementation review with no blocking finding.
- Documentation is organized under `docs/design`, `docs/planning`, `docs/guides`, `docs/reviews`, and `docs/repository`.
- Course originals, private submission files, raw internal review evidence, build output, runtime `.ser` files, and identity-bearing archives remain untracked.
- Java source roots, build files, generated JAXB project source, IntelliJ configuration, tests, and packaging inputs do not exist yet.

## Authority

- Complete approved design: `docs/design/EXERCISE-1-DESIGN.md`
- Concise design entry point: `docs/design/EXERCISE-1-DESIGN-BRIEF.md`
- Repository policy: `docs/repository/GITHUB-REPOSITORY-DESIGN.md`
- Documentation map: `docs/README.md`

## Fixed implementation boundaries

- Java 25
- Three modules: DTO, Engine, and UI
- UI depends on Engine and DTO; Engine depends on DTO; DTO depends only on the JDK
- JAXB/XJC remains Engine-private integration technology
- One repository-owned Windows Batch build is authoritative for compilation, testing, packaging, and verification
- Three separate non-fat application JARs
- Exact final ZIP verification includes a clean Windows run and manual grader walkthrough

## Exact next action

Create and approve `docs/planning/IMPLEMENTATION-PLAN.md` from the reviewed D-071 design. Do not create Java source, configuration, or implementation branches before that approval.
