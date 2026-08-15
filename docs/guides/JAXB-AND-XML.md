# JAXB and XML

## Purpose

JAXB is an Engine-private integration mechanism. Generated mapping classes represent the supplied XML schema, while handwritten application code owns validation, domain construction, behavior, and the supported Engine API.

## Generation workflow

1. Keep the approved `xjc-run.bat` beside the seven JAXB `mod` JARs.
2. Run the wrapper with package `guessmarket.engine.xml.generated` and the trusted schema.
3. Verify the complete generated package tree.
4. Copy it under `modules/guessmarket-engine/src/main/java/guessmarket/engine/xml/generated`.
5. Keep generated source in Git and never edit it manually.
6. Regenerate only when the schema changes.

The XJC and JXC JARs are development tools. Only the five runtime JAXB JARs enter the application runtime classpath.

## Runtime boundary

- `XmlMarketLoader` owns path checks, stream ownership, schema setup, JAXB unmarshalling, and low-level exception translation.
- `JaxbMarketMapper` owns application validation and conversion into a fresh ordered domain collection.
- Generated classes never appear in DTOs or the supported Engine API.
- The Engine retains no reference to the generated JAXB graph.

## Atomic loading

The loader and mapper complete schema validation, generated-graph interpretation, application validation, and candidate-domain construction before the Engine changes live state. One final map-reference assignment commits the candidate. Any failure leaves the earlier live state unchanged.

## Approved validation rules

- Event IDs may use the complete `xs:int` range and must be unique across the candidate.
- Every event has exactly two ordered options.
- Commission is inclusive from `0` through `90`.
- LMSR `b` is positive.
- Empty or whitespace-only source text and duplicate option labels remain valid where the schema permits them.
- XML event order is preserved.
- Generated XML types are converted to application-owned domain values.

## Failure contract

Low-level path, I/O, schema, and JAXB failures become structured Engine outcomes with a stable error code, clear detail, recovery hint, and available location context. Programming defects remain visible and are not disguised as user input errors.
