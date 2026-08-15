package guessmarket.engine;

final class LmsrCalculator {
    private static final double SMALL_RESULT_THRESHOLD = -37.0;
    private static final double LARGE_EXPM1_THRESHOLD = 50.0;
    private static final double LOG_ONE_MINUS_EXP_THRESHOLD = Math.log(2.0);

    private LmsrCalculator() {
    }

    static double totalCost(int quantityOne, int quantityTwo, int b) {
        requireNonnegative(quantityOne, "quantityOne");
        requireNonnegative(quantityTwo, "quantityTwo");
        requirePositive(b, "b");

        double scaledOne = quantityOne / (double) b;
        double scaledTwo = quantityTwo / (double) b;
        double maximum = Math.max(scaledOne, scaledTwo);
        double weightOne = Math.exp(scaledOne - maximum);
        double weightTwo = Math.exp(scaledTwo - maximum);
        double cost = b * (maximum + Math.log(weightOne + weightTwo));

        return requireFinite(cost, "total cost");
    }

    static double priceForOption(int selectedQuantity, int otherQuantity, int b) {
        requireNonnegative(selectedQuantity, "selectedQuantity");
        requireNonnegative(otherQuantity, "otherQuantity");
        requirePositive(b, "b");

        long difference = (long) selectedQuantity - otherQuantity;
        double scaledDifference = difference / (double) b;
        double price;
        if (scaledDifference >= 0.0) {
            double otherWeight = Math.exp(-scaledDifference);
            price = 1.0 / (1.0 + otherWeight);
        } else {
            double selectedWeight = Math.exp(scaledDifference);
            price = selectedWeight / (1.0 + selectedWeight);
        }

        return requireFinite(price, "option price");
    }

    static double purchaseCost(
            int selectedQuantity,
            int otherQuantity,
            int purchaseQuantity,
            int b) {
        requireNonnegative(selectedQuantity, "selectedQuantity");
        requireNonnegative(otherQuantity, "otherQuantity");
        requirePositive(purchaseQuantity, "purchaseQuantity");
        requirePositive(b, "b");

        long difference = (long) selectedQuantity - otherQuantity;
        double z = difference / (double) b;
        double h = purchaseQuantity / (double) b;
        double t;
        if (difference < 0) {
            long combinedDifference = difference + purchaseQuantity;
            double logisticCorrection = softplus(z);
            double expm1Correction = logOneMinusExpOfNegative(h);
            t = combinedDifference / (double) b
                    - logisticCorrection
                    + expm1Correction;
        } else {
            double logP = -softplus(-z);
            double logExpm1 = logExpm1(h);
            t = logP + logExpm1;
        }

        double cost;
        if (t < SMALL_RESULT_THRESHOLD) {
            cost = Math.exp(Math.log(b) + t);
        } else {
            cost = b * softplus(t);
        }

        requireFinite(cost, "purchase cost");
        if (cost <= 0.0) {
            throw new ArithmeticException("purchase cost must be positive");
        }
        return cost;
    }

    static double maximumSubsidy(int b) {
        requirePositive(b, "b");
        return requireFinite(b * Math.log(2.0), "maximum subsidy");
    }

    private static double softplus(double value) {
        return Math.max(value, 0.0)
                + Math.log1p(Math.exp(-Math.abs(value)));
    }

    private static double logExpm1(double positiveValue) {
        if (positiveValue < LARGE_EXPM1_THRESHOLD) {
            return Math.log(Math.expm1(positiveValue));
        }
        return positiveValue + logOneMinusExpOfNegative(positiveValue);
    }

    private static double logOneMinusExpOfNegative(double positiveValue) {
        if (positiveValue <= LOG_ONE_MINUS_EXP_THRESHOLD) {
            return Math.log(-Math.expm1(-positiveValue));
        }
        return Math.log1p(-Math.exp(-positiveValue));
    }

    private static void requireNonnegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be nonnegative");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static double requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " must be finite");
        }
        return value;
    }
}
