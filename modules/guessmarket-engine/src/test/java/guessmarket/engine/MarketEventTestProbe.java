package guessmarket.engine;

import guessmarket.dto.MarketEventDetails;
import java.util.Objects;

/** Test-only bridge for observing package-private domain state from XML tests. */
public final class MarketEventTestProbe {
    private MarketEventTestProbe() {
    }

    public static Snapshot snapshot(MarketEvent event) {
        MarketEvent requiredEvent = Objects.requireNonNull(event, "event");
        return new Snapshot(requiredEvent.toDetails(), requiredEvent.getLiquidityParameter());
    }

    public record Snapshot(MarketEventDetails details, int liquidityParameter) {
        public Snapshot {
            Objects.requireNonNull(details, "details");
        }
    }
}
