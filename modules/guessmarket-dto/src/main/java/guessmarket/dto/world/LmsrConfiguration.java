package guessmarket.dto.world;

public record LmsrConfiguration(int b) implements PricingConfiguration {
    public LmsrConfiguration {
        if (b <= 0) throw new IllegalArgumentException("b must be positive");
    }
}
