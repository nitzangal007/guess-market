package guessmarket.dto.world;

public record OrderBookConfiguration(int initial, int d, boolean allowMint) implements PricingConfiguration {
    public OrderBookConfiguration {
        if (initial < 0 || d <= 0) throw new IllegalArgumentException("initial must be nonnegative and d positive");
    }
}
