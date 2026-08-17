package guessmarket.dto;

import java.util.Objects;

public final class MarketOptionSnapshot {
    private final int optionNumber;
    private final String label;
    private final int shareQuantity;
    private final double currentPrice;

    public MarketOptionSnapshot(
            int optionNumber,
            String label,
            int shareQuantity,
            double currentPrice) {
        if (optionNumber != 1 && optionNumber != 2) {
            throw new IllegalArgumentException("Option number must be 1 or 2");
        }
        if (shareQuantity < 0) {
            throw new IllegalArgumentException("Share quantity cannot be negative");
        }
        if (!Double.isFinite(currentPrice) || currentPrice < 0.0 || currentPrice > 1.0) {
            throw new IllegalArgumentException("Current price must be finite and between 0 and 1");
        }

        this.optionNumber = optionNumber;
        this.label = Objects.requireNonNull(label, "label");
        this.shareQuantity = shareQuantity;
        this.currentPrice = currentPrice;
    }

    public int getOptionNumber() {
        return optionNumber;
    }

    public String getLabel() {
        return label;
    }

    public int getShareQuantity() {
        return shareQuantity;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
}
