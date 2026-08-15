package guessmarket.engine;

import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.PurchaseReceipt;
import java.nio.file.Path;
import java.util.List;

public interface GuessMarketEngine {
    int loadEventsFromXml(Path path) throws EngineOperationException;

    List<MarketEventSummary> listEvents() throws EngineOperationException;

    MarketEventDetails getEventDetails(int eventId) throws EngineOperationException;

    PurchaseReceipt purchaseShares(int eventId, int optionNumber, int quantity)
            throws EngineOperationException;

    MarketEventDetails closeEvent(int eventId, int winningOptionNumber)
            throws EngineOperationException;

    void saveState(Path path) throws EngineOperationException;

    int restoreState(Path path) throws EngineOperationException;
}
