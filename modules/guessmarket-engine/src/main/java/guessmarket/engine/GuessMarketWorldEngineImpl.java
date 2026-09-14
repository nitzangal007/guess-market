package guessmarket.engine;

import guessmarket.dto.world.OpeningPreview;
import guessmarket.dto.world.WorldSnapshot;
import guessmarket.dto.world.PurchasePreview;
import guessmarket.dto.world.PurchaseResult;
import guessmarket.dto.world.ClosePreview;
import guessmarket.dto.world.CloseResult;
import guessmarket.engine.xml.ex2.Ex2XmlWorldLoader;
import java.nio.file.Path;
import guessmarket.dto.world.OrderRequest;
import guessmarket.dto.world.OrderPreview;
import guessmarket.dto.world.OrderResult;

public final class GuessMarketWorldEngineImpl implements GuessMarketWorldEngine {
    private final Ex2XmlWorldLoader loader = new Ex2XmlWorldLoader();
    private MarketWorld activeWorld;
    private long revision;

    @Override public synchronized OrderPreview previewOrder(OrderRequest request)throws EngineOperationException,WorldCommandException {
        requireWorld();return activeWorld.planOrder(request,revision).preview();
    }
    @Override public synchronized OrderResult submitOrder(OrderRequest request,long expectedWorldRevision)throws EngineOperationException,WorldCommandException {
        requireWorld();requireRevision(expectedWorldRevision);
        var candidate=activeWorld.planOrder(request,revision);
        return new OrderResult(publish(candidate.world()),candidate.preview());
    }
    @Override public synchronized ClosePreview previewOrderBookClose(String actor,int eventId,int winner)throws EngineOperationException,WorldCommandException {
        requireWorld();return activeWorld.previewOrderBookClose(actor,eventId,winner,revision);
    }
    @Override public synchronized CloseResult closeOrderBookEvent(String actor,int eventId,int winner,long expectedWorldRevision)throws EngineOperationException,WorldCommandException {
        requireWorld();requireRevision(expectedWorldRevision);
        var candidate=activeWorld.closeOrderBookEvent(actor,eventId,winner,revision);
        var snapshot=candidate.snapshot();
        var settlement=snapshot.events().stream().filter(e->e.id()==eventId).findFirst().orElseThrow().orderBook().orElseThrow().settlement().orElseThrow();
        var result=new CloseResult(snapshot,settlement);
        publish(candidate);return result;
    }
    private void requireRevision(long expected)throws WorldCommandException {
        if(expected!=revision)throw new WorldCommandException(WorldCommandException.Code.STALE_WORLD,"The world changed. Review this operation again.");
    }

    @Override public synchronized OpeningPreview previewOrderBookOpening(String actingUser,int eventId)
            throws EngineOperationException,WorldCommandException {
        requireWorld();
        return activeWorld.previewOrderBookOpening(actingUser,eventId,revision);
    }
    @Override public synchronized WorldSnapshot openOrderBookEvent(String actingUser,int eventId,long expectedWorldRevision)
            throws EngineOperationException,WorldCommandException {
        requireWorld();
        if(expectedWorldRevision!=revision)
            throw new WorldCommandException(WorldCommandException.Code.STALE_WORLD,
                    "The world changed after this preview. Review the event again before opening.");
        return publish(activeWorld.openOrderBookEvent(actingUser,eventId));
    }

    @Override public synchronized WorldSnapshot loadWorldFromXml(Path path) throws EngineOperationException {
        MarketWorld candidate = loader.load(path);
        return publish(candidate);
    }
    @Override public synchronized OpeningPreview previewLmsrOpening(String actingUser, int eventId)
            throws EngineOperationException, WorldCommandException {
        requireWorld();
        return activeWorld.previewLmsrOpening(actingUser, eventId, revision);
    }
    @Override public synchronized WorldSnapshot openLmsrEvent(String actingUser, int eventId, long expectedWorldRevision)
            throws EngineOperationException, WorldCommandException {
        requireWorld();
        if (expectedWorldRevision != revision)
            throw new WorldCommandException(WorldCommandException.Code.STALE_WORLD,
                    "The world changed after this preview. Review the event again before opening.");
        return publish(activeWorld.openLmsrEvent(actingUser, eventId));
    }
    @Override public synchronized ClosePreview previewLmsrClose(String actingUser,int eventId,int winningOption)
            throws EngineOperationException,WorldCommandException {
        requireWorld();
        return activeWorld.previewLmsrClose(actingUser,eventId,winningOption,revision);
    }
    @Override public synchronized CloseResult closeLmsrEvent(String actingUser,int eventId,int winningOption,
            long expectedWorldRevision) throws EngineOperationException,WorldCommandException {
        requireWorld();
        if (revision!=expectedWorldRevision) throw new WorldCommandException(WorldCommandException.Code.STALE_WORLD,
                "The world changed after this preview. Review the payouts again.");
        MarketWorld candidate=activeWorld.closeLmsrEvent(actingUser,eventId,winningOption,revision);
        WorldSnapshot snapshot=candidate.snapshot();
        var event=snapshot.events().stream().filter(value -> value.id()==eventId).findFirst().orElseThrow();
        CloseResult result=new CloseResult(snapshot,event.lmsrTrading().orElseThrow().settlement().orElseThrow());
        long nextRevision=Math.incrementExact(revision);
        activeWorld=candidate;
        revision=nextRevision;
        return result;
    }
    private WorldSnapshot publish(MarketWorld candidate) {
        WorldSnapshot snapshot = candidate.snapshot();
        long nextRevision = Math.incrementExact(revision);
        // Publish only after all replacement state and display data have been prepared.
        activeWorld = candidate;
        revision = nextRevision;
        return snapshot;
    }
    @Override public synchronized PurchasePreview previewLmsrPurchase(String actingUser, int eventId,
            int optionNumber, int quantity) throws EngineOperationException, WorldCommandException {
        requireWorld();
        return activeWorld.previewLmsrPurchase(actingUser,eventId,optionNumber,quantity,revision);
    }
    @Override public synchronized PurchaseResult purchaseLmsrShares(String actingUser, int eventId,
            int optionNumber, int quantity, long expectedWorldRevision)
            throws EngineOperationException, WorldCommandException {
        requireWorld();
        if (revision!=expectedWorldRevision) throw new WorldCommandException(WorldCommandException.Code.STALE_WORLD,
                "The world changed after this preview. Review the purchase again.");
        MarketWorld candidate=activeWorld.purchaseLmsrShares(actingUser,eventId,optionNumber,quantity);
        WorldSnapshot snapshot=candidate.snapshot();
        var event=snapshot.events().stream().filter(value -> value.id()==eventId).findFirst().orElseThrow();
        var receipt=event.lmsrTrading().orElseThrow().newestFirstHistory().getFirst();
        boolean newlyBlocked=snapshot.users().stream().filter(user -> user.name().equals(actingUser))
                .findFirst().orElseThrow().blocked();
        PurchaseResult result=new PurchaseResult(snapshot,receipt,newlyBlocked);
        long nextRevision=Math.incrementExact(revision);
        activeWorld=candidate;
        revision=nextRevision;
        return result;
    }
    @Override public synchronized WorldSnapshot getWorldSnapshot() throws EngineOperationException {
        requireWorld();
        return activeWorld.snapshot();
    }
    private void requireWorld() throws EngineOperationException {
        if (activeWorld == null) throw new EngineOperationException(EngineErrorCode.NO_SYSTEM_LOADED,
                "No EX2 world is loaded.", "Load an EX2 XML file first.");
    }
}
