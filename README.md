# Guess Market - Exercise 3

A Java 25 prediction market with one shared in-memory Tomcat server and independent JavaFX HTTP clients. Each client logs in as one account, uploads events, loads funds, and trades through the server. LMSR and Order Book rules from Exercise 2 are retained. **Implemented bonuses: None.** Exercise 4 is outside this release.

Current source: [GitHub main](https://github.com/nitzangal007/guess-market/tree/main). [Detailed Exercise 3 instructions](README-EXERCISE3.md) explain accounts, trading, recovery and accounting. This repository contains source and pinned dependencies; the private submission ZIP and identity-bearing PDF are distributed separately.

## Run the Exercise 3 distribution

1. Install Windows x64 Java 25 and Apache Tomcat 10.1. Verified development versions: Oracle JDK 25.0.4, Tomcat 10.1.61, Jakarta Servlet 6.0.
2. Extract the complete submission ZIP. Copy its single `guessmarket.war` to Tomcat's `webapps` folder. Start Tomcat with Java 25 on port **8080**. Keep the WAR basename unchanged.
3. Keep `client/run.bat`, `client/lib` and `client/javafx` together. Run `client/run.bat` with Java 25 on `JAVA_HOME` or PATH. JavaFX module JARs and Windows native files are included; no IDE or `JAVAFX_HOME` is needed to launch.
4. Run the batch again for another client with a different English login name. Both connect to `http://localhost:8080/guessmarket/api/v1/`.
5. Load funds, upload valid EX3 XML, then review and confirm financial actions. Upload assigns ownership to the logged-in account, appends events as Not started, and does not itself debit cash.

For Tomcat, extract its Windows distribution, set `JAVA_HOME` to the Java 25 JDK, and run `bin/startup.bat` from the Tomcat installation. Its default HTTP connector must use port 8080. On setup errors, run `bin/catalina.bat run` in a console to see the server log. Wait for the `guessmarket.war` deployment to finish before logging in. Stop the server deliberately with `bin/shutdown.bat` after use; stopping resets this in-memory application. The submitted ZIP/PDF are supplied separately for the course and are not GitHub release assets.

Every event has exactly two ordered outcomes. The supplied EX3 schema caps the list at two and course clarification retains binary trading despite broader handout wording. One or more than two is rejected; arbitrary-outcome LMSR is outside scope. Empty/repeated labels still have distinct option numbers.

Use **Logout** before closing the app or its CMD host, and allow the request to finish. Abrupt process termination can leave the name reserved until the original session expires after 30 minutes of inactivity. Reconnecting afterward retains the account in the same server run. Server restart intentionally clears all accounts, events and receipts.

## Build and test from source

Use Windows x64 and Oracle JDK 25.0.4, preferably in a short checkout path. JavaFX, JAXB, Gson, OkHttp/Okio/Kotlin and JUnit dependencies are vendored; the build downloads nothing.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
Remove-Item Env:JAVAFX_HOME -ErrorAction SilentlyContinue
.\build-exercise3.bat client-core
```

**Course-input prerequisite:** two preserved EX3 compatibility tests require authorized canonical `small.xml` and `multiple.xml` in `provided/assignment-3/test files ex3/`. That directory is ignored and unpublished. Obtain these course inputs before running the complete core suite. A public clone alone cannot run those two tests. They are not skipped or replaced. Product compilation uses included dependencies and embedded schemas; the packaged app does not require the samples to launch or upload another valid file.

Compilation uses UTF-8, `--release 25`, `-Xlint:all` and `-Werror`. Output is under `build/exercise3`: `dev/guessmarket.war`, `dev/client`, timestamped JUnit reports and a build-input hash manifest. Deploy that WAR and use the development client's `run.bat` as above.

| Target | Scope |
| --- | --- |
| `client-core` | DTO/Engine/server/client compilation and non-visible tests. `native-ui` and `live-server` tagged tests are explicitly outside this target. |
| `client-ui` | Adds shown JavaFX UI tests, excluding live-server integration. Requires permitted native windows. |
| `client-native` | Selects real-server native integration. Deploy an isolated server first; optional process-local `GUESSMARKET_TEST_URL` sets its API URL. |
| `all` | Core build plus the preserved full Exercise 2 regression in a separate JVM, including native windows. |

[Dependency metadata](dependencies/exercise3-dependencies.json) records pinned versions, hashes, source URLs, transitive closure and licenses. The distribution includes the same metadata as `licenses/dependency-manifest.json`. `packaging/package-exercise3.ps1` accepts `OutputDirectory`, verified private `ManualPdf`, and plain `ArchiveName` to package a verified build. No private PDF is needed for compilation.

## Architecture

```text
JavaFX client + DTO -- HTTP/JSON, isolated cookies --> Tomcat server + Engine + DTO
```

| Module | Responsibility |
| --- | --- |
| `guessmarket-client` | Events/Account views, dialogs, HTTP, polling, retained uncertain requests and private client state. Entry: `guessmarket.client.ClientMain`. No Engine/JAXB. |
| `guessmarket-server` | Session-bound actors, validation, multipart XML, revision/UUID receipts and one synchronization gate. Tomcat supplies Servlet API. |
| `guessmarket-engine` | Existing market rules plus account creation/credit/event append, secure stream parsing, scoped snapshots and observational journal. No JavaFX. |
| `guessmarket-dto` | Concrete immutable requests, reviews, snapshots and receipts. JDK-only. |

The server serializes transitions and snapshot capture under one gate; network reads and JSON writing occur outside it. Clients request server-calculated reviews and confirm their revision. They retain UUIDs for uncertain outcomes, with receipt lookup and explicit retry; automatic mutation retries and redirects are disabled. Polling uses a separate worker, at most one in flight. Only the FX thread changes controls.

## Validation and known limits

The accepted non-visible runtime passed **247 core tests** and **32 independent reviewer methods**, with all four client findings closed. Independent HTTP/recovery/poll checks and an exact-package two-transport soak supplement those tests. Earlier Exercise 2 regression evidence is 236/236, separate from current native acceptance. A passing `client-core` does not imply tagged native or platform checks ran.

Human reports cover two-account funding, shared XML visibility, LMSR opening/buy/close and a custom-label Order Book opening. A complete visible Order Book workflow, all dialogs/keyboard/DPI states, clean Windows 10 and exact client/server restart remain unverified. Development/native evidence uses Windows 11. Source publication does not imply course submission.

Known deferred UI issue: empty/invalid funds submission can grow the validation banner and clip dialog buttons. Close that dialog with its X and reopen with a positive amount. The unverified layout patch is excluded. Closing the CMD host is different and can terminate the client abruptly.

## Earlier exercises and notices

The [Exercise 2 guide](README-EXERCISE2.md) and [class/accounting manual](packaging/README-exercise2.md) retain the prior JavaFX workflow. `build-javafx.bat`, `guessmarket-javafx-ui` and `packaging/package-exercise2.ps1` remain Exercise 2 tools. Exercise 1's console UI remains in `guessmarket-ui`, with `build.bat` and `packaging/run.bat`; its private submission manual is excluded. Use the Exercise 3 commands above for the current application.

Third-party components retain original licenses/notices in `tools`, embedded JAR metadata and the distribution's license folders. Application code is published for coursework inspection; no application open-source license is granted.
