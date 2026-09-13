package guessmarket.engine;

import guessmarket.dto.world.OpeningPreview;
import guessmarket.dto.world.WorldSnapshot;
import guessmarket.dto.world.PurchasePreview;
import guessmarket.dto.world.PurchaseResult;
import guessmarket.dto.world.ClosePreview;
import guessmarket.dto.world.CloseResult;
import java.nio.file.Path;
import guessmarket.dto.world.OrderRequest;
import guessmarket.dto.world.OrderPreview;
import guessmarket.dto.world.OrderResult;

public interface GuessMarketWorldEngine {
    OrderPreview previewOrder(OrderRequest request)throws EngineOperationException,WorldCommandException;
    OrderResult submitOrder(OrderRequest request,long expectedWorldRevision)throws EngineOperationException,WorldCommandException;
    ClosePreview previewOrderBookClose(String actor,int eventId,int winner)throws EngineOperationException,WorldCommandException;
    CloseResult closeOrderBookEvent(String actor,int eventId,int winner,long expectedWorldRevision)throws EngineOperationException,WorldCommandException;
    /** Opening requires initial funding divisible into whole pairs. */
    OpeningPreview previewOrderBookOpening(String actingUser,int eventId)
            throws EngineOperationException,WorldCommandException;
    WorldSnapshot openOrderBookEvent(String actingUser,int eventId,long expectedWorldRevision)
            throws EngineOperationException,WorldCommandException;
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
