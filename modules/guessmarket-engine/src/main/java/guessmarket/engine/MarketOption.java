package guessmarket.engine;

import java.io.Serializable;
import java.util.Objects;

final class MarketOption implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int optionNumber;
    private final String label;
    private int quantity;

    MarketOption(int optionNumber, String label) {
        if (optionNumber != 1 && optionNumber != 2) {
            throw new IllegalArgumentException("Option number must be 1 or 2");
        }

        this.optionNumber = optionNumber;
        this.label = Objects.requireNonNull(label, "label");
        quantity = 0;
    }

    int getOptionNumber() {
        return optionNumber;
    }

    String getLabel() {
        return label;
    }

    int getQuantity() {
        return quantity;
    }

    void commitPurchase(int newQuantity) {
        quantity = newQuantity;
    }
}
