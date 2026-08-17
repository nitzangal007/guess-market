package guessmarket.ui.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.MarketOptionSnapshot;
import guessmarket.dto.PurchaseReceipt;
import guessmarket.dto.TradeHistoryEntry;
import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class GuessMarketConsoleAppTest {
    private static final String MENU = """
            ============================================================
                                   GUESS MARKET
            ============================================================

            DATA
              1. Load events from XML
              2. Display all events
              3. View an event's trading status

            TRADING
              4. Purchase shares
              5. Close an event

            STATE AND SESSION
              6. Save current state
              7. Restore saved state
              8. Exit

            Commands 2 through 6 require a loaded system.

            Choose a command [1-8]:
            """;

    @Test
    void completeHappySessionRunsAllEightCommandsWithOneBlankLineBeforeEachFreshMenu() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        engine.loadedEventCount = 2;
        engine.restoredEventCount = 2;
        engine.eventSummaries = List.of(
                summary(-7, "First open event", EventStatus.OPEN),
                summary(42, "Second open event", EventStatus.OPEN));
        MarketEventDetails firstBefore = details(-7, "First open event", EventStatus.OPEN,
                CommissionMode.ON_PURCHASE, OptionalInt.empty(), List.of());
        MarketEventDetails secondBefore = details(42, "Second open event", EventStatus.OPEN,
                CommissionMode.ON_CLOSE, OptionalInt.empty(), List.of());
        MarketEventDetails firstAfter = details(-7, "First open event", EventStatus.OPEN,
                CommissionMode.ON_PURCHASE, OptionalInt.empty(),
                List.of(new TradeHistoryEntry(2, "Away", 5, 2.0, 0.2, 2.2)));
        MarketEventDetails secondAfter = details(42, "Second open event", EventStatus.CLOSED,
                CommissionMode.ON_CLOSE, OptionalInt.of(1), List.of());
        engine.detailsById.put(-7, firstBefore);
        engine.detailsById.put(42, secondBefore);
        engine.purchaseReceipt = new PurchaseReceipt(2, 5, 2.0, 0.2, 2.2, firstAfter);
        engine.closeResult = secondAfter;
        String script = """
                1
                C:\\data files\\multiple.xml
                2
                3
                2
                4
                1
                2
                5
                5
                2
                1
                6
                C:\\state files\\market
                7
                C:\\state files\\market
                8
                """;

        String output = run(engine, script);

        assertEquals(List.of(
                "loadEventsFromXml(C:\\data files\\multiple.xml)",
                "listEvents()",
                "listEvents()",
                "getEventDetails(42)",
                "listEvents()",
                "getEventDetails(-7)",
                "purchaseShares(-7,2,5)",
                "listEvents()",
                "getEventDetails(42)",
                "closeEvent(42,1)",
                "saveState(C:\\state files\\market)",
                "restoreState(C:\\state files\\market)"), engine.calls);
        assertEquals(8, occurrences(output, MENU));
        assertExactlyOneBlankLineBeforeEachRepeatedMenu(output);
        assertTrue(output.contains("System loaded successfully.\nLoaded events: 2"));
        assertTrue(output.contains("Purchase completed successfully."));
        assertTrue(output.contains("UPDATED EVENT STATUS"));
        assertTrue(output.contains("Event closed successfully."));
        assertTrue(output.contains("FINAL EVENT STATUS"));
        assertTrue(output.contains("System state saved successfully."));
        assertTrue(output.contains("System state restored successfully.\nRestored events: 2"));
        assertTrue(output.endsWith("Goodbye.\n"));
    }

    @Test
    void invalidMainMenuTextReturnsToFreshMenusWithoutEnteringACommand() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();

        String output = run(engine, "\nword\n0\n9\n8\n");

        assertEquals(List.of(), engine.calls);
        assertEquals(4, occurrences(output, "Invalid menu choice. Enter a number from 1 to 8."));
        assertEquals(5, occurrences(output, MENU));
        assertExactlyOneBlankLineBeforeEachRepeatedMenu(output);
    }

    @Test
    void commandsTwoThroughSixBeforeLoadingRenderRecoverableErrorsAndContinue() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        EngineOperationException noSystem = failure(EngineErrorCode.NO_SYSTEM_LOADED);
        engine.failWith(FakeGuessMarketEngine.LIST, noSystem);
        engine.failWith(FakeGuessMarketEngine.SAVE, noSystem);

        String output = run(engine, "2\n3\n4\n5\n6\nC:\\state\\market\n8\n");

        assertEquals(List.of(
                "listEvents()", "listEvents()", "listEvents()", "listEvents()",
                "saveState(C:\\state\\market)"), engine.calls);
        assertEquals(5, occurrences(output, "Error: No system is loaded."));
        assertEquals(5, occurrences(output, "Recovery: Load XML or restore saved state first."));
        assertEquals(6, occurrences(output, MENU));
        assertExactlyOneBlankLineBeforeEachRepeatedMenu(output);
    }

    @Test
    void invalidSecondaryInputRepeatsOnlyItsPromptAndEndOfInputExitsNormally() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        engine.eventSummaries = List.of(summary(17, "Only event", EventStatus.OPEN));

        String output = run(engine, "3\n0\nword\n");

        assertEquals(List.of("listEvents()"), engine.calls);
        assertEquals(3, occurrences(output, "Choose an event [1-1]:"));
        assertEquals(2, occurrences(output, "Invalid selection. Enter a number from 1 to 1."));
        assertEquals(1, occurrences(output, MENU));
        assertTrue(output.endsWith("Input closed. Exiting.\n"));
    }

    @Test
    void detailsUseFullListWhilePurchaseAndCloseFilterOpenEventsAndTranslateFreshPositions() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        engine.eventSummaries = List.of(
                summary(-9, "First", EventStatus.OPEN),
                summary(80, "Closed", EventStatus.CLOSED),
                summary(0, "Last", EventStatus.OPEN));
        engine.detailsById.put(-9, details(-9, "First", EventStatus.OPEN,
                CommissionMode.ON_CLOSE, OptionalInt.empty(), List.of()));
        engine.detailsById.put(80, details(80, "Closed", EventStatus.CLOSED,
                CommissionMode.ON_CLOSE, OptionalInt.of(2), List.of()));
        engine.detailsById.put(0, details(0, "Last", EventStatus.OPEN,
                CommissionMode.ON_PURCHASE, OptionalInt.empty(), List.of()));
        MarketEventDetails purchased = details(0, "Last", EventStatus.OPEN,
                CommissionMode.ON_PURCHASE, OptionalInt.empty(),
                List.of(new TradeHistoryEntry(1, "Home", 3, 1.5, 0.15, 1.65)));
        engine.purchaseReceipt = new PurchaseReceipt(1, 3, 1.5, 0.15, 1.65, purchased);
        engine.closeResult = details(-9, "First", EventStatus.CLOSED,
                CommissionMode.ON_CLOSE, OptionalInt.of(2), List.of());

        String output = run(engine, "3\n2\n4\n2\n1\n3\n5\n1\n2\n8\n");

        assertEquals(List.of(
                "listEvents()", "getEventDetails(80)",
                "listEvents()", "getEventDetails(0)", "purchaseShares(0,1,3)",
                "listEvents()", "getEventDetails(-9)", "closeEvent(-9,2)"), engine.calls);
        assertEquals(1, occurrences(output, "2. Event ID: 80"));
        assertEquals(2, occurrences(output, "2. Event ID: 0"));
        assertEquals(3, occurrences(output, "1. Event ID: -9"));
        assertEquals(2, occurrences(output, "OPEN EVENTS"));
        assertEquals(1, occurrences(engine.calls.toString(), "getEventDetails(0)"));
        assertEquals(1, occurrences(engine.calls.toString(), "getEventDetails(-9)"));
    }

    @Test
    void loadedSystemWithNoOpenEventsDoesNotPromptOrCallMutatingOperations() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        engine.eventSummaries = List.of(summary(5, "Closed", EventStatus.CLOSED));

        String output = run(engine, "4\n5\n8\n");

        assertEquals(List.of("listEvents()", "listEvents()"), engine.calls);
        assertTrue(output.contains("No open events are available for purchasing shares."));
        assertTrue(output.contains("No open events are available to close."));
        assertFalse(output.contains("Choose an event [1-"));
    }

    @Test
    void failedPurchaseAndCloseRenderRecoveryAndDoNotRenderSuccessResults() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        engine.eventSummaries = List.of(summary(7, "Open", EventStatus.OPEN));
        engine.detailsById.put(7, details(7, "Open", EventStatus.OPEN,
                CommissionMode.ON_PURCHASE, OptionalInt.empty(), List.of()));
        engine.failWith(FakeGuessMarketEngine.PURCHASE, failure(EngineErrorCode.INVALID_QUANTITY));
        engine.failWith(FakeGuessMarketEngine.CLOSE, failure(EngineErrorCode.EVENT_NOT_OPEN));

        String output = run(engine, "4\n1\n1\n3\n5\n1\n2\n8\n");

        assertEquals(List.of(
                "listEvents()", "getEventDetails(7)", "purchaseShares(7,1,3)",
                "listEvents()", "getEventDetails(7)", "closeEvent(7,2)"), engine.calls);
        assertTrue(output.contains("Error: The purchase quantity is invalid."));
        assertTrue(output.contains("Recovery: Start the purchase again with a smaller positive whole number."));
        assertTrue(output.contains("Error: The event is closed."));
        assertTrue(output.contains("Recovery: Choose an open event."));
        assertFalse(output.contains("Purchase completed successfully."));
        assertFalse(output.contains("Event closed successfully."));
    }

    @Test
    void unexpectedRuntimeEngineDefectRemainsVisible() {
        FakeGuessMarketEngine engine = new FakeGuessMarketEngine();
        IllegalStateException defect = new IllegalStateException("visible programming defect");
        engine.throwDefectOn(FakeGuessMarketEngine.LIST, defect);
        StringWriter text = new StringWriter();
        GuessMarketConsoleApp app = app(engine, "2\n", text);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, app::run);

        assertSame(defect, thrown);
        assertFalse(text.toString().contains("Error:"));
        assertFalse(text.toString().contains("Recovery:"));
    }

    @Test
    void consoleMainIsThePublicStaticVoidEntryPoint() throws Exception {
        Method main = ConsoleMain.class.getDeclaredMethod("main", String[].class);

        assertTrue(Modifier.isPublic(ConsoleMain.class.getModifiers()));
        assertTrue(Modifier.isFinal(ConsoleMain.class.getModifiers()));
        assertTrue(Modifier.isPublic(main.getModifiers()));
        assertTrue(Modifier.isStatic(main.getModifiers()));
        assertEquals(void.class, main.getReturnType());
    }

    private static String run(FakeGuessMarketEngine engine, String script) {
        StringWriter text = new StringWriter();
        app(engine, script, text).run();
        return text.toString();
    }

    private static GuessMarketConsoleApp app(
            FakeGuessMarketEngine engine,
            String script,
            StringWriter text) {
        PrintWriter output = new PrintWriter(text, true);
        return new GuessMarketConsoleApp(
                engine,
                new ConsoleInput(new Scanner(script), output),
                new ConsoleRenderer(output));
    }

    private static MarketEventSummary summary(int eventId, String name, EventStatus status) {
        return new MarketEventSummary(
                eventId,
                name,
                "Description for " + name,
                10,
                CommissionMode.ON_PURCHASE,
                List.of("Home", "Away"),
                status);
    }

    private static MarketEventDetails details(
            int eventId,
            String name,
            EventStatus status,
            CommissionMode commissionMode,
            OptionalInt winner,
            List<TradeHistoryEntry> history) {
        return new MarketEventDetails(
                eventId,
                name,
                "Detailed description for " + name,
                10,
                commissionMode,
                status,
                List.of(
                        new MarketOptionSnapshot(1, "Home", 3, 0.6),
                        new MarketOptionSnapshot(2, "Away", 2, 0.4)),
                status == EventStatus.CLOSED ? -1.25 : 2.5,
                commissionMode == CommissionMode.ON_PURCHASE ? 0.25 : 0.0,
                history,
                winner);
    }

    private static EngineOperationException failure(EngineErrorCode code) {
        return new EngineOperationException(code, "test failure", "test recovery");
    }

    private static void assertExactlyOneBlankLineBeforeEachRepeatedMenu(String output) {
        int menuIndex = output.indexOf(MENU);
        assertEquals(0, menuIndex);
        menuIndex = output.indexOf(MENU, MENU.length());
        while (menuIndex >= 0) {
            assertTrue(menuIndex >= 2);
            assertEquals("\n\n", output.substring(menuIndex - 2, menuIndex));
            if (menuIndex >= 3) {
                assertFalse(output.charAt(menuIndex - 3) == '\n');
            }
            menuIndex = output.indexOf(MENU, menuIndex + MENU.length());
        }
    }

    private static int occurrences(String value, String fragment) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(fragment, index)) >= 0) {
            count++;
            index += fragment.length();
        }
        return count;
    }
}
