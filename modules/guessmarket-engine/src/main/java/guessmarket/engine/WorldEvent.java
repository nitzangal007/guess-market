package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.EventSnapshot;
import guessmarket.dto.world.PricingConfiguration;
import guessmarket.dto.world.WorldEventStatus;
import guessmarket.dto.world.LmsrConfiguration;
import java.util.Optional;
import java.util.List;
import java.util.Objects;

/** Immutable EX2 event state. Loading creates an unfunded, not-started event. */
public record WorldEvent(int id, String name, String description, List<String> options,
                         int commission, CommissionMode commissionMode,
                         String marketMakerName, PricingConfiguration pricing,
                         WorldEventStatus status, double contractBalance, LmsrTradingState trading) {
    public WorldEvent(int id, String name, String description, List<String> options,
                      int commission, CommissionMode commissionMode, String marketMakerName,
                      PricingConfiguration pricing, WorldEventStatus status, double contractBalance) {
        this(id,name,description,options,commission,commissionMode,marketMakerName,pricing,status,contractBalance,
                pricing instanceof LmsrConfiguration ? LmsrTradingState.empty() : null);
    }
    public WorldEvent(int id, String name, String description, List<String> options,
                      int commission, CommissionMode commissionMode,
                      String marketMakerName, PricingConfiguration pricing) {
        this(id, name, description, options, commission, commissionMode, marketMakerName,
                pricing, WorldEventStatus.NOT_STARTED, 0.0);
    }
    public WorldEvent {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        options = List.copyOf(options);
        Objects.requireNonNull(commissionMode);
        Objects.requireNonNull(marketMakerName);
        Objects.requireNonNull(pricing);
        Objects.requireNonNull(status);
        if ((pricing instanceof LmsrConfiguration) != (trading != null))
            throw new IllegalArgumentException("Trading state must match the pricing method");
        if (options.size() != 2 || commission < 0 || commission > 90
                || !Double.isFinite(contractBalance) || contractBalance < 0)
            throw new IllegalArgumentException("Invalid event definition or balance");
    }
    public EventSnapshot snapshot() {
        return new EventSnapshot(id, name, description, options, commission, commissionMode,
                status, marketMakerName, contractBalance, pricing,
                pricing instanceof LmsrConfiguration lmsr
                        ? Optional.of(trading.snapshot(id,options,lmsr.b())) : Optional.empty());
    }
}
