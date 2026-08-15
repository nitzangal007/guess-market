# Guess Market GitHub Repository Design

Status: approved written specification on 2026-08-15. No repository migration, remote connection, push, or implementation is authorized by this document alone.

## 1. Purpose

The GitHub repository will serve three roles:

1. Preserve a reviewable development history for the evolving Guess Market codebase across Exercises 1 through 3.
2. Give the course grader access to the project code through the link required in the submission README.
3. Become a polished portfolio repository after the course, a publication-safety review, and Nitzan's explicit approval.

The repository is not a mirror of the complete Java Course Workspace. It contains the Guess Market project and only the supporting material that an external developer, grader, or recruiter can use productively.

## 2. Repository identity

- Owner: `nitzangal007`
- Repository name: `guess-market`
- Repository URL after creation: `https://github.com/nitzangal007/guess-market`
- Initial visibility: private
- Default branch: `main`
- Initial description: `A Java 25 prediction-market simulator built around LMSR pricing, modular architecture, XML/JAXB loading, automated testing, persistence, and reproducible Windows packaging.`

The name is intentionally product-focused and remains valid as the codebase evolves through later exercises.

## 3. Local repository architecture

The single authoritative project location remains:

`assignments/guess-market`

After the approved migration, that folder will contain its own `.git` directory and will be the local clone connected to the GitHub repository.

The surrounding Java Course Workspace repository will continue to exist locally, but it will stop tracking `assignments/guess-market`. Its earlier commits will continue to preserve the historical project snapshots that already exist there. The migration will remove the project folder only from the outer repository index. It will not delete, move, or duplicate the physical project files.

This creates two independent histories with different purposes:

- The outer repository preserves private workspace history and other course-workspace files.
- The dedicated repository owns every future Guess Market source, test, build, and public-safe documentation commit.

Git commands for Guess Market implementation must run with `assignments/guess-market` as their repository root. The repository-local handoff must state this explicitly so that an agent does not accidentally operate on the outer repository.

## 4. Migration safety gates

The dedicated repository migration may begin only after all of these conditions hold:

1. The corrected Exercise 1 design is complete and synchronized.
2. Nitzan approves this written GitHub design.
3. A detailed repository-migration plan is written and approved.
4. The current outer worktree is inventoried and its intended documentation and supplied-file changes receive a deliberate checkpoint.
5. The exact project path is verified before any outer-index change.
6. The future tracked and ignored file sets pass privacy, credential, course-material, and license reviews.

The migration must be recoverable. It must preserve the outer history, keep every physical project file in place, and verify both repositories after the split.

An empty private GitHub repository may be reserved before the migration. It must be created without a generated README, `.gitignore`, or license commit. It must not receive the project history until the migration plan and the clean initial snapshot are approved.

## 5. Publication boundary

### 5.1 Tracked project content

The dedicated repository will track:

- Root `README.md` with an external-reader entry point and honest project status.
- A public-safe repository-local `PROJECT_HANDOFF.md`.
- The approved Java module source and resource trees.
- All handwritten production Java source.
- All retained XJC-generated Java source required by the build.
- The Engine-owned runtime XSD resource.
- Required supplied and custom XML test-fixture copies used by automated tests.
- JUnit test source and project-owned test helpers.
- `build.bat` and its repository-owned verification logic.
- Repository-owned manifest, launcher, and other packaging inputs.
- The selected minimal third-party dependency set and its required licenses and notices.
- Curated project documentation defined in Section 6.
- `submissions/README.md`, which explains that actual submission snapshots remain local-only.
- The repository ignore rules and other small source-control configuration files approved by the migration plan.

### 5.2 Always excluded

The dedicated repository will never track:

- `private/`, including every identity-bearing submission README.
- Actual files beneath `submissions/`, other than its explanatory `README.md`.
- The `provided/` course-source tree, including handouts, course guides, learning files, and archives.
- Generated `build/` output, IntelliJ output, IDE metadata, compiled loose classes, logs, temporary files, and local verification extractions.
- Submission ZIP files or other identity-bearing distribution artifacts.
- Runtime `.ser` files.
- Credentials, access tokens, authenticated configuration, or private account data.
- Outer-workspace instructions, private memory files, or machine-specific paths.
- Unused JAXB executables, documentation, examples, sample schemas, and unrelated distribution files.

The physical local versions of ignored course sources and tools may remain available in the project folder when required for learning or provenance. Their presence on disk does not authorize Git tracking or publication.

## 6. Public documentation set

The repository will not publish every local document automatically. It will publish the documents that help another person understand, build, test, review, or evaluate the project.

The intended public documentation set is:

- `README.md`: concise project overview, current status, capabilities, screenshots or examples when available, requirements, quick start, and documentation links.
- `PROJECT_HANDOFF.md`: public-safe current state, authoritative paths inside the repository, latest verified checkpoint, and exact next task.
- `docs/README.md`: the documentation index and category map.
- `docs/design/ARCHITECTURE.md`: modules, dependency direction, package boundaries, major components, and important runtime flows.
- `docs/design/EXERCISE-1-DESIGN.md`: the complete approved Exercise 1 design and decision history.
- `docs/design/EXERCISE-1-DESIGN-BRIEF.md`: a concise entry point to the complete design.
- `docs/planning/IMPLEMENTATION-PLAN.md`: the approved task-by-task implementation plan.
- `docs/guides/BUILD-AND-RUN.md`: Java version, dependencies, authoritative build, launcher, artifact layout, and clean-machine instructions.
- `docs/guides/TESTING.md`: test topology, requirement traceability, packaged-system verification, and known evidence boundaries.
- `docs/guides/JAXB-AND-XML.md`: the externally useful parts of the JAXB generation, mapping, validation, and resource workflow.
- `docs/guides/IMPLEMENTATION-WALKTHROUGH.md`: the living method inventory, learning explanations, and verified stage history maintained alongside implementation.
- `docs/reviews/DESIGN-REVIEW-RESOLUTIONS.md`: a concise record of the adversarial review's material findings and their approved resolutions.
- `docs/repository/GITHUB-REPOSITORY-DESIGN.md`: this repository and publication policy.

The following current working records remain local-only after their useful conclusions are distilled into the public documents above:

- The pre-implementation review prompt and raw review report.
- The internal master roadmap.
- The raw JAR-recording evidence report.
- The current local workspace-layout guide.
- Course learning guides, lecturer checklists, supplied source files, screenshots, and transcripts.

Before the clean initial commit, every public document must be checked for stale claims, machine-specific paths, personal identifiers, broken references to excluded files, and unnecessary reproduction of course-owned text.

The repository may be established before `docs/planning/IMPLEMENTATION-PLAN.md` exists. In that sequence, the implementation plan is the first substantive planning document added after the standalone repository is verified and before any Java implementation branch is created.

## 7. Third-party dependency policy

The repository will remain reproducible without downloading dependencies during `build.bat`.

It will track only the minimal approved dependency set:

- The seven JARs under `tools/jaxb-ri-4.0.5/mod`.
- `tools/jaxb-ri-4.0.5/xjc-run.bat`.
- The JAXB distribution license and any required notices.
- The approved JUnit Platform Console Standalone JAR under `tools/testing`.
- The JUnit license and any required notices.

The five JAXB runtime JARs remain the only JAXB JARs placed on the running application's classpath. The XJC and JXC JARs remain development tools and never enter application JARs or the runtime ZIP.

The local complete JAXB distribution may remain on disk for course provenance and documentation, but its `bin`, `docs`, `samples`, unrelated schema, and other unused files will not be tracked by the dedicated repository.

The repository must identify all vendored third-party binaries and their roles in its documentation. A fresh-clone verification must prove that the tracked dependency set is sufficient for the approved build and test flow.

## 8. Visibility, grader access, and publication

The repository remains private throughout the active Java course, including the later Guess Market exercises.

A private repository URL does not grant access by itself. For a personal-account repository, the grader must be invited by GitHub username or email and must accept the invitation. That collaborator receives write access because personal repositories do not provide a separate read-only collaborator role.

Before Exercise 1 submission, Nitzan must obtain course guidance on one of these access models:

1. Invite a named grader account to the private repository.
2. Make the repository public because the course explicitly expects public access.
3. Use another access method explicitly accepted by the course.

This access question is a required submission-readiness item, not an implementation blocker. Repository visibility must not change without Nitzan's explicit approval.

Public portfolio release requires all of the following:

- Completion of the active course work.
- Confirmation that publication does not conflict with current course rules or other students' active work.
- A final privacy and credential scan of the complete reachable history.
- Verification of every included dependency license and notice.
- A public-documentation and broken-link review.
- Removal of any identity-bearing or course-owned material that was never approved for publication.
- Nitzan's explicit publication approval.

## 9. Authentication and commit identity

- GitHub owner: `nitzangal007`.
- Local GitHub authentication will use GitHub's browser-based sign-in flow.
- No personal access token or credential may be stored in a project file.
- The dedicated repository will use Nitzan's GitHub-provided no-reply commit email, verified during setup.
- The outer repository's previous commits and direct email metadata will not be copied into the dedicated public-facing history.
- Every push must follow a staged-path review and a privacy and credential scan.

## 10. GitHub feature configuration

Initial configuration:

- `main` is the default branch.
- Issues are enabled for discovered defects and bounded follow-up work.
- Pull requests are enabled.
- Wiki, Discussions, GitHub Projects, and GitHub Pages are disabled.
- GitHub Releases remain unused until a sanitized portfolio artifact is approved.
- GitHub Actions is added only after the authoritative local `build.bat` exists and passes locally.
- Repository topics are added only after the implemented code provides evidence for them.
- No open-source license is added to Nitzan's own code during the active course.

Before public publication, Nitzan will separately choose whether the project receives an MIT license or remains under default copyright. Third-party license files remain present regardless of that choice.

## 11. Branch, commit, and pull-request workflow

The repository uses a lightweight milestone-based GitHub flow.

### 11.1 Branches

- `main` contains only approved, working checkpoints.
- Each implementation-plan milestone uses one short-lived branch.
- Agent-created branch names use the `codex/` prefix.
- Example branches include `codex/e1-project-foundation`, `codex/e1-dto-contracts`, `codex/e1-lmsr-domain`, `codex/e1-engine-xml`, `codex/e1-console-ui`, `codex/e1-persistence`, and `codex/e1-packaging`.
- There is no long-lived `develop` branch and no GitFlow release-branch hierarchy.

### 11.2 Commits

- Commits are small, coherent, and reviewable.
- Tests normally travel with the behavior they verify.
- Commit subjects use clear prefixes such as `feat:`, `test:`, `fix:`, `docs:`, and `build:`.
- No implementation change is committed directly to `main`.

### 11.3 Pull requests

- One pull request closes one implementation-plan milestone.
- The pull request records scope, material implementation choices, verification evidence, and remaining limitations.
- Required tests and milestone checks must pass before merge.
- The complete diff, staged paths, private-file exclusions, and credential scan must be reviewed before merge.
- Pull requests are squash-merged into `main` and their branches are deleted.

GitHub Issues do not replace the approved implementation plan. They are used for discovered defects or bounded follow-up work that needs an independently visible lifecycle.

## 12. Continuous integration

GitHub Actions is deferred until the repository-owned `build.bat` exists and succeeds locally.

The first CI workflow will:

- Run on a GitHub-hosted Windows runner.
- Invoke the authoritative `build.bat` rather than reproduce its build logic in YAML.
- Run for pull requests and pushes to `main`.
- Fail when the authoritative build or mandatory test verifier fails.

CI is supplemental evidence. It does not replace the required clean Windows 10 or demonstrably compatible clean-Windows run of the exact final ZIP, the IntelliJ cross-check, or the final manual grader walkthrough.

## 13. Assignment checkpoints and releases

The source commit corresponding to each exact approved submission receives an immutable source tag:

- `exercise-1-submission`
- `exercise-2-submission`
- `exercise-3-submission`

The tags identify source checkpoints only. Identity-bearing submission ZIPs are not uploaded as GitHub releases.

After the course and the public-release review gates pass, the portfolio project may receive a sanitized `v1.0.0` release. That release must use public-safe documentation and artifacts rather than reusing an identity-bearing course submission package.

## 14. Approval record

Nitzan approved the following decisions on 2026-08-15:

- Portfolio-ready purpose after the course.
- A dedicated Guess Market repository rather than the complete Java Course Workspace.
- The existing `assignments/guess-market` folder as the future standalone repository root.
- A clean publication-safe history that begins before implementation.
- Layered, curated public documentation with the complete approved design and implementation plan.
- Minimal vendored build dependencies with required licenses, while course originals remain excluded.
- Repository identity `nitzangal007/guess-market`.
- Private visibility throughout the active course and an explicit later publication gate.
- Explicit grader-access clarification before submission.
- Browser-based authentication and no-reply commit identity.
- Lightweight milestone branches, pull requests, squash merges, and no long-lived `develop` branch.
- CI that later invokes `build.bat` and never replaces exact-artifact Windows verification.
- Source tags per assignment and no publication of identity-bearing submission ZIPs.

## 15. Exact next step

The written specification is approved. Finish and verify the standalone repository migration first. After the dedicated repository is established, create and approve the detailed Java implementation plan as the next separate phase. Do not begin Java implementation before that later plan is approved.
