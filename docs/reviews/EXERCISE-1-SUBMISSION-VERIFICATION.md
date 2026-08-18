# Exercise 1 Submission Verification

## Scope and decision boundary

This record tracks Stage 7 verification for the exact Exercise 1 candidate artifact. It separates local evidence from evidence that must be collected manually. It does not authorize publication, tagging, copying to a submission location, or submission.

Candidate ZIP SHA-256:

```text
B5288F21DD3B7A10940427013368E3B520B9EF989DFF2252FF70F0D7E754551C
```

The candidate is generated at `build/distributions/guess-market-exercise-1.zip` by a clean `build.bat` run. The private `README.pdf` build input remains ignored and is intentionally not described here.

## Completed local evidence - 2026-08-18

| Gate | Evidence | Result |
| --- | --- | --- |
| README PDF | The approved English README was exported, reopened, and visually reviewed as a readable two-page PDF. The PDF includes the run instructions, package contents, main modules and classes, assumptions, source/build note, GitHub link, and save-and-restore bonus declaration. It remains ignored by Git. | Passed locally |
| Zero-based authoritative build | A fresh `build.bat` run compiled all modules and tests, then rebuilt the JARs and candidate ZIP from zero-based build output. | Passed locally |
| Mandatory JUnit proof | 11 required test classes and 145 tests executed. Failures, container failures, skips, disabled tests, and aborts were all zero. | Passed locally |
| Packaged runtime scenarios | The rebuilt ZIP was extracted under a path containing spaces. The build verified the launcher inside and outside the package directory, valid load-list-purchase-close-save behavior, restore in a new process, invalid-XML recovery without an exception type, and explicit exit. | Passed locally |
| README-led walkthrough | A second fresh extraction of the same ZIP hash was started from its parent directory using only `run.bat` and the README procedure. It loaded a valid XML file, displayed all three events, and exited normally. | Passed locally |
| End of input | The exact extracted package was started from outside its directory with zero-byte standard input. It ended with `Input closed. Exiting.` and exposed no raw exception. | Passed locally |
| Package membership | The ZIP contains exactly `README.pdf`, `guessmarket-ui.jar`, `run.bat`, and `lib/` with the two application library JARs and five approved JAXB runtime JARs. | Passed locally |
| Source and privacy review | Tracked status was clean. The private README was ignored and untracked. No tracked personal ID, local user path, `.ser` state file, or credential-pattern match was found. Gitleaks scanned 60 commits and reported no leaks. | Passed locally |

## Required manual evidence still open

| Gate | Why local evidence cannot replace it | Required record |
| --- | --- | --- |
| IntelliJ artifact cross-check | This checks the IDE artifact configuration separately from the repository-owned build. | The three artifacts, their classes/resources, manifest, and dependencies, with confirmation that no IDE output enters the submission. |
| Clean Windows 10 execution | A development machine or Windows 11 result is not the required clean Windows 10 evidence. | OS evidence, Java version, the ZIP hash above, fresh extraction location, launcher command, central transcript, and result. |
| Public-access check | The repository is intentionally private until Nitzan changes its visibility. | After Nitzan makes it public, an unauthenticated view must open the documented GitHub link and the intended `main` branch. |

## Final gate

After the three manual gates are recorded, present this same ZIP hash and all evidence to Nitzan. Only Nitzan's explicit final approval may authorize copying the ZIP to a submission location, copying it to Drive, tagging `exercise-1-submission`, changing repository visibility, or submitting it.
