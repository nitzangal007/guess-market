# Guess Market - Exercise 3

One shared in-memory Tomcat market and independent JavaFX HTTP clients. Implemented bonuses: None.

Every event has exactly two outcomes, identified as Option 1 and Option 2. The handout retains broader-outcome wording, but the supplied EX3 schema caps the option list at two and the course clarification retains binary trading. The approved scope therefore requires exactly two outcomes for both methods; one or more than two is rejected. Arbitrary-outcome LMSR is outside this scope.

## Run the submission

1. Install a Windows x64 Java 25 JDK and Apache Tomcat 10.1. The verified development versions are Oracle 25.0.4 and Tomcat 10.1.61, Jakarta Servlet 6.0.
2. Extract the complete archive into a folder. Copy `guessmarket.war` to Tomcat's `webapps` directory. Start Tomcat with Java 25 and its HTTP connector on port 8080.
3. Keep `client/run.bat`, `client/lib` and `client/javafx` together. Launch `client/run.bat`. It uses Java from `JAVA_HOME` when set, otherwise `java.exe` on PATH. JavaFX and every client library/native dependency are included; no `JAVAFX_HOME` or IDE is required.
4. Launch the batch again for another independent client. Both use `http://localhost:8080/guessmarket/api/v1/`. The WAR basename determines the `/guessmarket` context and must remain unchanged.

Set `JAVA_HOME` to the Java 25 JDK before starting Tomcat. Run Tomcat's `bin/startup.bat`; use `bin/catalina.bat run` in a console to inspect startup errors. Wait for WAR deployment before login. Tomcat's `bin/shutdown.bat` stops the server and clears this in-memory application's state. The ZIP/PDF are supplied separately to the course, not published as GitHub release assets. Exercise 4 is outside this release; bonuses are None.

This distribution includes the WAR, client launcher/libraries/JavaFX native files, licenses and a PDF user manual. The manual contains required private submitter details. Source handouts, tests and internal evidence are excluded from the submission archive.

## Use the market

Log in with an English name. Names are trimmed and compared case-insensitively; the first accepted spelling remains the display name. A connected name cannot be used by another client. A first account starts at zero. Load funds when an opening needs funding; an inactive name reconnects to its retained account during the same server run.

Upload an EX3 XML file from any legal client path, including paths with spaces. The entire batch appends atomically, assigns its owner to the logged-in account and starts each event as Not started. It changes no cash until explicit opening. XML is limited to 5 MiB and the request to 6 MiB. Duplicate normalized event names, schema/semantic errors, malformed XML and external entities are rejected without replacing existing markets.

Events shows every uploaded event with method, status and commission filters. Select an event to inspect overview, your position, public trade history and settlement. Your own balance stays visible in the header. On narrow windows, Back returns to the event list. The Account tab has Available (active events), Owned (including unopened events) and Participations (including closed history), your cash activity and read-only public users.

The owner reviews and confirms opening or closing. LMSR purchases require an option number and positive whole quantity. Order Book orders additionally require BUY/SELL and an exact decimal limit from 0 through `d - 0.01`. Prices need not be whole cents. Option numbers distinguish equal or empty labels. There is no manual Cancel or Mint action; matching, minting, reservations and blocked-order cancellation follow the accepted Exercise 2 rules.

Every financial action uses a server-calculated review followed by confirmation. If the market changed, keep the form input, review the updated effects and confirm again. Calculation cells display two decimals; exact editable limits and Exact values disclosures preserve full amounts. Account rows distinguish funding, purchase, sale, fees, payout and refund. Journal running balance explains components; Stored balance after transaction is the Engine's authoritative result. A representation adjustment only reconciles existing binary64 results and never changes trading arithmetic.

A negative-cash purchase can block opening/trading. Load funds remains available, and a blocked owner may close an active owned event. Access recovers only when a real receipt leaves cash strictly above zero.

The client polls once per second with at most one poll in flight. A disconnection keeps cached information visibly stale and disables submissions. An uncertain mutation retains its UUID and exact inputs/bytes. Use Check/retry same request to look up its receipt or explicitly retry that captured request. A local timeout/cancel does not prove server rollback. Automatic HTTP POST retries and redirects are disabled, including OkHttp's 503 Retry-After behavior.

Logout releases the connected name and clears private client state while retaining the server account. Sessions expire after 30 minutes without activity. Server restart erases accounts, events and receipts, changes the server instance ID and requires a fresh login. Name-only accounts are a coursework policy, not password-protected accounts.

## Structure and build

DTO remains JDK-only. New concrete wire records are in `guessmarket.dto.api`. Engine retains the E2 domain and adds `SharedMarketEngineImpl`, secure EX3 XML parsing, scoped snapshot mapping and an observational immutable `AccountJournal`. `MarketWorld` adds account/credit/append operations without replacing existing E2 entry points or arithmetic.

The new server module owns `MarketApplication`, session/name lifecycle, actor-scoped request registry, typed JSON validation, multipart upload and servlet routing. Its one application gate serializes market reads, commands and receipt publication. Parsing/network I/O and JSON writing occur outside the gate. The WAR contains Engine/DTO/Gson/JAXB, with Servlet API supplied by Tomcat and no JavaFX/OkHttp.

The new client module contains `MarketHttpClient`, isolated `SessionCookieJar`, retained `UploadAttempt`, separate `OperationGate`/`DashboardGate`, `PollCoordinator`, `RemoteMarketSession` and focused Events/Account/detail/dialog views. It depends on DTO/Gson/OkHttp/JavaFX and contains no Engine/JAXB. Network and file work runs on workers; only the FX thread changes controls/properties.

Pinned versions, dependency closure, checksums, source URLs and licenses are in `dependencies/exercise3-dependencies.json` in the source checkout. In the extracted submission, that same manifest is `licenses/dependency-manifest.json`. `build-exercise3.bat client-core` compiles/tests E3; `all` additionally invokes the complete E2 regression in its own JVM. A separate `client-native` target selects live-server native integration, with an optional process-local `GUESSMARKET_TEST_URL`. New shared-module E3 tests use `src/test-exercise3/java` so original E1/E2 build boundaries remain intact. Existing E1/E2 launchers remain independent.

## Source and verification state

Source compilation uses the included dependencies and embedded schemas. The current `build-exercise3.bat` entry point also runs tests. Two supplied-input compatibility tests require the canonical local `small.xml` and `multiple.xml` under `provided/assignment-3/test files ex3/`. Those course inputs are excluded from public source and the submission archive. Obtain authorized course copies before running the full core suite; a public clone alone cannot run those two tests. They are preserved and are not silently skipped. The packaged app does not need those files to launch, and users can select their own valid EX3 XML to upload.

Current Exercise 3 source: https://github.com/nitzangal007/guess-market/tree/main. The root README describes Exercise 3; README-EXERCISE2.md preserves the earlier workflow. Source publication is separate from course submission.

Local server tests and native client checks run on Windows 11. Non-visible runtime review passed 247 core tests and 32 independent reviewer methods. Human reports cover two-account funding, shared upload visibility, LMSR open/buy/close and a custom-label Order Book opening. A full visible Order Book workflow, all controls/keyboard/DPI, clean Windows 10 and exact client/server restart remain unverified. Source publication does not establish those gates or course submission.

Known deferred UI issue: empty/invalid funds submission can clip dialog buttons when its validation banner expands. Close that dialog with its X and reopen with a positive amount. The unverified layout patch is excluded. Prefer in-app Logout and allow the request to finish before closing the CMD host; abrupt termination can retain the connected name until the original session expires.
