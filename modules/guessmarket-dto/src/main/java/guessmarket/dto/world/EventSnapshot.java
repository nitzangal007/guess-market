package guessmarket.dto.world;

import guessmarket.dto.CommissionMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EventSnapshot(int id, String name, String description, List<String> optionLabels,
                            int commissionPercentage, CommissionMode commissionMode,
                            WorldEventStatus status, String marketMakerName,
                            double contractBalance, PricingConfiguration pricing,
                            Optional<LmsrTradingSnapshot> lmsrTrading, Optional<OrderBookSnapshot> orderBook) {
    public EventSnapshot(int id, String name, String description, List<String> optionLabels,
                         int commissionPercentage, CommissionMode commissionMode, WorldEventStatus status,
                         String marketMakerName, double contractBalance, PricingConfiguration pricing,
                         Optional<LmsrTradingSnapshot> lmsrTrading) {
        this(id,name,description,optionLabels,commissionPercentage,commissionMode,status,
                marketMakerName,contractBalance,pricing,lmsrTrading,Optional.empty());
    }
    public EventSnapshot(int id, String name, String description, List<String> optionLabels,
                         int commissionPercentage, CommissionMode commissionMode, WorldEventStatus status,
                         String marketMakerName, double contractBalance, PricingConfiguration pricing) {
        this(id, name, description, optionLabels, commissionPercentage, commissionMode, status,
                marketMakerName, contractBalance, pricing, Optional.empty());
    }
    public EventSnapshot {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        optionLabels = List.copyOf(optionLabels);
        if (optionLabels.size() != 2) throw new IllegalArgumentException("Exactly two options required");
        if (commissionPercentage < 0 || commissionPercentage > 90)
            throw new IllegalArgumentException("Commission must be between 0 and 90");
        Objects.requireNonNull(commissionMode);
        Objects.requireNonNull(status);
        Objects.requireNonNull(marketMakerName);
        Objects.requireNonNull(pricing);
        Objects.requireNonNull(lmsrTrading);
        Objects.requireNonNull(orderBook);
        if (!Double.isFinite(contractBalance)) throw new IllegalArgumentException("Contract balance must be finite");
    }
}
