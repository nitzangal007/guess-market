package guessmarket.engine;

import guessmarket.dto.world.OpeningPreview;
import guessmarket.dto.world.WorldSnapshot;
import guessmarket.dto.world.PurchasePreview;
import guessmarket.dto.world.PurchaseResult;
import guessmarket.dto.world.ClosePreview;
import guessmarket.dto.world.CloseResult;
import java.nio.file.Path;

public interface GuessMarketWorldEngine {
    WorldSnapshot loadWorldFromXml(Path path) throws EngineOperationException;
    WorldSnapshot getWorldSnapshot() throws EngineOperationException;
    ClosePreview previewLmsrClose(String actingUser,int eventId,int winningOption)
            throws EngineOperationException,WorldCommandException;
    CloseResult closeLmsrEvent(String actingUser,int eventId,int winningOption,long expectedWorldRevision)
            throws EngineOperationException,WorldCommandException;
    PurchasePreview previewLmsrPurchase(String actingUser, int eventId, int optionNumber, int quantity)
            throws EngineOperationException, WorldCommandException;
    PurchaseResult purchaseLmsrShares(String actingUser, int eventId, int optionNumber, int quantity,
                                    long expectedWorldRevision) throws EngineOperationException, WorldCommandException;
    OpeningPreview previewLmsrOpening(String actingUser, int eventId)
            throws EngineOperationException, WorldCommandException;
    WorldSnapshot openLmsrEvent(String actingUser, int eventId, long expectedWorldRevision)
            throws EngineOperationException, WorldCommandException;
}
