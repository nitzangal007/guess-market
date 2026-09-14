package guessmarket.engine;

import java.math.BigDecimal;
import guessmarket.dto.world.LmsrConfiguration;

/** Trace-verified numerical reconciliation, private to Exercise 2 settlement. */
final class LmsrSettlementRounding {
    private LmsrSettlementRounding() { }

    record Proof(BigDecimal quoteAllowance, BigDecimal additionLoss) {
        BigDecimal maximumAdjustment() { return quoteAllowance.add(additionLoss); }
    }
    record Interval(double lower, double upper) { }

    static Proof verify(WorldEvent event) {
        int b = ((LmsrConfiguration) event.pricing()).b();
        double replay = LmsrCalculator.totalCost(0, 0, b);
        BigDecimal exactCredits = exact(replay);
        Interval before = potential(0, 0, b);
        BigDecimal quoteAllowance = exact(before.upper()).subtract(exactCredits).max(BigDecimal.ZERO);
        int one = 0, two = 0;
        long sequence = 0;
        for (var trade : event.trading().history()) {
            if (trade.sequence() != ++sequence || trade.quantity() <= 0
                    || (trade.optionNumber() != 1 && trade.optionNumber() != 2))
                throw new ArithmeticException("Invalid purchase history");
            double quote = LmsrCalculator.purchaseCost(trade.optionNumber() == 1 ? one : two,
                    trade.optionNumber() == 1 ? two : one, trade.quantity(), b);
            if (Double.compare(quote, trade.shareCost()) != 0)
                throw new ArithmeticException("Recorded credit differs from its purchase quote");
            if (trade.optionNumber() == 1) one = Math.addExact(one, trade.quantity());
            else two = Math.addExact(two, trade.quantity());
            Interval after = potential(one, two, b);
            // Mathematical cost is C(after)-C(before). Sum upper bounds exactly.
            BigDecimal undercharge = exact(after.upper()).subtract(exact(before.lower())).subtract(exact(quote));
            quoteAllowance = quoteAllowance.add(undercharge.max(BigDecimal.ZERO));
            exactCredits = exactCredits.add(exact(quote));
            replay += quote;
            before = after;
        }
        if (one != event.trading().quantityOne() || two != event.trading().quantityTwo()
                || Double.compare(replay, event.contractBalance()) != 0)
            throw new ArithmeticException("Contract or quantities differ from purchase history");
        return new Proof(quoteAllowance, exactCredits.subtract(exact(replay)).max(BigDecimal.ZERO));
    }

    // C(q1,q2) = max(q1,q2) + b*log1p(exp(-abs(q1-q2)/b)).
    // Basic operations widen by one neighbour. Math exp/log1p have a 1-ulp
    // accuracy contract; two neighbours cover it even across a binade boundary.
    static Interval potential(int one, int two, int b) {
        if (one < 0 || two < 0 || b <= 0) throw new IllegalArgumentException("Invalid LMSR state");
        double z = -Math.abs((long) one - two) / (double) b;
        double expLow = Math.max(0, downTwice(Math.exp(Math.nextDown(z))));
        double expHigh = Math.min(1, upTwice(Math.exp(Math.min(0, Math.nextUp(z)))));
        double logLow = Math.max(0, downTwice(Math.log1p(expLow)));
        double logHigh = upTwice(Math.log1p(expHigh));
        double maximum = Math.max(one, two);
        return new Interval(Math.max(maximum, Math.nextDown(maximum + Math.nextDown(b * logLow))),
                Math.nextUp(maximum + Math.nextUp(b * logHigh)));
    }
    private static double downTwice(double value) { return Math.nextDown(Math.nextDown(value)); }
    private static double upTwice(double value) { return Math.nextUp(Math.nextUp(value)); }
    private static BigDecimal exact(double value) { return new BigDecimal(value); }
}
