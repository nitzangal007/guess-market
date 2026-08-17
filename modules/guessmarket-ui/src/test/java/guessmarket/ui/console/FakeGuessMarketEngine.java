package guessmarket.ui.console;

import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.PurchaseReceipt;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.GuessMarketEngine;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FakeGuessMarketEngine implements GuessMarketEngine {
    static final String LOAD = "loadEventsFromXml";
    static final String LIST = "listEvents";
    static final String DETAILS = "getEventDetails";
    static final String PURCHASE = "purchaseShares";
    static final String CLOSE = "closeEvent";
    static final String SAVE = "saveState";
    static final String RESTORE = "restoreState";

    final List<String> calls = new ArrayList<>();
    final Map<Integer, MarketEventDetails> detailsById = new HashMap<>();
    private final Map<String, EngineOperationException> checkedFailures = new HashMap<>();
    private final Map<String, RuntimeException> runtimeDefects = new HashMap<>();

    int loadedEventCount;
    int restoredEventCount;
    List<MarketEventSummary> eventSummaries = List.of();
    PurchaseReceipt purchaseReceipt;
    MarketEventDetails closeResult;

    void failWith(String operation, EngineOperationException exception) {
        checkedFailures.put(operation, exception);
    }

    void throwDefectOn(String operation, RuntimeException defect) {
        runtimeDefects.put(operation, defect);
    }

    @Override
    public int loadEventsFromXml(Path path) throws EngineOperationException {
        before(LOAD, LOAD + "(" + path + ")");
        return loadedEventCount;
    }

    @Override
    public List<MarketEventSummary> listEvents() throws EngineOperationException {
        before(LIST, LIST + "()");
        return eventSummaries;
    }

    @Override
    public MarketEventDetails getEventDetails(int eventId) throws EngineOperationException {
        before(DETAILS, DETAILS + "(" + eventId + ")");
        MarketEventDetails details = detailsById.get(eventId);
        if (details == null) {
            throw new IllegalStateException("No event details configured for ID " + eventId);
        }
        return details;
    }

    @Override
    public PurchaseReceipt purchaseShares(int eventId, int optionNumber, int quantity)
            throws EngineOperationException {
        before(PURCHASE, PURCHASE + "(" + eventId + "," + optionNumber + "," + quantity + ")");
        if (purchaseReceipt == null) {
            throw new IllegalStateException("No purchase receipt configured");
        }
        return purchaseReceipt;
    }

    @Override
    public MarketEventDetails closeEvent(int eventId, int winningOptionNumber)
            throws EngineOperationException {
        before(CLOSE, CLOSE + "(" + eventId + "," + winningOptionNumber + ")");
        if (closeResult == null) {
            throw new IllegalStateException("No close result configured");
        }
        return closeResult;
    }

    @Override
    public void saveState(Path path) throws EngineOperationException {
        before(SAVE, SAVE + "(" + path + ")");
    }

    @Override
    public int restoreState(Path path) throws EngineOperationException {
        before(RESTORE, RESTORE + "(" + path + ")");
        return restoredEventCount;
    }

    private void before(String operation, String call) throws EngineOperationException {
        calls.add(call);
        RuntimeException defect = runtimeDefects.get(operation);
        if (defect != null) {
            throw defect;
        }
        EngineOperationException failure = checkedFailures.get(operation);
        if (failure != null) {
            throw failure;
        }
    }
}
