package guessmarket.ui.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ConsoleRendererTest {
    private static final String SEPARATOR = "------------------------------------------------------------";

    @Test
    void menuIsTheCompleteD072Literal() {
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);

        renderer.renderMenu();

        assertEquals("""
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
                """, text.toString());
    }

    @Test
    void summariesUseCleanSectionsFreshNumberingAndNoTruncation() {
        String longDescription = "This deliberately long XML description must remain complete without wrapping, truncation, or a width calculation.";
        List<MarketEventSummary> summaries = List.of(
                new MarketEventSummary(-9, "First", longDescription, 5, CommissionMode.ON_PURCHASE,
                        List.of("Yes", "No"), EventStatus.OPEN),
                new MarketEventSummary(80, "Second", "Closed event", 0, CommissionMode.ON_CLOSE,
                        List.of("Home", "Away"), EventStatus.CLOSED));
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);

        renderer.renderEventSummaries(summaries, false);
        renderer.renderEventSummaries(List.of(summaries.get(0)), true);

        String output = text.toString();
        assertTrue(output.contains(SEPARATOR + "\nEVENTS\n" + SEPARATOR));
        assertTrue(output.contains(SEPARATOR + "\nOPEN EVENTS\n" + SEPARATOR));
        assertTrue(output.contains("1. Event ID: -9"));
        assertTrue(output.contains("2. Event ID: 80"));
        assertTrue(output.contains(longDescription));
        assertTrue(output.contains("Commission percentage: 5%\n   Commission method: On purchase"));
        assertTrue(output.contains("Commission percentage: 0%\n   Commission method: On close"));
        assertEquals(2, occurrences(output, "1. Event ID: -9"));
    }

    @Test
    void detailsRenderOpenAndClosedShapesIncludingCommissionAccountAndHistory() {
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);
        MarketEventDetails open = details(EventStatus.OPEN, CommissionMode.ON_PURCHASE, 10, 3.25, 0.25,
                List.of(history(2, "No", 2, 1.2, 0.12, 1.32), history(1, "Yes", 1, 0.5, 0.05, 0.55)), OptionalInt.empty());
        MarketEventDetails closedProfit = details(EventStatus.CLOSED, CommissionMode.ON_CLOSE, 15, 4.0, 0.0,
                List.of(history(1, "Yes", 1, 1.0, 0.0, 1.0)), OptionalInt.of(2));

        renderer.renderEventDetails(open);
        renderer.renderEventDetails(closedProfit);

        String output = text.toString();
        assertTrue(output.contains("EVENT DETAILS"));
        assertTrue(output.contains("ID: -11"));
        assertTrue(output.contains("Status: OPEN"));
        assertTrue(output.contains("Commission: 10% on purchase"));
        assertTrue(output.contains("Commission: 15% on close"));
        assertTrue(output.contains("OPTIONS\n  1. Yes\n     Price: 0.73   Shares purchased: 3"));
        assertTrue(output.contains("ACCOUNT\n  Event account balance: 3.25\n  Total commission collected: 0.25"));
        assertTrue(output.contains("PURCHASE HISTORY\n  1. Option: 2 - No"));
        assertTrue(output.contains("  2. Option: 1 - Yes"));
        assertTrue(output.contains("Winner: 2 - No"));
        assertTrue(output.contains("Final market-maker result: Profit 4.00"));
        assertFalse(output.contains("Winner: 0"));
    }

    @Test
    void closedDetailsClassifySubsidyBreakEvenAndSignedZeroFromUnroundedBalance() {
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);

        renderer.renderEventDetails(details(EventStatus.CLOSED, CommissionMode.ON_CLOSE, 0, -0.004, 0.0, List.of(), OptionalInt.of(1)));
        renderer.renderEventDetails(details(EventStatus.CLOSED, CommissionMode.ON_CLOSE, 0, -0.0, 0.0, List.of(), OptionalInt.of(1)));

        String output = text.toString();
        assertTrue(output.contains("Final market-maker result: Subsidy 0.00"));
        assertTrue(output.contains("Final market-maker result: Break-even 0.00"));
        assertTrue(output.contains("No purchases have been made."));
    }

    @Test
    void receiptAndFinalStatusUseOnlyApprovedTopLevelHeadings() {
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);
        MarketEventDetails details = details(EventStatus.OPEN, CommissionMode.ON_PURCHASE, 10, 1.0, 0.1, List.of(), OptionalInt.empty());
        PurchaseReceipt receipt = new PurchaseReceipt(1, 2, 1.0, 0.1, 1.1, details);

        renderer.renderPurchaseReceipt(receipt);
        renderer.renderFinalEventStatus(details(EventStatus.CLOSED, CommissionMode.ON_CLOSE, 0, 0.0, 0.0, List.of(), OptionalInt.of(1)));

        String output = text.toString();
        assertTrue(output.contains("Purchase completed successfully."));
        assertTrue(output.contains("PURCHASE SUMMARY"));
        assertTrue(output.contains("UPDATED EVENT STATUS"));
        assertTrue(output.contains("Event closed successfully."));
        assertTrue(output.contains("FINAL EVENT STATUS"));
        assertTrue(output.contains("Option: 1 - Yes"));
        assertTrue(output.contains("Purchase commission: 0.10"));
    }

    @Test
    void emptyOpenSetsAndAllApprovedSessionMessagesArePlainEnglish() {
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);

        renderer.renderNoOpenEventsForPurchase();
        renderer.renderNoOpenEventsForClose();
        renderer.renderLoadSuccess(2);
        renderer.renderSaveSuccess();
        renderer.renderRestoreSuccess(3);
        renderer.renderGoodbye();
        renderer.renderInputClosed();

        assertEquals("""
                No open events are available for purchasing shares.
                No open events are available to close.
                System loaded successfully.
                Loaded events: 2
                System state saved successfully.
                System state restored successfully.
                Restored events: 3
                Goodbye.
                Input closed. Exiting.
                """, text.toString());
    }

    @Test
    void formatterUsesUsFixedDecimalsNormalizesNegativeZeroAndRejectsNonFiniteValues() {
        ConsoleRenderer renderer = renderer(new StringWriter());

        assertEquals("12.50", renderer.formatFinancial(12.5));
        assertEquals("-12.35", renderer.formatFinancial(-12.345));
        assertEquals("0.00", renderer.formatFinancial(0.0000001));
        assertEquals("0.00", renderer.formatFinancial(-0.0));
        assertEquals("0.00", renderer.formatFinancial(-0.004));
        assertThrows(IllegalArgumentException.class, () -> renderer.formatFinancial(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> renderer.formatFinancial(Double.POSITIVE_INFINITY));
    }

    @Test
    void everyEngineCodeMapsToAsciiErrorAndRecoveryWithoutRawCauseOrAbsentContext() {
        StringWriter text = new StringWriter();
        ConsoleRenderer renderer = renderer(text);
        for (EngineErrorCode code : EngineErrorCode.values()) {
            renderer.renderError(new EngineOperationException(code, "raw low-level cause text", "raw recovery hint"));
        }
        renderer.renderError(new EngineOperationException(
                EngineErrorCode.EVENT_NOT_FOUND,
                "ignored raw cause",
                "ignored raw hint",
                Path.of("C:\\market files\\missing.xml"),
                null,
                -7,
                null,
                null,
                null,
                null,
                null,
                null,
                new IllegalStateException("hidden stack detail")));

        String output = text.toString();
        assertEquals(17, occurrences(output, "Error: "));
        assertEquals(17, occurrences(output, "Recovery: "));
        assertTrue(output.contains("Event ID -7 does not exist."));
        assertFalse(output.contains("raw low-level cause text"));
        assertFalse(output.contains("raw recovery hint"));
        assertFalse(output.contains("hidden stack detail"));
        assertFalse(output.contains("null"));
        assertFalse(output.contains("\u001B"));
        assertFalse(output.contains("\u2550"));
        assertFalse(output.contains("\u001B[2J"));
    }

    private static ConsoleRenderer renderer(StringWriter text) {
        return new ConsoleRenderer(new PrintWriter(text, true));
    }

    private static MarketEventDetails details(
            EventStatus status,
            CommissionMode commissionMode,
            int commissionPercentage,
            double balance,
            double collected,
            List<TradeHistoryEntry> history,
            OptionalInt winner) {
        return new MarketEventDetails(
                -11,
                "An event",
                "A detailed description with spaces.",
                commissionPercentage,
                commissionMode,
                status,
                List.of(
                        new MarketOptionSnapshot(1, "Yes", 3, 0.731058578630005),
                        new MarketOptionSnapshot(2, "No", 1, 0.268941421369995)),
                balance,
                collected,
                history,
                winner);
    }

    private static TradeHistoryEntry history(
            int optionNumber,
            String label,
            int quantity,
            double baseCost,
            double commission,
            double total) {
        return new TradeHistoryEntry(optionNumber, label, quantity, baseCost, commission, total);
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
