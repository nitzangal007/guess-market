# Exercise 1 Submission Verification

## Scope and decision boundary

This record tracks Stage 7 verification for the exact Exercise 1 candidate artifact. It separates local evidence from evidence that must be collected manually. It does not authorize publication, tagging, copying to a submission location, or submission.

Candidate ZIP SHA-256:

```text
078F0DFCA6EAA58E3A057026675301AEF8762E41E94444FEDA543CA83A2068BA
```

The candidate is generated at `build/distributions/211722525-submission.zip` by a clean `build.bat` run. The private `README.pdf` build input remains ignored and is intentionally not described here. A second clean build produced a different archive byte hash because the newly created JAR archives carry build metadata, while extraction proved the contents of the three application JARs identical. The hash above identifies the exact candidate retained after the most recent successful build.

## Completed local evidence - 2026-08-18

| Gate | Evidence | Result |
| --- | --- | --- |
| README PDF | The approved English README was reopened and visually reviewed as a readable two-page PDF. It includes run instructions, package contents, main modules and classes, assumptions, source/build note, GitHub link, both current contact emails, student ID, and the save-and-restore bonus declaration. It remains ignored by Git. | Passed locally |
| Zero-based authoritative build | A fresh `build.bat` run compiled all modules and tests, then rebuilt the JARs and candidate ZIP from zero-based build output. | Passed locally |
| Mandatory JUnit proof | 11 required test classes and 145 tests executed. Failures, container failures, skips, disabled tests, and aborts were all zero. | Passed locally |
| Packaged runtime scenarios | The rebuilt ZIP was extracted under a path containing spaces. The build verified the launcher inside and outside the package directory, valid load-list-purchase-close-save behavior, restore in a new process, invalid-XML recovery without an exception type, and explicit exit. | Passed locally |
| README-led walkthrough | A second fresh extraction of the same ZIP hash was started from its parent directory using only `run.bat` and the README procedure. It loaded a valid XML file, displayed all three events, and exited normally. | Passed locally |
| End of input | The exact extracted package was started from outside its directory with zero-byte standard input. It ended with `Input closed. Exiting.` and exposed no raw exception. | Passed locally |
| Package membership | The ZIP contains exactly `README.pdf`, `guessmarket-ui.jar`, `run.bat`, and `lib/` with the two application library JARs and five approved JAXB runtime JARs. | Passed locally |
| IntelliJ artifact ownership cross-check | Nitzan configured the three separate artifact layouts: DTO from `main3`, Engine from `main2`, and UI from `main`. Each layout contained only its module compile output. No test output, unpacked dependency, or IDE output was added. IntelliJ did not accept the repository's nonstandard UI manifest filename, so the repository-owned `build.bat` remains authoritative for the actual manifest and dependency validation. | Passed as a supplementary ownership check |
| Submission-notice package audit | The exact ZIP name is `211722525-submission.zip`. It contains only the PDF README, `run.bat`, the UI JAR, the DTO and Engine JARs, and the five required JAXB runtime JARs. The extracted README contains the ID, both current contact emails, and the GitHub link. | Passed locally |
| Source and privacy review | Tracked status was clean before this release-record update. The private README was ignored and untracked. No tracked local user path, `.ser` state file, or credential-pattern match was found. A full reachable-history scan reported no leaks. The ID is intentionally present in the generated final archive name because the submission notice requires it. | Passed locally |

## Required manual evidence still open

| Gate | Why local evidence cannot replace it | Required record |
| --- | --- | --- |
| Clean Windows 10 execution | Nitzan reported a successful run on a separate computer, but the formal record has not established whether it was clean Windows 10, its Java version, or the exact candidate hash. | OS evidence, Java version, the ZIP hash above, fresh extraction location, launcher command, central transcript, and result. |
| Public-access check | The repository is intentionally private until Nitzan changes its visibility. | After Nitzan makes it public, an unauthenticated view must open the documented GitHub link and the intended `main` branch. |

## Final gate

After the two remaining manual gates are recorded, present this same ZIP hash and all evidence to Nitzan. Only Nitzan's explicit final approval may authorize copying the ZIP to a submission location, copying it to Drive, tagging `exercise-1-submission`, changing repository visibility, or submitting it.
