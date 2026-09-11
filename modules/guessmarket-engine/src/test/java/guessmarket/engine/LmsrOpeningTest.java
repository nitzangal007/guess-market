package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LmsrOpeningTest {
    private Path small() {
        return Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml");
    }
    @Test void realPreviewAndOpeningConserveCashWithoutCommissionOrParticipation() throws Exception {
        var engine = new GuessMarketWorldEngineImpl();
        var before = engine.loadWorldFromXml(small());
        var preview = engine.previewLmsrOpening("Tikva", 1);
        double funding = LmsrCalculator.totalCost(0, 0, 100);
        assertEquals(69.31471805599453, funding, 1e-13);
        assertEquals(funding, preview.requiredFunding());
        assertEquals(10000.0, preview.currentBalance());
        assertEquals(10000.0 - funding, preview.balanceAfterOpening());
        assertEquals("Mujtaba is Dead", preview.eventName());
        assertEquals(before, engine.getWorldSnapshot());
        var after = engine.openLmsrEvent("Tikva", 1, preview.worldRevision());
        assertEquals(WorldEventStatus.ACTIVE, after.events().getFirst().status());
        assertEquals(funding, after.events().getFirst().contractBalance());
        var tikva = after.users().get(1);
        assertEquals(10000.0 - funding, tikva.currentBalance());
        assertEquals(10000.0, tikva.currentBalance() + after.events().getFirst().contractBalance(), 1e-10);
        assertTrue(tikva.participatingEventIds().isEmpty());
        assertEquals(List.of(1), tikva.ownedEventIds());
        assertEquals(before.users().getFirst(), after.users().getFirst());
        assertEquals(before.events().get(1), after.events().get(1));
        assertEquals(10000.0, before.users().get(1).currentBalance());
        assertEquals(WorldEventStatus.NOT_STARTED, before.events().getFirst().status());
        assertThrows(UnsupportedOperationException.class, () -> after.users().clear());
        var repeat = assertThrows(WorldCommandException.class,
                () -> engine.openLmsrEvent("Tikva", 1, preview.worldRevision()));
        assertEquals(WorldCommandException.Code.STALE_WORLD, repeat.getCode());
        assertEquals(after, engine.getWorldSnapshot());
        assertEquals(WorldCommandException.Code.WRONG_STATUS,
                assertThrows(WorldCommandException.class, () -> engine.previewLmsrOpening("Tikva", 1)).getCode());
    }
    @Test void identityMethodAndReloadRejectionsLeaveWholeWorldUntouched() throws Exception {
        var engine = new GuessMarketWorldEngineImpl();
        var before = engine.loadWorldFromXml(small());
        var preview = engine.previewLmsrOpening("Tikva", 1);
        for (String nonOwner : List.of("Avrum", "Menash", "tikva", "")) {
            assertThrows(WorldCommandException.class, () -> engine.openLmsrEvent(nonOwner, 1, preview.worldRevision()));
            assertEquals(before, engine.getWorldSnapshot());
        }
        assertEquals(WorldCommandException.Code.WRONG_METHOD, assertThrows(WorldCommandException.class,
                () -> engine.openLmsrEvent("Avrum", 2, preview.worldRevision())).getCode());
        assertEquals(WorldCommandException.Code.EVENT_NOT_FOUND, assertThrows(WorldCommandException.class,
                () -> engine.openLmsrEvent("Tikva", 999, preview.worldRevision())).getCode());
        assertEquals(before, engine.getWorldSnapshot());
        engine.loadWorldFromXml(small());
        assertEquals(WorldCommandException.Code.STALE_WORLD, assertThrows(WorldCommandException.class,
                () -> engine.openLmsrEvent("Tikva", 1, preview.worldRevision())).getCode());
        assertEquals(before, engine.getWorldSnapshot());
    }
    private MarketWorld world(double cash, WorldEventStatus status) {
        var event = new WorldEvent(1, "Boundary event", "Details", List.of("Yes", "No"), 90,
                CommissionMode.ON_PURCHASE, "Owner", new LmsrConfiguration(100), status, 0);
        return new MarketWorld(Map.of(1, event), Map.of("Owner", new MarketUser("Owner", cash, List.of(1))));
    }
    @Test void exactFundingSucceedsAndOneUlpLessFailsAtomically() throws Exception {
        double funding = LmsrCalculator.totalCost(0, 0, 100);
        var equal = world(funding, WorldEventStatus.NOT_STARTED);
        var before = equal.snapshot();
        var opened = equal.openLmsrEvent("Owner", 1).snapshot();
        assertEquals(0.0, opened.users().getFirst().currentBalance());
        assertEquals(funding, opened.events().getFirst().contractBalance());
        assertEquals(before, equal.snapshot());
        var insufficient = world(Math.nextDown(funding), WorldEventStatus.NOT_STARTED);
        var poorBefore = insufficient.snapshot();
        assertEquals(WorldCommandException.Code.INSUFFICIENT_FUNDS, assertThrows(WorldCommandException.class,
                () -> insufficient.openLmsrEvent("Owner", 1)).getCode());
        assertEquals(poorBefore, insufficient.snapshot());
        var closed = world(1000, WorldEventStatus.CLOSED);
        assertEquals(WorldCommandException.Code.WRONG_STATUS, assertThrows(WorldCommandException.class,
                () -> closed.openLmsrEvent("Owner", 1)).getCode());
    }
    @Test void noWorldCannotPreviewOrOpen() {
        var engine = new GuessMarketWorldEngineImpl();
        assertThrows(EngineOperationException.class, () -> engine.previewLmsrOpening("Tikva", 1));
        assertThrows(EngineOperationException.class, () -> engine.openLmsrEvent("Tikva", 1, 0));
    }
}

