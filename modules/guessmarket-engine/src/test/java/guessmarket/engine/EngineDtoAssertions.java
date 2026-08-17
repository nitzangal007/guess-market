package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketOptionSnapshot;
import guessmarket.dto.TradeHistoryEntry;

final class EngineDtoAssertions {
    private EngineDtoAssertions() {
    }

    static void assertDetailsEqual(MarketEventDetails expected, MarketEventDetails actual) {
        assertEquals(expected.getEventId(), actual.getEventId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getCommissionPercentage(), actual.getCommissionPercentage());
        assertEquals(expected.getCommissionMode(), actual.getCommissionMode());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(Double.doubleToLongBits(expected.getEventAccountBalance()),
                Double.doubleToLongBits(actual.getEventAccountBalance()));
        assertEquals(Double.doubleToLongBits(expected.getTotalCommissionCollected()),
                Double.doubleToLongBits(actual.getTotalCommissionCollected()));
        assertEquals(expected.getWinningOptionNumber(), actual.getWinningOptionNumber());

        assertEquals(expected.getOptions().size(), actual.getOptions().size());
        for (int index = 0; index < expected.getOptions().size(); index++) {
            assertOptionEqual(expected.getOptions().get(index), actual.getOptions().get(index));
        }

        assertEquals(expected.getPurchaseHistory().size(), actual.getPurchaseHistory().size());
        for (int index = 0; index < expected.getPurchaseHistory().size(); index++) {
            assertHistoryEqual(
                    expected.getPurchaseHistory().get(index),
                    actual.getPurchaseHistory().get(index));
        }
    }

    private static void assertOptionEqual(MarketOptionSnapshot expected, MarketOptionSnapshot actual) {
        assertEquals(expected.getOptionNumber(), actual.getOptionNumber());
        assertEquals(expected.getLabel(), actual.getLabel());
        assertEquals(expected.getShareQuantity(), actual.getShareQuantity());
        assertEquals(Double.doubleToLongBits(expected.getCurrentPrice()),
                Double.doubleToLongBits(actual.getCurrentPrice()));
    }

    private static void assertHistoryEqual(TradeHistoryEntry expected, TradeHistoryEntry actual) {
        assertEquals(expected.getOptionNumber(), actual.getOptionNumber());
        assertEquals(expected.getOptionLabel(), actual.getOptionLabel());
        assertEquals(expected.getShareQuantity(), actual.getShareQuantity());
        assertEquals(Double.doubleToLongBits(expected.getBaseShareCost()),
                Double.doubleToLongBits(actual.getBaseShareCost()));
        assertEquals(Double.doubleToLongBits(expected.getPurchaseCommission()),
                Double.doubleToLongBits(actual.getPurchaseCommission()));
        assertEquals(Double.doubleToLongBits(expected.getTotalPaid()),
                Double.doubleToLongBits(actual.getTotalPaid()));
    }
}
