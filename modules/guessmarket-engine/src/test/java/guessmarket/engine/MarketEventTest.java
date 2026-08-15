package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.TradeHistoryEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketEventTest {
    private static final double TOLERANCE = 1.0E-12;

    @Test
    void constructorCreatesOpenZeroBasedEventAndPreservesAllowedXmlValues() {
        MarketEvent event = new MarketEvent(
                Integer.MIN_VALUE,
                "",
                "",
                CommissionMode.ON_PURCHASE,
                0,
                100,
                "same",
                "same");

        MarketEventDetails details = event.toDetails();
        MarketEventSummary summary = event.toSummary();

        assertEquals(Integer.MIN_VALUE, details.getEventId());
        assertEquals("", details.getName());
        assertEquals("", details.getDescription());
        assertEquals(CommissionMode.ON_PURCHASE, details.getCommissionMode());
        assertEquals(0, details.getCommissionPercentage());
        assertEquals(EventStatus.OPEN, details.getStatus());
        assertEquals(0.0, details.getEventAccountBalance());
        assertEquals(0.0, details.getTotalCommissionCollected());
        assertEquals(0, details.getOptions().get(0).getShareQuantity());
        assertEquals(0, details.getOptions().get(1).getShareQuantity());
        assertEquals(0.5, details.getOptions().get(0).getCurrentPrice(), TOLERANCE);
        assertEquals(0.5, details.getOptions().get(1).getCurrentPrice(), TOLERANCE);
        assertEquals(List.of(), details.getPurchaseHistory());
        assertTrue(details.getWinningOptionNumber().isEmpty());
        assertEquals(List.of("same", "same"), summary.getOptionLabels());
        assertEquals(EventStatus.OPEN, summary.getStatus());
        assertEquals(100.0 * Math.log(2.0), event.maximumSubsidy(), TOLERANCE);
    }

    @Test
    void constructorRejectsInvalidDomainDefinitions() {
        assertThrows(
                NullPointerException.class,
                () -> new MarketEvent(1, null, "description", CommissionMode.ON_CLOSE,
                        0, 1, "one", "two"));
        assertThrows(
                NullPointerException.class,
                () -> new MarketEvent(1, "name", "description", null,
                        0, 1, "one", "two"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketEvent(1, "name", "description", CommissionMode.ON_CLOSE,
                        -1, 1, "one", "two"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketEvent(1, "name", "description", CommissionMode.ON_CLOSE,
                        91, 1, "one", "two"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MarketEvent(1, "name", "description", CommissionMode.ON_CLOSE,
                        0, 0, "one", "two"));
        assertThrows(
                NullPointerException.class,
                () -> new MarketEvent(1, "name", "description", CommissionMode.ON_CLOSE,
                        0, 1, null, "two"));
    }

    @Test
    void onPurchaseCommissionAtNinetyPercentCreditsCostAndCommission() throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 90, 100);
        double expectedBaseCost = LmsrCalculator.purchaseCost(0, 0, 100, 100);

        TradeRecord record = event.purchase(1, 100);
        MarketEventDetails details = event.toDetails();

        assertEquals(expectedBaseCost, record.getBaseShareCost(), TOLERANCE);
        assertEquals(expectedBaseCost * 0.9, record.getPurchaseCommission(), TOLERANCE);
        assertEquals(expectedBaseCost * 1.9, record.getTotalPaid(), TOLERANCE);
        assertEquals(record.getTotalPaid(), details.getEventAccountBalance(), TOLERANCE);
        assertEquals(record.getPurchaseCommission(), details.getTotalCommissionCollected(), TOLERANCE);
        assertEquals(100, details.getOptions().get(0).getShareQuantity());
    }

    @Test
    void onCloseCommissionAtNinetyPercentIsRetainedExactlyOnce() throws Exception {
        MarketEvent event = event(CommissionMode.ON_CLOSE, 90, 100);
        TradeRecord record = event.purchase(1, 100);

        assertEquals(0.0, record.getPurchaseCommission());
        assertEquals(record.getBaseShareCost(), event.toDetails().getEventAccountBalance());

        event.close(1);

        MarketEventDetails details = event.toDetails();
        assertEquals(record.getBaseShareCost() - 10.0,
                details.getEventAccountBalance(), TOLERANCE);
        assertEquals(90.0, details.getTotalCommissionCollected(), TOLERANCE);
        assertEquals(EventStatus.CLOSED, details.getStatus());
        assertEquals(1, details.getWinningOptionNumber().orElseThrow());
    }

    @Test
    void zeroPercentCommissionWorksInBothModes() throws Exception {
        for (CommissionMode mode : CommissionMode.values()) {
            MarketEvent event = event(mode, 0, 100);
            TradeRecord record = event.purchase(1, 10);

            assertEquals(0.0, record.getPurchaseCommission());
            assertEquals(record.getBaseShareCost(), record.getTotalPaid());

            event.close(1);

            assertEquals(0.0, event.toDetails().getTotalCommissionCollected());
            assertEquals(record.getBaseShareCost() - 10.0,
                    event.toDetails().getEventAccountBalance(), TOLERANCE);
        }
    }

    @Test
    void multiplePurchasesAccumulateQuantitiesAccountingAndNewestFirstHistory()
            throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 5, 100);
        TradeRecord first = event.purchase(1, 10);
        TradeRecord second = event.purchase(2, 50);
        TradeRecord third = event.purchase(1, 5);

        MarketEventDetails details = event.toDetails();
        List<TradeHistoryEntry> history = details.getPurchaseHistory();

        assertEquals(15, details.getOptions().get(0).getShareQuantity());
        assertEquals(50, details.getOptions().get(1).getShareQuantity());
        assertEquals(first.getTotalPaid() + second.getTotalPaid() + third.getTotalPaid(),
                details.getEventAccountBalance(), TOLERANCE);
        assertEquals(first.getPurchaseCommission()
                        + second.getPurchaseCommission()
                        + third.getPurchaseCommission(),
                details.getTotalCommissionCollected(), TOLERANCE);
        assertEquals(3, history.size());
        assertHistoryEntry(history.get(0), third);
        assertHistoryEntry(history.get(1), second);
        assertHistoryEntry(history.get(2), first);
    }

    @Test
    void quantityOverflowIsRejectedWithoutChangingAnyDomainState() throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 5, Integer.MAX_VALUE);
        event.purchase(1, Integer.MAX_VALUE);

        EngineOperationException exception = assertRejectedUnchanged(
                event,
                EngineErrorCode.INVALID_QUANTITY,
                () -> event.purchase(1, 1));

        assertEquals(1, exception.getOptionNumber().orElseThrow());
        assertEquals(1, exception.getQuantity().orElseThrow());
    }

    @Test
    void nonpositiveQuantityAndInvalidPurchaseOptionAreRejectedAtomically() throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 5, 100);

        assertRejectedUnchanged(
                event,
                EngineErrorCode.INVALID_QUANTITY,
                () -> event.purchase(1, 0));
        assertRejectedUnchanged(
                event,
                EngineErrorCode.INVALID_QUANTITY,
                () -> event.purchase(1, -1));
        assertRejectedUnchanged(
                event,
                EngineErrorCode.INVALID_OPTION,
                () -> event.purchase(3, 1));
    }

    @Test
    void closedEventRejectsPurchaseAndRepeatedCloseAtomically() throws Exception {
        MarketEvent event = event(CommissionMode.ON_CLOSE, 5, 100);
        event.purchase(1, 10);
        event.close(1);

        EngineOperationException purchaseFailure = assertRejectedUnchanged(
                event,
                EngineErrorCode.EVENT_NOT_OPEN,
                () -> event.purchase(2, 1));
        EngineOperationException closeFailure = assertRejectedUnchanged(
                event,
                EngineErrorCode.EVENT_NOT_OPEN,
                () -> event.close(2));

        assertEquals(EventStatus.CLOSED, purchaseFailure.getEventStatus().orElseThrow());
        assertEquals(EventStatus.CLOSED, closeFailure.getEventStatus().orElseThrow());
    }

    @Test
    void invalidWinningOptionIsRejectedAtomically() throws Exception {
        MarketEvent event = event(CommissionMode.ON_CLOSE, 5, 100);
        event.purchase(1, 10);

        assertRejectedUnchanged(
                event,
                EngineErrorCode.INVALID_OPTION,
                () -> event.close(0));
    }

    @Test
    void closingOnLosingOptionProducesProfit() throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 5, 100);
        TradeRecord record = event.purchase(1, 100);

        event.close(2);

        assertTrue(event.toDetails().getEventAccountBalance() > 0.0);
        assertEquals(record.getTotalPaid(),
                event.toDetails().getEventAccountBalance(), TOLERANCE);
    }

    @Test
    void closingOnPurchasedOptionProducesSubsidy() throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 5, 100);
        TradeRecord record = event.purchase(1, 100);

        event.close(1);

        assertTrue(event.toDetails().getEventAccountBalance() < 0.0);
        assertEquals(record.getTotalPaid() - 100.0,
                event.toDetails().getEventAccountBalance(), TOLERANCE);
    }

    @Test
    void closingWithoutPurchasesProducesExactBreakEven() throws Exception {
        MarketEvent event = event(CommissionMode.ON_CLOSE, 90, 100);

        event.close(2);

        MarketEventDetails details = event.toDetails();
        assertEquals(0.0, details.getEventAccountBalance(), 0.0);
        assertEquals(0.0, details.getTotalCommissionCollected(), 0.0);
        assertEquals(EventStatus.CLOSED, details.getStatus());
    }

    @Test
    void tinyDisfavoredPurchaseRemainsPositiveAndCommits() throws Exception {
        MarketEvent event = event(CommissionMode.ON_CLOSE, 0, 1);
        event.purchase(1, 100);

        TradeRecord tiny = event.purchase(2, 1);

        assertRelativeEquals(6.392138950083687E-44, tiny.getBaseShareCost(), 1.0E-14);
        assertTrue(tiny.getBaseShareCost() > 0.0);
        assertEquals(1, event.toDetails().getOptions().get(1).getShareQuantity());
    }

    @Test
    void unrepresentablePurchaseIsMappedToCheckedFailureAndRejectedAtomically()
            throws Exception {
        MarketEvent event = event(CommissionMode.ON_CLOSE, 0, 1);
        event.purchase(1, 1000);

        EngineOperationException failure = assertRejectedUnchanged(
                event,
                EngineErrorCode.FINANCIAL_CALCULATION_FAILED,
                () -> event.purchase(2, 1));

        assertInstanceOf(ArithmeticException.class, failure.getCause());
    }

    @Test
    void domainSerializationAndVisibilityStayInsideTheEngineBoundary() throws Exception {
        assertTrue(Serializable.class.isAssignableFrom(MarketEvent.class));
        assertTrue(Serializable.class.isAssignableFrom(MarketOption.class));
        assertTrue(Serializable.class.isAssignableFrom(EventAccount.class));
        assertTrue(Serializable.class.isAssignableFrom(CommissionPolicy.class));
        assertTrue(Serializable.class.isAssignableFrom(TradeRecord.class));

        assertTrue(Modifier.isPublic(MarketEvent.class.getModifiers()));
        assertFalse(Modifier.isPublic(MarketOption.class.getModifiers()));
        assertFalse(Modifier.isPublic(EventAccount.class.getModifiers()));
        assertFalse(Modifier.isPublic(CommissionPolicy.class.getModifiers()));
        assertFalse(Modifier.isPublic(TradeRecord.class.getModifiers()));
        assertFalse(Modifier.isPublic(
                MarketEvent.class.getDeclaredMethod("purchase", int.class, int.class)
                        .getModifiers()));
        assertFalse(Modifier.isPublic(
                MarketEvent.class.getDeclaredMethod("close", int.class).getModifiers()));
    }

    @Test
    void packagePrivateObservationsPreserveOwnedIdentityForLaterValidation() throws Exception {
        MarketEvent event = event(CommissionMode.ON_PURCHASE, 5, 100);
        MarketEvent otherEvent = event(CommissionMode.ON_CLOSE, 0, 1);

        assertEquals(17, event.getEventId());
        assertEquals("Final", event.getName());
        assertEquals("Two-option market", event.getDescription());
        assertEquals(100, event.getLiquidityParameter());
        assertEquals(EventStatus.OPEN, event.getStatus());
        assertTrue(event.getWinningOptionNumber().isEmpty());
        assertEquals(CommissionMode.ON_PURCHASE, event.getCommissionPolicy().getMode());
        assertEquals(5, event.getCommissionPolicy().getPercentage());
        assertSame(event.getCommissionPolicy(), event.getCommissionPolicy());
        assertSame(event.getAccount(), event.getAccount());
        assertSame(event.getOptions(), event.getOptions());
        assertSame(event.getPurchaseHistory(), event.getPurchaseHistory());
        assertNotSame(event.getAccount(), otherEvent.getAccount());
        assertNotSame(event.getOptions(), otherEvent.getOptions());
        assertNotSame(event.getOptions().get(0), event.getOptions().get(1));

        TradeRecord record = event.purchase(1, 10);

        assertSame(record, event.getPurchaseHistory().get(0));
        assertEquals(10, event.getOptions().get(0).getQuantity());
    }

    private static MarketEvent event(CommissionMode mode, int percentage, int b) {
        return new MarketEvent(
                17,
                "Final",
                "Two-option market",
                mode,
                percentage,
                b,
                "First",
                "Second");
    }

    private static void assertHistoryEntry(TradeHistoryEntry entry, TradeRecord record) {
        assertEquals(record.getOptionNumber(), entry.getOptionNumber());
        assertEquals(record.getOptionLabel(), entry.getOptionLabel());
        assertEquals(record.getQuantity(), entry.getShareQuantity());
        assertEquals(record.getBaseShareCost(), entry.getBaseShareCost());
        assertEquals(record.getPurchaseCommission(), entry.getPurchaseCommission());
        assertEquals(record.getTotalPaid(), entry.getTotalPaid());
    }

    private static EngineOperationException assertRejectedUnchanged(
            MarketEvent event,
            EngineErrorCode expectedCode,
            ThrowingTransition transition) throws IOException {
        byte[] before = serialize(event);

        EngineOperationException exception = assertThrows(
                EngineOperationException.class,
                transition::run);

        assertEquals(expectedCode, exception.getCode());
        assertArrayEquals(before, serialize(event));
        return exception;
    }

    private static byte[] serialize(MarketEvent event) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(event);
        }
        return bytes.toByteArray();
    }

    private static void assertRelativeEquals(
            double expected,
            double actual,
            double relativeTolerance) {
        assertEquals(expected, actual, Math.abs(expected) * relativeTolerance);
    }

    @FunctionalInterface
    private interface ThrowingTransition {
        void run() throws EngineOperationException;
    }
}
