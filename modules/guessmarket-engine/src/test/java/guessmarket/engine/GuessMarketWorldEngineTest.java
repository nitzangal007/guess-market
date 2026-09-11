package guessmarket.engine;

import guessmarket.dto.world.OrderBookConfiguration;
import guessmarket.dto.world.WorldEventStatus;
import guessmarket.dto.world.WorldSnapshot;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class GuessMarketWorldEngineTest {
    private static Path fixture(String name) {
        return Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures", name);
    }

    @Test void smallWorldStartsUnfundedWithSeparateOwnershipAndParticipation() throws Exception {
        GuessMarketWorldEngine engine = new GuessMarketWorldEngineImpl();
        assertEquals(EngineErrorCode.NO_SYSTEM_LOADED,
                assertThrows(EngineOperationException.class, engine::getWorldSnapshot).getCode());
        WorldSnapshot world = engine.loadWorldFromXml(fixture("small.xml"));
        assertEquals(2, world.events().size());
        assertEquals(3, world.users().size());
        assertEquals("Avrum", world.users().getFirst().name());
        assertEquals(1000, world.users().getFirst().currentBalance());
        assertEquals(List.of(2), world.users().getFirst().ownedEventIds());
        assertEquals(10000, world.users().get(1).currentBalance());
        assertEquals(List.of(1), world.users().get(1).ownedEventIds());
        assertEquals(100, world.users().get(2).currentBalance());
        assertTrue(world.users().get(2).ownedEventIds().isEmpty());
        for (var user : world.users()) assertTrue(user.participatingEventIds().isEmpty());
        for (var event : world.events()) {
            assertEquals(WorldEventStatus.NOT_STARTED, event.status());
            assertEquals(0, event.contractBalance());
        }
        assertEquals(new OrderBookConfiguration(100, 1, true), world.events().get(1).pricing());
        assertEquals("Avrum", world.events().get(1).marketMakerName());
        assertThrows(UnsupportedOperationException.class, () -> world.users().clear());
        assertThrows(UnsupportedOperationException.class, () -> world.events().clear());
        assertThrows(UnsupportedOperationException.class, () -> world.users().getFirst().ownedEventIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> world.events().getFirst().optionLabels().clear());
    }

    @Test void failedWorldReplacementPreservesEverySnapshotFieldThenValidLoadReplacesAll() throws Exception {
        GuessMarketWorldEngine engine = new GuessMarketWorldEngineImpl();
        WorldSnapshot original = engine.loadWorldFromXml(fixture("small.xml"));
        for (String invalid : List.of("error-2.xml", "error-3.xml")) {
            assertEquals(EngineErrorCode.XML_DATA_INVALID,
                    assertThrows(EngineOperationException.class, () -> engine.loadWorldFromXml(fixture(invalid))).getCode());
            assertEquals(original, engine.getWorldSnapshot());
        }
        var replacement = engine.loadWorldFromXml(fixture("multiple.xml"));
        assertEquals(4, replacement.events().size());
        assertEquals(List.of(1, 4, 3), replacement.users().get(1).ownedEventIds());
        assertEquals(2, original.events().size(), "Old snapshots stay immutable after replacement");
        assertEquals(replacement, engine.getWorldSnapshot());
    }
}
