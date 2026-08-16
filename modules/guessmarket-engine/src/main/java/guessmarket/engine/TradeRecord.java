package guessmarket.engine;

import guessmarket.dto.TradeHistoryEntry;
import java.io.Serializable;
import java.util.Objects;

final class TradeRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int FINANCIAL_EQUALITY_ULPS = 8;

    private final int optionNumber;
    private final String optionLabel;
    private final int quantity;
    private final double baseShareCost;
    private final double purchaseCommission;
    private final double totalPaid;

    TradeRecord(
            int optionNumber,
            String optionLabel,
            int quantity,
            double baseShareCost,
            double purchaseCommission,
            double totalPaid) {
        if (optionNumber != 1 && optionNumber != 2) {
            throw new IllegalArgumentException("Option number must be 1 or 2");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (!Double.isFinite(baseShareCost) || baseShareCost <= 0.0) {
            throw new IllegalArgumentException("Base share cost must be finite and positive");
        }
        if (!Double.isFinite(purchaseCommission) || purchaseCommission < 0.0) {
            throw new IllegalArgumentException(
                    "Purchase commission must be finite and nonnegative");
        }
        if (!Double.isFinite(totalPaid) || totalPaid <= 0.0) {
            throw new IllegalArgumentException("Total paid must be finite and positive");
        }
        if (!totalsMatch(totalPaid, baseShareCost + purchaseCommission)) {
            throw new IllegalArgumentException(
                    "Total paid must equal base cost plus purchase commission");
        }

        this.optionNumber = optionNumber;
        this.optionLabel = Objects.requireNonNull(optionLabel, "optionLabel");
        this.quantity = quantity;
        this.baseShareCost = baseShareCost;
        this.purchaseCommission = purchaseCommission;
        this.totalPaid = totalPaid;
    }

    int getOptionNumber() {
        return optionNumber;
    }

    String getOptionLabel() {
        return optionLabel;
    }

    int getQuantity() {
        return quantity;
    }

    double getBaseShareCost() {
        return baseShareCost;
    }

    double getPurchaseCommission() {
        return purchaseCommission;
    }

    double getTotalPaid() {
        return totalPaid;
    }

    TradeHistoryEntry toHistoryEntry() {
        return new TradeHistoryEntry(
                optionNumber,
                optionLabel,
                quantity,
                baseShareCost,
                purchaseCommission,
                totalPaid);
    }

    private static boolean totalsMatch(double actualTotal, double expectedTotal) {
        if (!Double.isFinite(expectedTotal)) {
            return false;
        }

        double scale = Math.max(Math.abs(actualTotal), Math.abs(expectedTotal));
        double tolerance = FINANCIAL_EQUALITY_ULPS * Math.ulp(scale);
        return Math.abs(actualTotal - expectedTotal) <= tolerance;
    }
}
