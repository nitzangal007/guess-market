package guessmarket.engine;

import guessmarket.dto.world.UserSnapshot;
import java.util.List;
import java.util.Objects;

/** Immutable EX2 account. Loading validation separately requires positive initial cash. */
public record MarketUser(String name, double currentBalance, List<Integer> ownedEventIds, boolean blocked) {
    public MarketUser(String name, double currentBalance, List<Integer> ownedEventIds) {
        this(name, currentBalance, ownedEventIds, false);
    }
    public MarketUser {
        Objects.requireNonNull(name);
        if (!Double.isFinite(currentBalance) || (currentBalance < 0 && !blocked))
            throw new IllegalArgumentException("Invalid current balance");
        ownedEventIds = List.copyOf(ownedEventIds);
    }
    /** A real receipt restores access only when the resulting cash is strictly positive. */
    MarketUser received(double receipt) {
        if(!Double.isFinite(receipt)||receipt<0)throw new IllegalArgumentException("Invalid receipt");
        double after=currentBalance+receipt;
        return new MarketUser(name,after,ownedEventIds,blocked&&!(receipt>0&&after>0));
    }
    public UserSnapshot snapshot() {
        return new UserSnapshot(name, currentBalance, ownedEventIds, List.of(), blocked, List.of());
    }
}
