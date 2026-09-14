package guessmarket.dto.world;

import java.util.List;
import java.util.Objects;

public record UserSnapshot(String name, double currentBalance, List<Integer> ownedEventIds,
                           List<Integer> participatingEventIds, boolean blocked, List<UserEventPosition> positions) {
    public UserSnapshot(String name, double currentBalance, List<Integer> ownedEventIds,
                        List<Integer> participatingEventIds) {
        this(name, currentBalance, ownedEventIds, participatingEventIds, false, List.of());
    }
    public UserSnapshot {
        Objects.requireNonNull(name);
        if (!Double.isFinite(currentBalance)) throw new IllegalArgumentException("Balance must be finite");
        ownedEventIds = List.copyOf(ownedEventIds);
        participatingEventIds = List.copyOf(participatingEventIds);
        positions = List.copyOf(positions);
    }
}
