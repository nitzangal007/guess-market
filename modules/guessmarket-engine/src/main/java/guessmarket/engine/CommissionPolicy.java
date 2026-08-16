package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import java.io.Serializable;
import java.util.Objects;

final class CommissionPolicy implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CommissionMode mode;
    private final int percentage;

    CommissionPolicy(CommissionMode mode, int percentage) {
        if (percentage < 0 || percentage > 90) {
            throw new IllegalArgumentException(
                    "Commission percentage must be between 0 and 90");
        }

        this.mode = Objects.requireNonNull(mode, "mode");
        this.percentage = percentage;
    }

    CommissionMode getMode() {
        return mode;
    }

    int getPercentage() {
        return percentage;
    }

    double purchaseCommission(double baseCost) {
        if (mode != CommissionMode.ON_PURCHASE || percentage == 0) {
            return 0.0;
        }
        return baseCost * (percentage / 100.0);
    }

    double closingCommission(double grossPayout) {
        if (mode != CommissionMode.ON_CLOSE || percentage == 0) {
            return 0.0;
        }
        return grossPayout * (percentage / 100.0);
    }
}
