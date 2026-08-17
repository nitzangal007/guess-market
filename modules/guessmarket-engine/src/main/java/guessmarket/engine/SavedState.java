package guessmarket.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

final class SavedState implements Serializable {
    private static final long serialVersionUID = 1L;

    static final int CURRENT_FORMAT_VERSION = 1;

    private final int formatVersion;
    private final ArrayList<MarketEvent> events;

    SavedState(Collection<MarketEvent> events) {
        this(CURRENT_FORMAT_VERSION, events);
    }

    SavedState(int formatVersion, Collection<MarketEvent> events) {
        this.formatVersion = formatVersion;
        this.events = new ArrayList<>(Objects.requireNonNull(events, "events"));
    }

    int getFormatVersion() {
        return formatVersion;
    }

    ArrayList<MarketEvent> getEvents() {
        return events;
    }
}
