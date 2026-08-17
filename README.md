# Guess Market

Guess Market is a Java 25 prediction-market simulator designed around LMSR pricing, modular architecture, XML/JAXB loading, automated testing, Java serialization, and reproducible Windows packaging.

## Current status

Exercise 1 design is complete through decision D-071 and passed a full pre-implementation review with no blocking design findings. Stages 1 and 2 are accepted and merged. Stage 3 is implemented and verified on its milestone branch, including retained JAXB source, trusted XML resources, ordered business mapping, and trusted-schema loading. It awaits Nitzan's review and approval. Persistence, UI, the authoritative build, and release artifacts remain later stages.

## Planned architecture

The project uses three modules with one-directional dependencies:

```text
guessmarket-ui -> guessmarket-engine -> guessmarket-dto
       |                  |
       +----------------->+
```

- `guessmarket-dto` owns immutable values shared across module boundaries.
- `guessmarket-engine` owns market behavior, XML loading, LMSR calculations, persistence, and the supported engine interface.
- `guessmarket-ui` owns console input, output, menus, recovery conversations, and application startup.

Each module will produce a separate non-fat JAR. The UI JAR will be the only executable application JAR.

## Exercise 1 scope

- Load and validate market definitions from XML through JAXB-generated mapping classes.
- List markets and show detailed market state.
- Purchase shares through stable LMSR calculations.
- Close markets and report the final market-maker profit, subsidy, or break-even result.
- Save and restore complete Engine state through the optional serialization bonus.
- Build, test, inspect, package, extract, and run the exact Windows submission artifact reproducibly.

## Requirements

- Oracle JDK 25
- Windows for the authoritative `build.bat` and final package verification
- Repository-vendored JAXB 4.0.5 dependencies
- Repository-vendored JUnit Platform Console Standalone 6.1.1 dependency for tests only

The authoritative build does not download dependencies.

## Documentation

Start with the [documentation index](docs/README.md).

- [Architecture](docs/design/ARCHITECTURE.md)
- [Exercise 1 design brief](docs/design/EXERCISE-1-DESIGN-BRIEF.md)
- [Complete Exercise 1 design](docs/design/EXERCISE-1-DESIGN.md)
- [Exercise 1 implementation plan](docs/planning/IMPLEMENTATION-PLAN.md)
- [Living implementation walkthrough](docs/guides/IMPLEMENTATION-WALKTHROUGH.md)
- [Build and run](docs/guides/BUILD-AND-RUN.md)
- [Testing strategy](docs/guides/TESTING.md)
- [JAXB and XML](docs/guides/JAXB-AND-XML.md)
- [Design review resolutions](docs/reviews/DESIGN-REVIEW-RESOLUTIONS.md)
- [Repository design](docs/repository/GITHUB-REPOSITORY-DESIGN.md)

The detailed plan was approved on 2026-08-15. Implementation proceeds through seven human review checkpoints.

## Repository boundaries

Course handouts, private submission documents, runtime `.ser` files, generated build output, and identity-bearing submission ZIP files remain outside Git. The repository includes only project-owned source and documentation, required test fixtures, reproducible build inputs, and the minimal vendored dependency set with its license material.

## License

No open-source license is granted for the project code during the active course. Vendored third-party components retain their own licenses.
