package guessmarket.ui.console;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class ConsoleRenderer {
    private static final String EQUALS_SEPARATOR = "============================================================";
    private static final String HYPHEN_SEPARATOR = "------------------------------------------------------------";

    private final PrintWriter output;

    ConsoleRenderer(PrintWriter output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    void renderMenu() {
        writeLine(EQUALS_SEPARATOR);
        writeLine("                       GUESS MARKET");
        writeLine(EQUALS_SEPARATOR);
        writeLine("");
        writeLine("DATA");
        writeLine("  1. Load events from XML");
        writeLine("  2. Display all events");
        writeLine("  3. View an event's trading status");
        writeLine("");
        writeLine("TRADING");
        writeLine("  4. Purchase shares");
        writeLine("  5. Close an event");
        writeLine("");
        writeLine("STATE AND SESSION");
        writeLine("  6. Save current state");
        writeLine("  7. Restore saved state");
        writeLine("  8. Exit");
        writeLine("");
        writeLine("Commands 2 through 6 require a loaded system.");
        writeLine("");
        writeLine("Choose a command [1-8]:");
    }

    void renderEventSummaries(List<MarketEventSummary> summaries, boolean openEvents) {
        Objects.requireNonNull(summaries, "summaries");
        renderTopLevelTitle(openEvents ? "OPEN EVENTS" : "EVENTS");
        for (int index = 0; index < summaries.size(); index++) {
            if (index > 0) {
                writeLine("");
            }
            renderSummary(index + 1, summaries.get(index));
        }
    }

    void renderEventDetails(MarketEventDetails details) {
        renderTopLevelTitle("EVENT DETAILS");
        renderDetailsBody(details);
    }

    void renderPurchaseReceipt(PurchaseReceipt receipt) {
        Objects.requireNonNull(receipt, "receipt");
        MarketEventDetails details = receipt.getResultingEventDetails();
        writeLine("Purchase completed successfully.");
        writeLine("");
        renderTopLevelTitle("PURCHASE SUMMARY");
        MarketOptionSnapshot option = optionFor(details, receipt.getOptionNumber());
        writeLine("Option: " + receipt.getOptionNumber() + " - " + option.getLabel());
        writeLine("Shares purchased: " + receipt.getPurchasedShareQuantity());
        writeLine("Base share cost: " + formatFinancial(receipt.getBaseShareCost()));
        if (details.getCommissionMode() == CommissionMode.ON_PURCHASE) {
            writeLine("Purchase commission: " + formatFinancial(receipt.getPurchaseCommission()));
        }
        writeLine("Total paid: " + formatFinancial(receipt.getTotalPaid()));
        writeLine("");
        renderTopLevelTitle("UPDATED EVENT STATUS");
        renderDetailsBody(details);
    }

    void renderFinalEventStatus(MarketEventDetails details) {
        writeLine("Event closed successfully.");
        writeLine("");
        renderTopLevelTitle("FINAL EVENT STATUS");
        renderDetailsBody(details);
    }

    void renderNoOpenEventsForPurchase() {
        writeLine("No open events are available for purchasing shares.");
    }

    void renderNoOpenEventsForClose() {
        writeLine("No open events are available to close.");
    }

    void renderLoadSuccess(int eventCount) {
        writeLine("System loaded successfully.");
        writeLine("Loaded events: " + eventCount);
    }

    void renderSaveSuccess() {
        writeLine("System state saved successfully.");
    }

    void renderRestoreSuccess(int eventCount) {
        writeLine("System state restored successfully.");
        writeLine("Restored events: " + eventCount);
    }

    void renderGoodbye() {
        writeLine("Goodbye.");
    }

    void renderInputClosed() {
        writeLine("Input closed. Exiting.");
    }

    void renderError(EngineOperationException exception) {
        Objects.requireNonNull(exception, "exception");
        ErrorPresentation presentation = errorPresentation(exception);
        writeLine("Error: " + presentation.message());
        writeLine("Recovery: " + presentation.recovery());
    }

    String formatFinancial(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Financial value must be finite");
        }
        String formatted = String.format(Locale.US, "%.2f", value);
        return "-0.00".equals(formatted) ? "0.00" : formatted;
    }

    private void renderTopLevelTitle(String title) {
        writeLine(HYPHEN_SEPARATOR);
        writeLine(title);
        writeLine(HYPHEN_SEPARATOR);
    }

    private void renderSummary(int position, MarketEventSummary summary) {
        Objects.requireNonNull(summary, "summary");
        writeLine(position + ". Event ID: " + summary.getEventId());
        writeLine("   Name: " + summary.getName());
        writeLine("   Description: " + summary.getDescription());
        writeLine("   Commission percentage: " + summary.getCommissionPercentage() + "%");
        writeLine("   Commission method: " + commissionMethod(summary.getCommissionMode()));
        writeLine("   Options:");
        writeLine("     1. " + summary.getOptionLabels().get(0));
        writeLine("     2. " + summary.getOptionLabels().get(1));
        writeLine("   Status: " + summary.getStatus());
    }

    private void renderDetailsBody(MarketEventDetails details) {
        Objects.requireNonNull(details, "details");
        writeLine("ID: " + details.getEventId());
        writeLine("Name: " + details.getName());
        writeLine("Description: " + details.getDescription());
        writeLine("Status: " + details.getStatus());
        if (details.getWinningOptionNumber().isPresent()) {
            int winner = details.getWinningOptionNumber().getAsInt();
            writeLine("Winner: " + winner + " - " + optionFor(details, winner).getLabel());
        }
        writeLine("Commission: " + details.getCommissionPercentage() + "% "
                + commissionMethod(details.getCommissionMode()).toLowerCase(Locale.ROOT));
        writeLine("");
        writeLine("OPTIONS");
        for (MarketOptionSnapshot option : details.getOptions()) {
            writeLine("  " + option.getOptionNumber() + ". " + option.getLabel());
            writeLine("     Price: " + formatFinancial(option.getCurrentPrice())
                    + "   Shares purchased: " + option.getShareQuantity());
        }
        writeLine("");
        writeLine("ACCOUNT");
        writeLine("  Event account balance: " + formatFinancial(details.getEventAccountBalance()));
        writeLine("  Total commission collected: " + formatFinancial(details.getTotalCommissionCollected()));
        if (details.getStatus() == EventStatus.CLOSED) {
            writeLine("  " + finalResult(details.getEventAccountBalance()));
        }
        writeLine("");
        writeLine("PURCHASE HISTORY");
        renderPurchaseHistory(details.getPurchaseHistory(), details.getCommissionMode());
    }

    private void renderPurchaseHistory(List<TradeHistoryEntry> history, CommissionMode commissionMode) {
        if (history.isEmpty()) {
            writeLine("  No purchases have been made.");
            return;
        }
        for (int index = 0; index < history.size(); index++) {
            TradeHistoryEntry entry = history.get(index);
            writeLine("  " + (index + 1) + ". Option: " + entry.getOptionNumber() + " - " + entry.getOptionLabel());
            writeLine("     Shares purchased: " + entry.getShareQuantity());
            writeLine("     Base share cost: " + formatFinancial(entry.getBaseShareCost()));
            if (commissionMode == CommissionMode.ON_PURCHASE) {
                writeLine("     Purchase commission: " + formatFinancial(entry.getPurchaseCommission()));
            }
            writeLine("     Total paid: " + formatFinancial(entry.getTotalPaid()));
        }
    }

    private String finalResult(double balance) {
        if (balance > 0.0) {
            return "Final market-maker result: Profit " + formatFinancial(balance);
        }
        if (balance < 0.0) {
            return "Final market-maker result: Subsidy " + formatFinancial(Math.abs(balance));
        }
        return "Final market-maker result: Break-even 0.00";
    }

    private ErrorPresentation errorPresentation(EngineOperationException exception) {
        return switch (exception.getCode()) {
            case NO_SYSTEM_LOADED -> new ErrorPresentation(
                    "No system is loaded.", "Load XML or restore saved state first.");
            case EVENT_NOT_FOUND -> new ErrorPresentation(
                    exception.getEventId().isPresent()
                            ? "Event ID " + exception.getEventId().getAsInt() + " does not exist."
                            : "The event does not exist.",
                    "Choose an event from the displayed list.");
            case EVENT_NOT_OPEN -> new ErrorPresentation(
                    exception.getEventId().isPresent()
                            ? "Event ID " + exception.getEventId().getAsInt() + " is closed."
                            : "The event is closed.",
                    "Choose an open event.");
            case INVALID_OPTION -> new ErrorPresentation(
                    exception.getOptionNumber().isPresent()
                            ? "Option " + exception.getOptionNumber().getAsInt() + " is invalid."
                            : "The option is invalid.",
                    "Choose option 1 or 2.");
            case INVALID_QUANTITY -> new ErrorPresentation(
                    "The purchase quantity is invalid.",
                    "Start the purchase again with a smaller positive whole number.");
            case FINANCIAL_CALCULATION_FAILED -> new ErrorPresentation(
                    "The operation exceeded the supported financial range.",
                    "Review event data and, for purchases, try a smaller quantity.");
            case INVALID_XML_PATH -> new ErrorPresentation(
                    "The XML path is invalid.", "Choose a regular .xml file.");
            case XML_FILE_NOT_FOUND -> new ErrorPresentation(
                    "The XML file was not found" + optionalPathSuffix(exception) + ".", "Check the path.");
            case XML_FILE_ACCESS_FAILED -> new ErrorPresentation(
                    "The XML file could not be read.", "Check access and try again.");
            case XML_STRUCTURE_INVALID -> new ErrorPresentation(
                    "The file is not valid Exercise 1 XML.", "Correct its XML or schema structure.");
            case XML_DATA_INVALID -> new ErrorPresentation(
                    "The XML contains invalid market data" + optionalXmlContextSuffix(exception) + ".",
                    "Correct the event data and try again.");
            case ENGINE_CONFIGURATION_ERROR -> new ErrorPresentation(
                    "Internal XML configuration is unavailable.", "Verify Engine packaging and JAXB dependencies.");
            case INVALID_STATE_PATH -> new ErrorPresentation(
                    "The saved-state path is invalid.", "Enter a valid base path and file name.");
            case STATE_FILE_NOT_FOUND -> new ErrorPresentation(
                    "The saved-state file was not found" + optionalPathSuffix(exception) + ".", "Check the path.");
            case STATE_FILE_ACCESS_FAILED -> new ErrorPresentation(
                    "The saved-state file could not be read or written.", "Check access and try again.");
            case SAVED_STATE_INVALID -> new ErrorPresentation(
                    "The saved-state file is invalid or incompatible.", "Choose a compatible valid file.");
        };
    }

    private String optionalPathSuffix(EngineOperationException exception) {
        return exception.getPath().map(path -> " at " + path).orElse("");
    }

    private String optionalXmlContextSuffix(EngineOperationException exception) {
        StringBuilder context = new StringBuilder();
        exception.getXmlEventNumber().ifPresent(number -> context.append(" in XML event ").append(number));
        exception.getFieldName().ifPresent(field -> context.append(context.isEmpty() ? " for field " : ", field ").append(field));
        exception.getLineNumber().ifPresent(line -> context.append(context.isEmpty() ? " at line " : ", line ").append(line));
        exception.getColumnNumber().ifPresent(column -> context.append(context.isEmpty() ? " at column " : ", column ").append(column));
        return context.toString();
    }

    private MarketOptionSnapshot optionFor(MarketEventDetails details, int optionNumber) {
        for (MarketOptionSnapshot option : details.getOptions()) {
            if (option.getOptionNumber() == optionNumber) {
                return option;
            }
        }
        throw new IllegalArgumentException("Details do not contain the requested option");
    }

    private String commissionMethod(CommissionMode mode) {
        return mode == CommissionMode.ON_PURCHASE ? "On purchase" : "On close";
    }

    private void writeLine(String text) {
        output.print(text);
        output.print('\n');
        output.flush();
    }

    private record ErrorPresentation(String message, String recovery) {
    }
}
