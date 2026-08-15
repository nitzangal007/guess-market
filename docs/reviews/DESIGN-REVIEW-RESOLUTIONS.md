# Design Review Resolutions

## Review result

The pre-implementation review audited the complete Exercise 1 contract, decisions D-001 through D-066, cross-section consistency, packaging evidence, XML fixtures, and 156 lecturer-checklist criteria. Thirteen findings were identified and resolved through decisions D-067 through D-071. The final review contains zero blocking design findings and declares the project ready for implementation planning.

## Material corrections

### D-067: market-maker account semantics

The event account is net cash starting at `0.0`. Purchases credit base cost and applicable commission. Close debits the aggregate net winning payout once and preserves the final balance. Positive means profit, negative means subsidy, and exact zero means break-even. The derived `b * ln(2)` maximum subsidy is never account cash.

### D-068: cancellation-safe LMSR delta

Purchase cost uses a direct stable log-domain delta rather than subtracting two large rounded totals. Representable tiny positive results remain positive. A required-positive amount that cannot be represented in binary64 is rejected atomically instead of becoming a free purchase.

### D-069: source-bounded XML validation

Event IDs accept the full `xs:int` range and require candidate-wide uniqueness. Unsupported restrictions on nonblank text and distinct option labels were removed. Exactly two ordered options, commission bounds, positive `b`, schema structure, and XML order remain enforced.

### D-070: direct-domain persistence

The serialization bonus uses one private Engine-owned versioned root around the direct domain graph. Restore applies a strict class filter and depth limit, validates a complete candidate, reconstructs a fresh ordered map, and commits once. Save attempts atomic publication and documents the explicitly non-atomic replacement fallback honestly.

### D-071: proof and packaging gates

The final decision closes traceability and verification gaps. All eleven required JUnit classes must execute, the report verifier rejects missing or incomplete evidence, the exact ZIP is inspected and tested after extraction, and a clean Windows run remains mandatory before submission readiness can be claimed.

## Remaining evidence

The review approved the design, not the implementation. Source, tests, build scripts, JARs, ZIP contents, IntelliJ artifacts, GitHub history, clean-Windows execution, and the final grader walkthrough still require executable evidence during later phases.
