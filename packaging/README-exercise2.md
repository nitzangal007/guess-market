# Guess Market - Exercise 2

Implemented bonuses: **None**.

A Java 25 prediction-market application with separate user accounts, LMSR events and Order Book events. The Windows x64 submission includes its Java 25.0.4 and JavaFX 25.0.4 runtime plus JAXB 4.0.5 dependencies. No Java installation, JAVA_HOME or JAVAFX_HOME setting is required to run the extracted submission.

## Run and use

1. Extract the complete ZIP into a writable folder. Keep `run.bat`, `lib` and `runtime` together, then double-click `run.bat`.
2. Use Load File to choose an Exercise 2 XML file. Original assignment XML is supplied separately and is not included in this archive. A failed load retains the previous world.
3. Inspect Events and its filters or open Users and select the acting user. Available, Owned and Participations show the appropriate events and retained closed history.
4. The event owner opens an event and transfers its initial funding. For LMSR, choose an outcome and quantity and review the purchase. For Order Book, choose BUY or SELL, outcome, whole quantity and unit limit, then review executions before confirming. Back preserves the entered limit; Cancel leaves the world unchanged.
5. The owner resolves an active event by selecting its winning outcome and reviewing payouts. Inspect trade history, holdings, fees and settlement afterward.

Each loaded world replaces the previous world after successful validation. Reloading creates a fresh simulation. The Exercise 2 interface does not save manual trades between application launches. Closing the main window exits the app.

## Modules and main classes

- `guessmarket-dto.jar`: immutable requests, snapshots, previews and receipts shared between UI and Engine.
- `guessmarket-engine.jar`: XML/schema and domain validation, accounts, LMSR calculations, Order Book matching, ledger and settlement. It does not depend on JavaFX.
- `guessmarket-javafx-ui.jar`: JavaFX controls, background operations and presentation. Main class: `guessmarket.ui.javafx.GuessMarketApplication`.

Key classes: `GuessMarketWorldEngineImpl` implements the Exercise 2 command boundary and revision checks; `MarketWorld` holds accounts and lifecycle; `OrderBookMatcher` plans compatible executions; `OrderBookOperations` validates and publishes orders and settlement; `OrderBookLedger` creates holdings/accounting/valuation snapshots; `WorldSession` serializes background commands and publishes observable UI state; `MainController` and `UsersController` drive the two views; `EventDetailsView` and `OrderBookView` present method-specific details; `OrderDialog` preserves exact input through review. FXML and CSS are included in the UI JAR, and the trusted schema is included in the Engine JAR.

## Meaningful implementation choices

- Match ordinary compatible orders first, at the resting order price. Best price wins; equal prices use FIFO. A partial remainder keeps its original priority.
- If mint is enabled, compatible opposite BUY orders may create pairs when their limits sum to at least `d`, including equality. The best opposite BUY price and then FIFO determine priority. There is no manual unilateral mint action.
- SELL orders reserve owned shares. BUY orders do not reserve cash. Self-matching and self-minting reject the entire command. Users cannot manually cancel orders in the selected assignment flow.
- Complete all accepted fills before applying final blocking and cancellation across events. Cash falling below zero after the gross purchase debit blocks buying/opening; a positive receipt restores eligibility only if final cash is strictly positive. A blocked owner may still settle owned events. Zero receipts do not recover access.
- Opening requires sufficient cash and a whole initial pair count. Otherwise-valid nondivisible Order Book funding loads but rejects at opening. Zero initial funding is valid.
- Holdings estimates use MID when both sides have quotes, otherwise LAST, otherwise Unavailable. Closed events use winner `d` and loser zero. Cumulative purchases, sale receipts, fees and owner funding are reported separately; settled holdings are not counted twice in profit/loss.
- Order limits and Order Book financial calculations use exact BigDecimal values. Sub-cent limits are allowed within the specified range. Entered limits remain exact in the book, editor and review. Calculated decimal outputs round half up to at most two fractional digits; display values are never sent back as calculation inputs.
- LMSR accounts use double arithmetic with finite/overflow checks. Positive debits that disappear against the current cash balance reject before publication. Legitimate settlement rounding is bounded and verified from purchase history; malformed or unjustifiable states reject. Preview revisions protect against confirming a stale world.

These choices follow the implemented assignment interpretation, including the detailed appendix mint equality rule and the locally selected cash-recovery policy.

## Source and dependencies

Current Exercise 2 source: https://github.com/nitzangal007/guess-market/tree/codex/e2-order-book

The repository also preserves the Exercise 1 console application and its separate build/documentation. This submission launches the Exercise 2 JavaFX application.

Bundled third-party notices are retained in `runtime/legal`, in dependency JARs and in `THIRD-PARTY-NOTICES.txt`. No open-source license is granted for application code; third-party components retain their own licenses. This runtime is for Windows x64. Local extracted-package verification does not certify other operating systems or every display scale.
