package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LmsrCalculatorTest {
    private static final double NORMAL_TOLERANCE = 1.0E-12;

    @Test
    void initialPricesAreOneHalf() {
        double firstPrice = LmsrCalculator.priceForOption(0, 0, 100);
        double secondPrice = LmsrCalculator.priceForOption(0, 0, 100);

        assertEquals(0.5, firstPrice, NORMAL_TOLERANCE);
        assertEquals(0.5, secondPrice, NORMAL_TOLERANCE);
    }

    @Test
    void complementaryPricesSumToOneForUnequalQuantities() {
        double firstPrice = LmsrCalculator.priceForOption(175, 40, 90);
        double secondPrice = LmsrCalculator.priceForOption(40, 175, 90);

        assertEquals(1.0, firstPrice + secondPrice, NORMAL_TOLERANCE);
    }

    @Test
    void buyingAnOptionRaisesItsPrice() {
        double initialPrice = LmsrCalculator.priceForOption(0, 0, 100);
        double priceAfterTen = LmsrCalculator.priceForOption(10, 0, 100);
        double priceAfterFifty = LmsrCalculator.priceForOption(50, 0, 100);

        assertTrue(initialPrice < priceAfterTen);
        assertTrue(priceAfterTen < priceAfterFifty);
    }

    @Test
    void largerPurchasesCostMore() {
        double tenShareCost = LmsrCalculator.purchaseCost(0, 0, 10, 100);
        double fiftyShareCost = LmsrCalculator.purchaseCost(0, 0, 50, 100);

        assertTrue(tenShareCost < fiftyShareCost);
    }

    @Test
    void largerLiquidityParameterProducesSmallerPriceMovement() {
        double thinMarketPrice = LmsrCalculator.priceForOption(100, 0, 100);
        double deepMarketPrice = LmsrCalculator.priceForOption(100, 0, 200);

        assertTrue(deepMarketPrice > 0.5);
        assertTrue(deepMarketPrice < thinMarketPrice);
    }

    @Test
    void maximumSubsidyEqualsInitialTotalCost() {
        double expected = 100.0 * Math.log(2.0);

        assertEquals(expected, LmsrCalculator.maximumSubsidy(100), NORMAL_TOLERANCE);
        assertEquals(expected, LmsrCalculator.totalCost(0, 0, 100), NORMAL_TOLERANCE);
    }

    @Test
    void approvedWorkedExampleMatchesAtFullPrecision() {
        assertEquals(
                69.3147180559945,
                LmsrCalculator.totalCost(0, 0, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                131.326168751822,
                LmsrCalculator.totalCost(100, 0, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                62.0114506958278,
                LmsrCalculator.purchaseCost(0, 0, 100, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                0.731058578630005,
                LmsrCalculator.priceForOption(100, 0, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                0.268941421369995,
                LmsrCalculator.priceForOption(0, 100, 100),
                NORMAL_TOLERANCE);
    }

    @Test
    void suppliedSimulatorOracleCasesMatch() {
        assertEquals(
                5.124947951362557,
                LmsrCalculator.purchaseCost(0, 0, 10, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                26.86185923263818,
                LmsrCalculator.purchaseCost(0, 10, 50, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                0.401312339887548,
                LmsrCalculator.priceForOption(10, 50, 100),
                NORMAL_TOLERANCE);
        assertEquals(
                0.598687660112452,
                LmsrCalculator.priceForOption(50, 10, 100),
                NORMAL_TOLERANCE);
    }

    @Test
    void veryLargeQuantitiesRemainFinite() {
        double totalCost = LmsrCalculator.totalCost(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE - 1,
                1);
        double price = LmsrCalculator.priceForOption(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE - 1,
                1);
        double purchaseCost = LmsrCalculator.purchaseCost(
                Integer.MAX_VALUE - 1,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE);

        assertTrue(Double.isFinite(totalCost));
        assertTrue(Double.isFinite(price));
        assertTrue(Double.isFinite(purchaseCost));
        assertTrue(purchaseCost > 0.0);
    }

    @Test
    void directDeltaRetainsRepresentableTinyPurchaseCost() {
        double cost = LmsrCalculator.purchaseCost(0, 100, 1, 1);

        assertRelativeEquals(6.392138950083687E-44, cost, 1.0E-14);
        assertTrue(cost > 0.0);
    }

    @Test
    void underflowedDisplayPriceDoesNotLoseRepresentableAggregateCost() {
        double price = LmsrCalculator.priceForOption(0, 1000, 1);
        double cost = LmsrCalculator.purchaseCost(0, 1000, 1000, 1);

        assertEquals(0.0, price);
        assertEquals(Math.log(2.0), cost, NORMAL_TOLERANCE);
        assertTrue(cost > 0.0);
    }

    @Test
    void unrepresentablePositivePurchaseCostIsRejected() {
        assertThrows(
                ArithmeticException.class,
                () -> LmsrCalculator.purchaseCost(0, 1000, 1, 1));
    }

    @Test
    void intBoundaryArithmeticDoesNotOverflowIntermediates() {
        double boundaryCost = LmsrCalculator.purchaseCost(
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE);

        assertTrue(Double.isFinite(boundaryCost));
        assertTrue(boundaryCost > 0.0);
        assertTrue(boundaryCost <= Integer.MAX_VALUE);
        assertEquals(
                1.0,
                LmsrCalculator.priceForOption(Integer.MAX_VALUE, 0, 1),
                0.0);
        assertEquals(
                0.0,
                LmsrCalculator.priceForOption(0, Integer.MAX_VALUE, 1),
                0.0);
    }

    @Test
    void largeOpposingTermsUseCombinedLongNumerator() {
        double expected = 3.0 * Math.log1p(Math.exp(-2.0 / 3.0));

        assertEquals(1.2431102605562161, expected, 0.0);
        assertEquals(
                expected,
                LmsrCalculator.purchaseCost(
                        0,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE - 2,
                        3),
                1.0E-15);
    }

    @Test
    void invalidInputsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LmsrCalculator.totalCost(-1, 0, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> LmsrCalculator.totalCost(0, 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> LmsrCalculator.priceForOption(0, -1, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> LmsrCalculator.purchaseCost(0, 0, 0, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> LmsrCalculator.purchaseCost(-1, 0, 1, 100));
        assertThrows(
                IllegalArgumentException.class,
                () -> LmsrCalculator.maximumSubsidy(-1));
    }

    private static void assertRelativeEquals(
            double expected,
            double actual,
            double relativeTolerance) {
        double tolerance = Math.abs(expected) * relativeTolerance;
        assertEquals(expected, actual, tolerance);
    }
}
