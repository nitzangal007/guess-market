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

## Verified Task 6 derivation evidence

Task 6 copied the canonical `GM-EX1-Schema.xsd` to the Engine production resource path and copied `single.xml`, `multiple.xml`, `error-2.xml`, and `error-3.xml` byte-for-byte to `fixtures/supplied`. The source XSD and retained resource copy both have SHA-256 `EE3B60ED4A4806F571FD6EED769D0DF10C6461B99C5CECEB4AA78C4A370E17B1`; the four supplied fixture hashes match `provided/assignment-1/test-files/README.md` exactly.

The intact course JAXB RI 4.0.5 wrapper generated the retained Engine-private package from a temporary byte-identical schema copy with:

```text
xjc-run.bat -p guessmarket.engine.xml.generated GM-EX1-Schema.xsd
```

The retained generated files are `Comision.java`, `GMEvent.java`, `GMEvents.java`, `GMLMSR.java`, `GMMethod.java`, `GMOptions.java`, `GuessMarket.java`, and `ObjectFactory.java`. Every file declares `package guessmarket.engine.xml.generated;` and exactly matches its wrapper staging source by SHA-256. Generated files are owned by this XSD and command, are retained in Git for the course build, and are never hand-edited.

The eight generated sources compiled with Oracle Java 25.0.4 using `--release 25 -encoding UTF-8 -Xlint:all -Werror` and exactly the five approved runtime JARs: `jakarta.activation-api.jar`, `angus-activation.jar`, `jakarta.xml.bind-api.jar`, `jaxb-core.jar`, and `jaxb-impl.jar`. The authoritative elevated compile completed with exit code `0`, eight discovered sources, and no warnings or errors. The initial sandbox-only temporary-output denial is recorded in the Task 6 report and was not treated as the final compile result.

Custom tracked fixtures under `fixtures/custom` cover D-069 full-range and nonpositive IDs, duplicate nonpositive IDs, present empty text and duplicate labels, one-option business-invalid data, commission bounds, positive `b`, a filename containing spaces, and missing-versus-empty schema cases. Task 7 owns mapper proofs and Task 8 owns loader proofs; this resource-generation task does not implement either class.

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
