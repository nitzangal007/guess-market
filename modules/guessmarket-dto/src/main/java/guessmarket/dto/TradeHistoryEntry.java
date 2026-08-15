package guessmarket.dto;

import java.util.Objects;

public final class TradeHistoryEntry {
    private final int optionNumber;
    private final String optionLabel;
    private final int shareQuantity;
    private final double baseShareCost;
    private final double purchaseCommission;
    private final double totalPaid;

    public TradeHistoryEntry(
            int optionNumber,
            String optionLabel,
            int shareQuantity,
            double baseShareCost,
            double purchaseCommission,
            double totalPaid) {
        if (optionNumber != 1 && optionNumber != 2) {
            throw new IllegalArgumentException("Option number must be 1 or 2");
        }
        if (shareQuantity <= 0) {
            throw new IllegalArgumentException("Share quantity must be positive");
        }
        if (!Double.isFinite(baseShareCost) || baseShareCost <= 0.0) {
            throw new IllegalArgumentException("Base share cost must be finite and positive");
        }
        if (!Double.isFinite(purchaseCommission) || purchaseCommission < 0.0) {
            throw new IllegalArgumentException("Purchase commission must be finite and non-negative");
        }
        if (!Double.isFinite(totalPaid) || totalPaid <= 0.0) {
            throw new IllegalArgumentException("Total paid must be finite and positive");
        }
        if (Double.compare(totalPaid, baseShareCost + purchaseCommission) != 0) {
            throw new IllegalArgumentException("Total paid must equal base cost plus purchase commission");
        }

        this.optionNumber = optionNumber;
        this.optionLabel = Objects.requireNonNull(optionLabel, "optionLabel");
        this.shareQuantity = shareQuantity;
        this.baseShareCost = baseShareCost;
        this.purchaseCommission = purchaseCommission;
        this.totalPaid = totalPaid;
    }

    public int getOptionNumber() {
        return optionNumber;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public int getShareQuantity() {
        return shareQuantity;
    }

    public double getBaseShareCost() {
        return baseShareCost;
    }

    public double getPurchaseCommission() {
        return purchaseCommission;
    }

    public double getTotalPaid() {
        return totalPaid;
    }
}
