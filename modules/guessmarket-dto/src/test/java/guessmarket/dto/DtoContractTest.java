package guessmarket.dto;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class DtoContractTest {

    @Test
    void summaryPreservesFullRangeEventIdAndEmptyText() {
        MarketEventSummary minimum = new MarketEventSummary(
                Integer.MIN_VALUE,
                "",
                "",
                0,
                CommissionMode.ON_PURCHASE,
                List.of("", ""),
                EventStatus.OPEN);
        MarketEventSummary maximum = new MarketEventSummary(
                Integer.MAX_VALUE,
                "Maximum",
                "",
                90,
                CommissionMode.ON_CLOSE,
                List.of("First", "Second"),
                EventStatus.CLOSED);

        assertAll(
                () -> assertEquals(Integer.MIN_VALUE, minimum.getEventId()),
                () -> assertEquals("", minimum.getName()),
                () -> assertEquals("", minimum.getDescription()),
                () -> assertEquals(0, minimum.getCommissionPercentage()),
                () -> assertEquals(CommissionMode.ON_PURCHASE, minimum.getCommissionMode()),
                () -> assertEquals(EventStatus.OPEN, minimum.getStatus()),
                () -> assertEquals(Integer.MAX_VALUE, maximum.getEventId()),
                () -> assertEquals(90, maximum.getCommissionPercentage()));
    }

    @Test
    void summaryDefensivelyCopiesTwoOrderedLabels() {
        List<String> labels = new ArrayList<>(List.of("same", "same"));
        MarketEventSummary summary = new MarketEventSummary(
                0,
                "",
                "",
                5,
                CommissionMode.ON_CLOSE,
                labels,
                EventStatus.OPEN);

        labels.set(0, "changed");

        assertEquals(List.of("same", "same"), summary.getOptionLabels());
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new MarketEventSummary(
                        1, null, "", 0, CommissionMode.ON_CLOSE, List.of("A", "B"), EventStatus.OPEN)),
                () -> assertThrows(NullPointerException.class, () -> new MarketEventSummary(
                        1, "", null, 0, CommissionMode.ON_CLOSE, List.of("A", "B"), EventStatus.OPEN)),
                () -> assertThrows(NullPointerException.class, () -> new MarketEventSummary(
                        1, "", "", 0, null, List.of("A", "B"), EventStatus.OPEN)),
                () -> assertThrows(NullPointerException.class, () -> new MarketEventSummary(
                        1, "", "", 0, CommissionMode.ON_CLOSE, null, EventStatus.OPEN)),
                () -> assertThrows(NullPointerException.class, () -> new MarketEventSummary(
                        1, "", "", 0, CommissionMode.ON_CLOSE, labelsWithNull(), EventStatus.OPEN)),
                () -> assertThrows(NullPointerException.class, () -> new MarketEventSummary(
                        1, "", "", 0, CommissionMode.ON_CLOSE, List.of("A", "B"), null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new MarketEventSummary(
                        1, "", "", -1, CommissionMode.ON_CLOSE, List.of("A", "B"), EventStatus.OPEN)),
                () -> assertThrows(IllegalArgumentException.class, () -> new MarketEventSummary(
                        1, "", "", 91, CommissionMode.ON_CLOSE, List.of("A", "B"), EventStatus.OPEN)),
                () -> assertThrows(IllegalArgumentException.class, () -> new MarketEventSummary(
                        1, "", "", 0, CommissionMode.ON_CLOSE, List.of("A"), EventStatus.OPEN)));
    }

    @Test
    void optionSnapshotRejectsInvalidOptionNumberQuantityAndPrice() {
        MarketOptionSnapshot first = new MarketOptionSnapshot(1, "", 0, 0.0);
        MarketOptionSnapshot second = new MarketOptionSnapshot(2, "Second", Integer.MAX_VALUE, 1.0);

        assertAll(
                () -> assertEquals(1, first.getOptionNumber()),
                () -> assertEquals("", first.getLabel()),
                () -> assertEquals(0, first.getShareQuantity()),
                () -> assertEquals(0.0, first.getCurrentPrice()),
                () -> assertEquals(2, second.getOptionNumber()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(0, "A", 0, 0.5)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(3, "A", 0, 0.5)),
                () -> assertThrows(NullPointerException.class,
                        () -> new MarketOptionSnapshot(1, null, 0, 0.5)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(1, "A", -1, 0.5)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(1, "A", 0, Double.NaN)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(1, "A", 0, Double.POSITIVE_INFINITY)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(1, "A", 0, -0.01)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new MarketOptionSnapshot(1, "A", 0, 1.01)));
    }

    @Test
    void historyEntryPreservesFinancialBreakdown() {
        TradeHistoryEntry entry = new TradeHistoryEntry(2, "", 3, 1.25, 0.25, 1.5);
        TradeHistoryEntry decimalEntry = new TradeHistoryEntry(1, "A", 1, 0.1, 0.2, 0.3);

        assertAll(
                () -> assertEquals(2, entry.getOptionNumber()),
                () -> assertEquals("", entry.getOptionLabel()),
                () -> assertEquals(3, entry.getShareQuantity()),
                () -> assertEquals(1.25, entry.getBaseShareCost()),
                () -> assertEquals(0.25, entry.getPurchaseCommission()),
                () -> assertEquals(1.5, entry.getTotalPaid()),
                () -> assertEquals(0.3, decimalEntry.getTotalPaid()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new TradeHistoryEntry(0, "A", 1, 1.0, 0.0, 1.0)),
                () -> assertThrows(NullPointerException.class,
                        () -> new TradeHistoryEntry(1, null, 1, 1.0, 0.0, 1.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new TradeHistoryEntry(1, "A", 0, 1.0, 0.0, 1.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new TradeHistoryEntry(1, "A", 1, Double.NaN, 0.0, 1.0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new TradeHistoryEntry(1, "A", 1, 1.0, -0.1, 0.9)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new TradeHistoryEntry(1, "A", 1, 1.0, 0.2, 1.0)));
    }

    @Test
    void detailsDefensivelyCopiesOptionsAndNewestFirstHistory() {
        List<MarketOptionSnapshot> options = new ArrayList<>(validOptions());
        TradeHistoryEntry newest = new TradeHistoryEntry(2, "B", 2, 1.0, 0.1, 1.1);
        TradeHistoryEntry older = new TradeHistoryEntry(1, "A", 1, 0.5, 0.0, 0.5);
        List<TradeHistoryEntry> history = new ArrayList<>(List.of(newest, older));

        MarketEventDetails details = new MarketEventDetails(
                -7,
                "",
                "",
                10,
                CommissionMode.ON_PURCHASE,
                EventStatus.OPEN,
                options,
                -4.75,
                0.1,
                history,
                OptionalInt.empty());

        options.clear();
        history.clear();

        assertAll(
                () -> assertEquals(-7, details.getEventId()),
                () -> assertEquals("", details.getName()),
                () -> assertEquals("", details.getDescription()),
                () -> assertEquals(10, details.getCommissionPercentage()),
                () -> assertEquals(CommissionMode.ON_PURCHASE, details.getCommissionMode()),
                () -> assertEquals(EventStatus.OPEN, details.getStatus()),
                () -> assertEquals(List.of(1, 2), details.getOptions().stream()
                        .map(MarketOptionSnapshot::getOptionNumber).toList()),
                () -> assertEquals(List.of(newest, older), details.getPurchaseHistory()),
                () -> assertEquals(-4.75, details.getEventAccountBalance()),
                () -> assertEquals(0.1, details.getTotalCommissionCollected()),
                () -> assertEquals(OptionalInt.empty(), details.getWinningOptionNumber()));
    }

    @Test
    void detailsRequiresWinnerOnlyForClosedEvent() {
        MarketEventDetails closed = new MarketEventDetails(
                1,
                "Name",
                "Description",
                0,
                CommissionMode.ON_CLOSE,
                EventStatus.CLOSED,
                validOptions(),
                0.0,
                0.0,
                List.of(),
                OptionalInt.of(2));

        assertEquals(OptionalInt.of(2), closed.getWinningOptionNumber());
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> details(
                        EventStatus.OPEN, OptionalInt.of(1))),
                () -> assertThrows(IllegalArgumentException.class, () -> details(
                        EventStatus.CLOSED, OptionalInt.empty())),
                () -> assertThrows(IllegalArgumentException.class, () -> details(
                        EventStatus.CLOSED, OptionalInt.of(3))),
                () -> assertThrows(NullPointerException.class, () -> new MarketEventDetails(
                        1, "", "", 0, CommissionMode.ON_CLOSE, EventStatus.OPEN,
                        validOptions(), 0.0, 0.0, List.of(), null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new MarketEventDetails(
                        1, "", "", 0, CommissionMode.ON_CLOSE, EventStatus.OPEN,
                        List.of(validOptions().get(1), validOptions().get(0)),
                        0.0, 0.0, List.of(), OptionalInt.empty())),
                () -> assertThrows(IllegalArgumentException.class, () -> new MarketEventDetails(
                        1, "", "", 0, CommissionMode.ON_CLOSE, EventStatus.OPEN,
                        validOptions(), Double.NaN, 0.0, List.of(), OptionalInt.empty())),
                () -> assertThrows(IllegalArgumentException.class, () -> new MarketEventDetails(
                        1, "", "", 0, CommissionMode.ON_CLOSE, EventStatus.OPEN,
                        validOptions(), 0.0, -0.01, List.of(), OptionalInt.empty())));
    }

    @Test
    void purchaseReceiptKeepsSameOperationDetails() {
        MarketEventDetails details = details(EventStatus.OPEN, OptionalInt.empty());
        PurchaseReceipt receipt = new PurchaseReceipt(1, 4, 2.0, 0.5, 2.5, details);
        PurchaseReceipt decimalReceipt = new PurchaseReceipt(2, 1, 0.1, 0.2, 0.3, details);

        assertAll(
                () -> assertEquals(1, receipt.getOptionNumber()),
                () -> assertEquals(4, receipt.getPurchasedShareQuantity()),
                () -> assertEquals(2.0, receipt.getBaseShareCost()),
                () -> assertEquals(0.5, receipt.getPurchaseCommission()),
                () -> assertEquals(2.5, receipt.getTotalPaid()),
                () -> assertSame(details, receipt.getResultingEventDetails()),
                () -> assertEquals(0.3, decimalReceipt.getTotalPaid()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PurchaseReceipt(3, 1, 1.0, 0.0, 1.0, details)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PurchaseReceipt(1, 0, 1.0, 0.0, 1.0, details)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new PurchaseReceipt(1, 1, 1.0, 0.2, 1.0, details)),
                () -> assertThrows(NullPointerException.class,
                        () -> new PurchaseReceipt(1, 1, 1.0, 0.0, 1.0, null)));
    }

    @Test
    void allReturnedCollectionsRejectMutation() {
        MarketEventSummary summary = new MarketEventSummary(
                1, "", "", 0, CommissionMode.ON_CLOSE, List.of("A", "B"), EventStatus.OPEN);
        MarketEventDetails details = details(EventStatus.OPEN, OptionalInt.empty());

        assertAll(
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> summary.getOptionLabels().add("C")),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> details.getOptions().clear()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> details.getPurchaseHistory().add(
                                new TradeHistoryEntry(1, "A", 1, 1.0, 0.0, 1.0))));
    }

    private static MarketEventDetails details(EventStatus status, OptionalInt winner) {
        return new MarketEventDetails(
                1,
                "Name",
                "Description",
                0,
                CommissionMode.ON_CLOSE,
                status,
                validOptions(),
                0.0,
                0.0,
                List.of(),
                winner);
    }

    private static List<MarketOptionSnapshot> validOptions() {
        return List.of(
                new MarketOptionSnapshot(1, "A", 0, 0.5),
                new MarketOptionSnapshot(2, "B", 0, 0.5));
    }

    private static List<String> labelsWithNull() {
        List<String> labels = new ArrayList<>();
        labels.add("A");
        labels.add(null);
        return labels;
    }
}
