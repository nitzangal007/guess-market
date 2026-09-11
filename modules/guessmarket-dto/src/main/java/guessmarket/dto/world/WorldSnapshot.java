package guessmarket.dto.world;

import java.util.List;

public record WorldSnapshot(List<EventSnapshot> events, List<UserSnapshot> users) {
    public WorldSnapshot {
        events = List.copyOf(events);
        users = List.copyOf(users);
    }
}
