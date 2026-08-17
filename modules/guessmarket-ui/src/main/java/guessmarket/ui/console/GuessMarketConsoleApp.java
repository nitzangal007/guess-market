package guessmarket.ui.console;

import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.PurchaseReceipt;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.GuessMarketEngine;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class GuessMarketConsoleApp {
    private final GuessMarketEngine engine;
    private final ConsoleInput input;
    private final ConsoleRenderer renderer;

    GuessMarketConsoleApp(
            GuessMarketEngine engine,
            ConsoleInput input,
            ConsoleRenderer renderer) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.input = Objects.requireNonNull(input, "input");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    void run() {
        boolean returningToMenu = false;
        try {
            while (true) {
                if (returningToMenu) {
                    renderer.renderBlankLine();
                }
                renderer.renderMenu();
                returningToMenu = true;

                int choice = input.readMenuChoice();
                try {
                    switch (choice) {
                        case 1 -> loadEvents();
                        case 2 -> displayAllEvents();
                        case 3 -> displayEventDetails();
                        case 4 -> purchaseShares();
                        case 5 -> closeEvent();
                        case 6 -> saveState();
                        case 7 -> restoreState();
                        case 8 -> {
                            renderer.renderGoodbye();
                            return;
                        }
                        default -> {
                            // ConsoleInput already reported the invalid one-shot menu choice.
                        }
                    }
                } catch (EngineOperationException exception) {
                    renderer.renderError(exception);
                }
            }
        } catch (ConsoleInput.EndOfInputException exception) {
            renderer.renderInputClosed();
        }
    }

    private void loadEvents()
            throws EngineOperationException, ConsoleInput.EndOfInputException {
        Path path = input.readXmlPath();
        int loadedEventCount = engine.loadEventsFromXml(path);
        renderer.renderLoadSuccess(loadedEventCount);
    }

    private void displayAllEvents() throws EngineOperationException {
        renderer.renderEventSummaries(engine.listEvents(), false);
    }

    private void displayEventDetails()
            throws EngineOperationException, ConsoleInput.EndOfInputException {
        List<MarketEventSummary> summaries = engine.listEvents();
        renderer.renderEventSummaries(summaries, false);
        int eventId = selectedEventId(summaries);
        renderer.renderEventDetails(engine.getEventDetails(eventId));
    }

    private void purchaseShares()
            throws EngineOperationException, ConsoleInput.EndOfInputException {
        List<MarketEventSummary> openEvents = openEvents(engine.listEvents());
        if (openEvents.isEmpty()) {
            renderer.renderNoOpenEventsForPurchase();
            return;
        }

        renderer.renderEventSummaries(openEvents, true);
        int eventId = selectedEventId(openEvents);
        MarketEventDetails currentDetails = engine.getEventDetails(eventId);
        renderer.renderEventDetails(currentDetails);
        int optionNumber = input.readPurchaseOption();
        int quantity = input.readPositiveQuantity();
        PurchaseReceipt receipt = engine.purchaseShares(eventId, optionNumber, quantity);
        renderer.renderPurchaseReceipt(receipt);
    }

    private void closeEvent()
            throws EngineOperationException, ConsoleInput.EndOfInputException {
        List<MarketEventSummary> openEvents = openEvents(engine.listEvents());
        if (openEvents.isEmpty()) {
            renderer.renderNoOpenEventsForClose();
            return;
        }

        renderer.renderEventSummaries(openEvents, true);
        int eventId = selectedEventId(openEvents);
        MarketEventDetails currentDetails = engine.getEventDetails(eventId);
        renderer.renderEventDetails(currentDetails);
        int winningOptionNumber = input.readWinningOption();
        MarketEventDetails finalDetails = engine.closeEvent(eventId, winningOptionNumber);
        renderer.renderFinalEventStatus(finalDetails);
    }

    private void saveState()
            throws EngineOperationException, ConsoleInput.EndOfInputException {
        Path path = input.readSavePath();
        engine.saveState(path);
        renderer.renderSaveSuccess();
    }

    private void restoreState()
            throws EngineOperationException, ConsoleInput.EndOfInputException {
        Path path = input.readRestorePath();
        int restoredEventCount = engine.restoreState(path);
        renderer.renderRestoreSuccess(restoredEventCount);
    }

    private int selectedEventId(List<MarketEventSummary> summaries)
            throws ConsoleInput.EndOfInputException {
        int position = input.readEventSelection(summaries.size());
        return summaries.get(position - 1).getEventId();
    }

    private List<MarketEventSummary> openEvents(List<MarketEventSummary> summaries) {
        List<MarketEventSummary> openEvents = new ArrayList<>();
        for (MarketEventSummary summary : summaries) {
            if (summary.getStatus() == EventStatus.OPEN) {
                openEvents.add(summary);
            }
        }
        return List.copyOf(openEvents);
    }
}
