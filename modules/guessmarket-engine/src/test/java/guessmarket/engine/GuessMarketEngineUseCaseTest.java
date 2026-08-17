package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.PurchaseReceipt;
import guessmarket.dto.TradeHistoryEntry;
import guessmarket.engine.xml.XmlMarketLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GuessMarketEngineUseCaseTest {

    @Test
    void implementationHasOnePublicNoArgumentConstructorAndNoOtherPublicConstructor() {
        Constructor<?>[] constructors = GuessMarketEngineImpl.class.getDeclaredConstructors();
        long publicConstructors = java.util.Arrays.stream(constructors)
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .count();

        assertAll(
                () -> assertTrue(Modifier.isPublic(GuessMarketEngineImpl.class.getModifiers())),
                () -> assertTrue(GuessMarketEngine.class.isAssignableFrom(GuessMarketEngineImpl.class)),
                () -> assertEquals(1, publicConstructors),
                () -> assertTrue(Modifier.isPublic(
                        GuessMarketEngineImpl.class.getDeclaredConstructor().getModifiers())),
                () -> assertTrue(java.util.Arrays.stream(constructors)
                        .filter(constructor -> constructor.getParameterCount() > 0)
                        .noneMatch(constructor -> Modifier.isPublic(constructor.getModifiers()))));
    }

    @Test
    void engineInterfaceDeclaresSevenCheckedOperations() throws NoSuchMethodException {
        assertTrue(GuessMarketEngine.class.isInterface());

        Method load = GuessMarketEngine.class.getDeclaredMethod("loadEventsFromXml", Path.class);
        Method list = GuessMarketEngine.class.getDeclaredMethod("listEvents");
        Method details = GuessMarketEngine.class.getDeclaredMethod("getEventDetails", int.class);
        Method purchase = GuessMarketEngine.class.getDeclaredMethod(
                "purchaseShares", int.class, int.class, int.class);
        Method close = GuessMarketEngine.class.getDeclaredMethod("closeEvent", int.class, int.class);
        Method save = GuessMarketEngine.class.getDeclaredMethod("saveState", Path.class);
        Method restore = GuessMarketEngine.class.getDeclaredMethod("restoreState", Path.class);
        ParameterizedType listReturnType = (ParameterizedType) list.getGenericReturnType();

        assertAll(
                () -> assertEquals(7, GuessMarketEngine.class.getDeclaredMethods().length),
                () -> assertMethod(load, int.class),
                () -> assertMethod(list, List.class),
                () -> assertEquals(List.class, listReturnType.getRawType()),
                () -> assertArrayEquals(
                        new Object[] {MarketEventSummary.class},
                        listReturnType.getActualTypeArguments()),
                () -> assertMethod(details, MarketEventDetails.class),
                () -> assertMethod(purchase, PurchaseReceipt.class),
                () -> assertMethod(close, MarketEventDetails.class),
                () -> assertMethod(save, void.class),
                () -> assertMethod(restore, int.class));
    }

    @Test
    void errorCodeContainsExactlyApprovedValues() {
        assertArrayEquals(
                new String[] {
                    "INVALID_XML_PATH",
                    "XML_FILE_NOT_FOUND",
                    "XML_FILE_ACCESS_FAILED",
                    "XML_STRUCTURE_INVALID",
                    "XML_DATA_INVALID",
                    "ENGINE_CONFIGURATION_ERROR",
                    "NO_SYSTEM_LOADED",
                    "EVENT_NOT_FOUND",
                    "EVENT_NOT_OPEN",
                    "INVALID_OPTION",
                    "INVALID_QUANTITY",
                    "FINANCIAL_CALCULATION_FAILED",
                    "INVALID_STATE_PATH",
                    "STATE_FILE_NOT_FOUND",
                    "STATE_FILE_ACCESS_FAILED",
                    "SAVED_STATE_INVALID"
                },
                java.util.Arrays.stream(EngineErrorCode.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void simpleExceptionPreservesRequiredFields() {
        EngineOperationException failure = new EngineOperationException(
                EngineErrorCode.NO_SYSTEM_LOADED,
                "No system is loaded",
                "Load XML or restore saved state first");

        assertAll(
                () -> assertTrue(Exception.class.isAssignableFrom(EngineOperationException.class)),
                () -> assertFalse(RuntimeException.class.isAssignableFrom(EngineOperationException.class)),
                () -> assertEquals(EngineErrorCode.NO_SYSTEM_LOADED, failure.getCode()),
                () -> assertEquals("No system is loaded", failure.getDetail()),
                () -> assertEquals("Load XML or restore saved state first", failure.getRecoveryHint()),
                () -> assertEquals("No system is loaded", failure.getMessage()),
                () -> assertNull(failure.getCause()),
                () -> assertEquals(Optional.empty(), failure.getPath()),
                () -> assertEquals(OptionalInt.empty(), failure.getXmlEventNumber()),
                () -> assertEquals(OptionalInt.empty(), failure.getEventId()),
                () -> assertEquals(Optional.empty(), failure.getFieldName()),
                () -> assertEquals(OptionalInt.empty(), failure.getOptionNumber()),
                () -> assertEquals(OptionalInt.empty(), failure.getQuantity()),
                () -> assertEquals(Optional.empty(), failure.getEventStatus()),
                () -> assertEquals(OptionalInt.empty(), failure.getLineNumber()),
                () -> assertEquals(OptionalInt.empty(), failure.getColumnNumber()));
    }

    @Test
    void completeExceptionPreservesOptionalContextAndCause() {
        Path path = Path.of("fixtures", "invalid.xml");
        Exception cause = new IllegalStateException("developer diagnosis only");
        EngineOperationException failure = new EngineOperationException(
                EngineErrorCode.XML_DATA_INVALID,
                "Commission is outside the accepted range",
                "Correct the event commission and load the file again",
                path,
                4,
                Integer.MIN_VALUE,
                "commission",
                2,
                7,
                EventStatus.CLOSED,
                10,
                20,
                cause);

        assertAll(
                () -> assertEquals(EngineErrorCode.XML_DATA_INVALID, failure.getCode()),
                () -> assertEquals(Optional.of(path), failure.getPath()),
                () -> assertEquals(OptionalInt.of(4), failure.getXmlEventNumber()),
                () -> assertEquals(OptionalInt.of(Integer.MIN_VALUE), failure.getEventId()),
                () -> assertEquals(Optional.of("commission"), failure.getFieldName()),
                () -> assertEquals(OptionalInt.of(2), failure.getOptionNumber()),
                () -> assertEquals(OptionalInt.of(7), failure.getQuantity()),
                () -> assertEquals(Optional.of(EventStatus.CLOSED), failure.getEventStatus()),
                () -> assertEquals(OptionalInt.of(10), failure.getLineNumber()),
                () -> assertEquals(OptionalInt.of(20), failure.getColumnNumber()),
                () -> assertSame(cause, failure.getCause()));
    }

    @Test
    void stateDependentOperationsRejectAnEmptyEngineWithTheCheckedLoadedStateRule() {
        GuessMarketEngine engine = new GuessMarketEngineImpl();
        Path statePath = Path.of("not-used-while-empty");

        List<ThrowingOperation> operations = List.of(
                engine::listEvents,
                () -> engine.getEventDetails(17),
                () -> engine.purchaseShares(17, 1, 1),
                () -> engine.closeEvent(17, 1),
                () -> engine.saveState(statePath));

        for (ThrowingOperation operation : operations) {
            EngineOperationException failure = assertThrows(
                    EngineOperationException.class,
                    operation::run);
            assertAll(
                    () -> assertEquals(EngineErrorCode.NO_SYSTEM_LOADED, failure.getCode()),
                    () -> assertEquals("No system is loaded", failure.getDetail()),
                    () -> assertEquals(
                            "Load XML or restore saved state first",
                            failure.getRecoveryHint()),
                    () -> assertTrue(failure.getEventId().isEmpty()));
        }
    }

    @Test
    void listsEveryEventInLoadedOrderAndUsesFullRangeActualIds() throws Exception {
        GuessMarketEngine engine = engineWithEvents(
                event(Integer.MAX_VALUE, "maximum"),
                event(-17, "negative"),
                event(Integer.MIN_VALUE, "minimum"),
                event(0, "zero"));

        List<MarketEventSummary> first = engine.listEvents();
        List<MarketEventSummary> second = engine.listEvents();

        assertIterableEquals(
                List.of(Integer.MAX_VALUE, -17, Integer.MIN_VALUE, 0),
                first.stream().map(MarketEventSummary::getEventId).toList());
        assertIterableEquals(
                List.of("maximum", "negative", "minimum", "zero"),
                first.stream().map(MarketEventSummary::getName).toList());
        assertEquals(Integer.MIN_VALUE, engine.getEventDetails(Integer.MIN_VALUE).getEventId());
        assertEquals(Integer.MAX_VALUE, engine.getEventDetails(Integer.MAX_VALUE).getEventId());
        assertNotSame(first, second);
        assertNotSame(first.get(0), second.get(0));
        assertThrows(UnsupportedOperationException.class,
                () -> first.add(first.get(0)));
        assertThrows(UnsupportedOperationException.class,
                () -> first.get(0).getOptionLabels().add("third"));
    }

    @Test
    void missingActualIdReportsContextWithoutChangingTheLoadedSystem() throws Exception {
        GuessMarketEngine engine = engineWithEvents(event(Integer.MIN_VALUE, "minimum"));
        List<MarketEventSummary> before = engine.listEvents();

        EngineOperationException failure = assertThrows(
                EngineOperationException.class,
                () -> engine.getEventDetails(Integer.MAX_VALUE));

        assertAll(
                () -> assertEquals(EngineErrorCode.EVENT_NOT_FOUND, failure.getCode()),
                () -> assertEquals(Integer.MAX_VALUE, failure.getEventId().orElseThrow()),
                () -> assertEquals(
                        "Event ID " + Integer.MAX_VALUE + " does not exist",
                        failure.getDetail()),
                () -> assertEquals(
                        "Choose an event from the displayed list",
                        failure.getRecoveryHint()),
                () -> assertIterableEquals(
                        before.stream().map(MarketEventSummary::getEventId).toList(),
                        engine.listEvents().stream().map(MarketEventSummary::getEventId).toList()));
    }

    @Test
    void successfulPurchasesReturnSameOperationDetailsAndNewestFirstHistory() throws Exception {
        GuessMarketEngine engine = engineWithEvents(event(42, "purchase"));

        PurchaseReceipt first = engine.purchaseShares(42, 1, 3);
        PurchaseReceipt second = engine.purchaseShares(42, 2, 2);
        MarketEventDetails current = engine.getEventDetails(42);
        List<TradeHistoryEntry> history = current.getPurchaseHistory();

        assertAll(
                () -> assertEquals(1, first.getOptionNumber()),
                () -> assertEquals(3, first.getPurchasedShareQuantity()),
                () -> assertEquals(3, first.getResultingEventDetails()
                        .getOptions().get(0).getShareQuantity()),
                () -> assertEquals(0, first.getResultingEventDetails()
                        .getOptions().get(1).getShareQuantity()),
                () -> assertEquals(2, second.getOptionNumber()),
                () -> assertEquals(2, second.getPurchasedShareQuantity()),
                () -> assertEquals(3, second.getResultingEventDetails()
                        .getOptions().get(0).getShareQuantity()),
                () -> assertEquals(2, second.getResultingEventDetails()
                        .getOptions().get(1).getShareQuantity()),
                () -> assertEquals(2, history.size()),
                () -> assertEquals(2, history.get(0).getOptionNumber()),
                () -> assertEquals(1, history.get(1).getOptionNumber()),
                () -> assertNotSame(second.getResultingEventDetails(), current),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> history.add(history.get(0))));
    }

    @Test
    void failedPurchasesPreservePriorPublicStateAndExposeCheckedContext() throws Exception {
        GuessMarketEngine engine = engineWithEvents(event(7, "failure"));
        engine.purchaseShares(7, 1, 2);
        MarketEventDetails before = engine.getEventDetails(7);

        EngineOperationException invalidOption = assertThrows(
                EngineOperationException.class,
                () -> engine.purchaseShares(7, 3, 1));
        assertDetailsEqual(before, engine.getEventDetails(7));

        EngineOperationException invalidQuantity = assertThrows(
                EngineOperationException.class,
                () -> engine.purchaseShares(7, 2, 0));
        assertDetailsEqual(before, engine.getEventDetails(7));

        EngineOperationException missingEvent = assertThrows(
                EngineOperationException.class,
                () -> engine.purchaseShares(8, 1, 1));
        assertDetailsEqual(before, engine.getEventDetails(7));

        assertAll(
                () -> assertEquals(EngineErrorCode.INVALID_OPTION, invalidOption.getCode()),
                () -> assertEquals(7, invalidOption.getEventId().orElseThrow()),
                () -> assertEquals(3, invalidOption.getOptionNumber().orElseThrow()),
                () -> assertEquals(1, invalidOption.getQuantity().orElseThrow()),
                () -> assertEquals(EventStatus.OPEN, invalidOption.getEventStatus().orElseThrow()),
                () -> assertEquals(EngineErrorCode.INVALID_QUANTITY, invalidQuantity.getCode()),
                () -> assertEquals(0, invalidQuantity.getQuantity().orElseThrow()),
                () -> assertEquals(EngineErrorCode.EVENT_NOT_FOUND, missingEvent.getCode()),
                () -> assertEquals(8, missingEvent.getEventId().orElseThrow()));
    }

    @Test
    void closeReturnsFinalDetailsAndEveryFailedClosePreservesThem() throws Exception {
        GuessMarketEngine engine = engineWithEvents(
                event(10, "open"),
                event(-4, "to close"));
        engine.purchaseShares(-4, 1, 5);

        MarketEventDetails closed = engine.closeEvent(-4, 1);
        assertEquals(EventStatus.CLOSED, closed.getStatus());
        assertEquals(1, closed.getWinningOptionNumber().orElseThrow());
        assertIterableEquals(
                List.of(EventStatus.OPEN, EventStatus.CLOSED),
                engine.listEvents().stream().map(MarketEventSummary::getStatus).toList());

        EngineOperationException repeated = assertThrows(
                EngineOperationException.class,
                () -> engine.closeEvent(-4, 2));
        assertDetailsEqual(closed, engine.getEventDetails(-4));

        MarketEventDetails openBefore = engine.getEventDetails(10);
        EngineOperationException invalidOption = assertThrows(
                EngineOperationException.class,
                () -> engine.closeEvent(10, 0));
        assertDetailsEqual(openBefore, engine.getEventDetails(10));

        assertAll(
                () -> assertEquals(EngineErrorCode.EVENT_NOT_OPEN, repeated.getCode()),
                () -> assertEquals(-4, repeated.getEventId().orElseThrow()),
                () -> assertEquals(EventStatus.CLOSED, repeated.getEventStatus().orElseThrow()),
                () -> assertEquals(EngineErrorCode.INVALID_OPTION, invalidOption.getCode()),
                () -> assertEquals(0, invalidOption.getOptionNumber().orElseThrow()));
    }

    private static void assertMethod(Method method, Class<?> returnType) {
        assertEquals(returnType, method.getReturnType());
        assertArrayEquals(
                new Class<?>[] {EngineOperationException.class},
                method.getExceptionTypes(),
                method.getName() + " must declare the checked Engine failure contract");
    }

    private static GuessMarketEngine engineWithEvents(MarketEvent... events) {
        LinkedHashMap<Integer, MarketEvent> initialEvents = new LinkedHashMap<>();
        for (MarketEvent event : events) {
            initialEvents.put(event.getEventId(), event);
        }
        return new GuessMarketEngineImpl(
                new XmlMarketLoader(),
                new SavedStateStore(),
                initialEvents);
    }

    private static MarketEvent event(int eventId, String name) {
        return new MarketEvent(
                eventId,
                name,
                "description",
                CommissionMode.ON_PURCHASE,
                5,
                100,
                "first",
                "second");
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
                                .map(GuessMarketEngineUseCaseTest::historyValues)
                                .toList(),
                        actual.getPurchaseHistory().stream()
                                .map(GuessMarketEngineUseCaseTest::historyValues)
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

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws EngineOperationException;
    }
}
