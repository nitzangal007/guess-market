package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.TradeHistoryEntry;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuessMarketEngineXmlLoadTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void successfulReloadReplacesRatherThanMergesTheOrderedLiveSystem() throws Exception {
        GuessMarketEngine engine = new GuessMarketEngineImpl();

        int firstCount = engine.loadEventsFromXml(fixturePath("supplied/multiple.xml"));
        engine.purchaseShares(1, 1, 4);
        int replacementCount = engine.loadEventsFromXml(fixturePath("supplied/single.xml"));

        assertAll(
                () -> assertEquals(3, firstCount),
                () -> assertEquals(1, replacementCount),
                () -> assertIterableEquals(
                        List.of(3),
                        engine.listEvents().stream()
                                .map(MarketEventSummary::getEventId)
                                .toList()),
                () -> assertEquals(0, engine.getEventDetails(3)
                        .getOptions().get(0).getShareQuantity()),
                () -> assertTrue(engine.getEventDetails(3).getPurchaseHistory().isEmpty()),
                () -> assertThrows(
                        EngineOperationException.class,
                        () -> engine.getEventDetails(1)));
    }

    @Test
    void publicLoadPreservesFullRangeIdsAndActualIdLookup() throws Exception {
        GuessMarketEngine engine = new GuessMarketEngineImpl();

        int count = engine.loadEventsFromXml(
                fixturePath("custom/valid full-range IDs and empty text.xml"));

        assertEquals(4, count);
        assertIterableEquals(
                List.of(0, -17, Integer.MIN_VALUE, Integer.MAX_VALUE),
                engine.listEvents().stream().map(MarketEventSummary::getEventId).toList());
        assertEquals(Integer.MIN_VALUE,
                engine.getEventDetails(Integer.MIN_VALUE).getEventId());
        assertEquals(Integer.MAX_VALUE,
                engine.getEventDetails(Integer.MAX_VALUE).getEventId());
    }

    @Test
    void failedFirstLoadLeavesTheEngineWithNoLoadedSystemAndKeepsXmlContext() {
        GuessMarketEngine engine = new GuessMarketEngineImpl();

        EngineOperationException loadFailure = assertThrows(
                EngineOperationException.class,
                () -> engine.loadEventsFromXml(fixturePath("supplied/error-2.xml")));
        EngineOperationException listFailure = assertThrows(
                EngineOperationException.class,
                engine::listEvents);

        assertAll(
                () -> assertEquals(EngineErrorCode.XML_DATA_INVALID, loadFailure.getCode()),
                () -> assertEquals(3, loadFailure.getXmlEventNumber().orElseThrow()),
                () -> assertEquals(1, loadFailure.getEventId().orElseThrow()),
                () -> assertEquals("id", loadFailure.getFieldName().orElseThrow()),
                () -> assertEquals(EngineErrorCode.NO_SYSTEM_LOADED, listFailure.getCode()));
    }

    @Test
    void everyRejectedReloadPreservesTheCompletePriorPublicState() throws Exception {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        engine.loadEventsFromXml(fixturePath("supplied/single.xml"));
        engine.purchaseShares(3, 1, 6);
        MarketEventDetails before = engine.getEventDetails(3);

        Path wrongSuffix = temporaryDirectory.resolve("wrong suffix.txt");
        Files.writeString(wrongSuffix, "not XML");
        Path missing = temporaryDirectory.resolve("missing.xml");
        Path malformed = temporaryDirectory.resolve("malformed.xml");
        Files.writeString(malformed, "<Guess-Market><GM-events>");

        List<FailedLoad> failures = List.of(
                new FailedLoad(wrongSuffix, EngineErrorCode.INVALID_XML_PATH),
                new FailedLoad(missing, EngineErrorCode.XML_FILE_NOT_FOUND),
                new FailedLoad(malformed, EngineErrorCode.XML_STRUCTURE_INVALID),
                new FailedLoad(
                        fixturePath("custom/invalid missing description.xml"),
                        EngineErrorCode.XML_STRUCTURE_INVALID),
                new FailedLoad(
                        fixturePath("supplied/error-2.xml"),
                        EngineErrorCode.XML_DATA_INVALID),
                new FailedLoad(
                        fixturePath("supplied/error-3.xml"),
                        EngineErrorCode.XML_DATA_INVALID));

        for (FailedLoad failedLoad : failures) {
            EngineOperationException exception = assertThrows(
                    EngineOperationException.class,
                    () -> engine.loadEventsFromXml(failedLoad.path()));

            assertEquals(failedLoad.code(), exception.getCode());
            assertIterableEquals(
                    List.of(3),
                    engine.listEvents().stream().map(MarketEventSummary::getEventId).toList());
            assertDetailsEqual(before, engine.getEventDetails(3));
        }
    }

    private static Path fixturePath(String relativeFixturePath) {
        try {
            return Path.of(GuessMarketEngineXmlLoadTest.class.getClassLoader().getResource(
                    "guessmarket/engine/xml/fixtures/" + relativeFixturePath).toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(
                    "Invalid test fixture URI: " + relativeFixturePath,
                    exception);
        }
    }

    private static void assertDetailsEqual(MarketEventDetails expected, MarketEventDetails actual) {
        assertAll(
                () -> assertEquals(expected.getEventId(), actual.getEventId()),
                () -> assertEquals(expected.getName(), actual.getName()),
                () -> assertEquals(expected.getDescription(), actual.getDescription()),
                () -> assertEquals(expected.getCommissionMode(), actual.getCommissionMode()),
                () -> assertEquals(expected.getCommissionPercentage(), actual.getCommissionPercentage()),
                () -> assertEquals(expected.getStatus(), actual.getStatus()),
                () -> assertEquals(expected.getEventAccountBalance(), actual.getEventAccountBalance()),
                () -> assertEquals(
                        expected.getTotalCommissionCollected(),
                        actual.getTotalCommissionCollected()),
                () -> assertEquals(expected.getWinningOptionNumber(), actual.getWinningOptionNumber()),
                () -> assertIterableEquals(
                        expected.getOptions().stream()
                                .map(option -> List.of(
                                        option.getOptionNumber(),
                                        option.getLabel(),
                                        option.getShareQuantity(),
                                        option.getCurrentPrice()))
                                .toList(),
                        actual.getOptions().stream()
                                .map(option -> List.of(
                                        option.getOptionNumber(),
                                        option.getLabel(),
                                        option.getShareQuantity(),
                                        option.getCurrentPrice()))
                                .toList()),
                () -> assertIterableEquals(
                        expected.getPurchaseHistory().stream()
                                .map(GuessMarketEngineXmlLoadTest::historyValues)
                                .toList(),
                        actual.getPurchaseHistory().stream()
                                .map(GuessMarketEngineXmlLoadTest::historyValues)
                                .toList()));
    }

    private static List<Object> historyValues(TradeHistoryEntry entry) {
        return List.of(
                entry.getOptionNumber(),
                entry.getOptionLabel(),
                entry.getShareQuantity(),
                entry.getBaseShareCost(),
                entry.getPurchaseCommission(),
                entry.getTotalPaid());
    }

    private record FailedLoad(Path path, EngineErrorCode code) {
    }
}
