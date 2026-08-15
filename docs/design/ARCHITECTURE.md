# Architecture

## Overview

Guess Market is designed as three Java 25 modules with a narrow, one-directional dependency graph.

```text
guessmarket-ui -> guessmarket-engine -> guessmarket-dto
       |                  |
       +----------------->+
```

The UI owns interaction. The Engine owns behavior and state. The DTO module owns immutable values that cross the supported module boundary.

## Modules

### DTO

Package: `guessmarket.dto`

The DTO module contains immutable summaries, details, option snapshots, receipts, and history entries. It depends only on the JDK and contains no console, XML, JAXB, persistence, or Engine implementation logic.

### Engine

Packages:

- `guessmarket.engine`
- `guessmarket.engine.xml`
- `guessmarket.engine.xml.generated`

The supported API is the `GuessMarketEngine` interface, its structured checked failure contract, and DTO return values. `GuessMarketEngineImpl` owns the ordered market collection and coordinates domain operations. Domain objects enforce lifecycle and arithmetic invariants. `LmsrCalculator` is stateless.

The XML boundary is private to the Engine. `XmlMarketLoader` owns file, schema, and JAXB mechanics. `JaxbMarketMapper` validates and converts a complete generated graph into application-owned domain objects. Live Engine state is replaced only after the complete candidate succeeds.

Bonus persistence also remains private to the Engine. It serializes one versioned `SavedState` root around the direct domain graph, validates a complete candidate during restore, and replaces live state once.

### UI

Package: `guessmarket.ui.console`

`ConsoleMain` creates the concrete Engine once and passes it through the interface. `GuessMarketConsoleApp` owns command conversations. `ConsoleInput` owns full-line parsing. `ConsoleRenderer` owns output and fixed two-decimal `Locale.US` financial formatting.

The UI filters open events for purchase and close menus, assigns temporary one-based display positions, and translates the selected position back to the actual full-range event ID. The Engine remains independent of console numbering.

## Main runtime flows

### XML load

1. The UI supplies a path to the Engine interface.
2. The Engine delegates file, schema, and JAXB work to the XML loader.
3. The mapper builds and validates a fresh ordered domain collection.
4. The Engine replaces the live collection through one reference assignment.
5. Any failure preserves the earlier live state and returns a structured application outcome.

### Purchase

1. The UI selects an open event and option.
2. The Engine validates the actual ID, lifecycle, amount, and representability.
3. The domain calculates the direct cancellation-safe LMSR cost delta.
4. Quantities, commission, account value, and history commit atomically.
5. The Engine returns an immutable receipt and updated details.

### Close

1. The Engine validates the event and winning option.
2. It debits the aggregate net winner payout once.
3. It preserves the final market-maker account value.
4. The UI presents a profit, subsidy, or break-even result from the unrounded sign.

### Save and restore

Save writes a unique temporary sibling, closes it, attempts an atomic publication move, and may use an explicitly non-atomic replacement fallback. Restore applies a strict deserialization filter, fully validates one candidate graph, reconstructs a fresh ordered map, and commits once.

## Build and package boundary

The authoritative Windows build will compile DTO, Engine, and UI in dependency order, run the mandatory test verifier, create three non-fat JARs, inspect their contents, stage the exact runtime layout, create the exact ZIP, extract it fresh, and run packaged-process checks. IntelliJ and future CI are independent cross-checks, not submission authority.
