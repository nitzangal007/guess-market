package guessmarket.dto;

import java.util.Objects;

public final class PurchaseReceipt {
    private final int optionNumber;
    private final int purchasedShareQuantity;
    private final double baseShareCost;
    private final double purchaseCommission;
    private final double totalPaid;
    private final MarketEventDetails resultingEventDetails;

    public PurchaseReceipt(
            int optionNumber,
            int purchasedShareQuantity,
            double baseShareCost,
            double purchaseCommission,
            double totalPaid,
            MarketEventDetails resultingEventDetails) {
        if (optionNumber != 1 && optionNumber != 2) {
            throw new IllegalArgumentException("Option number must be 1 or 2");
        }
        if (purchasedShareQuantity <= 0) {
            throw new IllegalArgumentException("Purchased share quantity must be positive");
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
        this.purchasedShareQuantity = purchasedShareQuantity;
        this.baseShareCost = baseShareCost;
        this.purchaseCommission = purchaseCommission;
        this.totalPaid = totalPaid;
        this.resultingEventDetails = Objects.requireNonNull(resultingEventDetails, "resultingEventDetails");
    }

    public int getOptionNumber() {
        return optionNumber;
    }

    public int getPurchasedShareQuantity() {
        return purchasedShareQuantity;
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

    public MarketEventDetails getResultingEventDetails() {
        return resultingEventDetails;
    }
}
