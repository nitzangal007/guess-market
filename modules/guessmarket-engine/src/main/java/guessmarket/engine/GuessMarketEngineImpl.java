package guessmarket.engine;

import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.dto.PurchaseReceipt;
import guessmarket.engine.xml.XmlMarketLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuessMarketEngineImpl implements GuessMarketEngine {
    private final EventLoader eventLoader;
    private final StateStore stateStore;
    private Map<Integer, MarketEvent> events = new LinkedHashMap<>();

    public GuessMarketEngineImpl() {
        this(new XmlMarketLoader(), new SavedStateStore(), new LinkedHashMap<>());
    }

    GuessMarketEngineImpl(
            XmlMarketLoader xmlLoader,
            SavedStateStore stateStore,
            Map<Integer, MarketEvent> initialEvents) {
        this(
                Objects.requireNonNull(xmlLoader, "xmlLoader")::load,
                adapt(Objects.requireNonNull(stateStore, "stateStore")),
                initialEvents);
    }

    GuessMarketEngineImpl(
            EventLoader eventLoader,
            StateStore stateStore,
            Map<Integer, MarketEvent> initialEvents) {
        this.eventLoader = Objects.requireNonNull(eventLoader, "eventLoader");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        events = new LinkedHashMap<>(Objects.requireNonNull(initialEvents, "initialEvents"));
    }

    @Override
    public int loadEventsFromXml(Path path) throws EngineOperationException {
        Map<Integer, MarketEvent> candidate = eventLoader.load(path);
        events = candidate;
        return candidate.size();
    }

    @Override
    public List<MarketEventSummary> listEvents() throws EngineOperationException {
        requireLoadedSystem();
        List<MarketEventSummary> summaries = new ArrayList<>(events.size());
        for (MarketEvent event : events.values()) {
            summaries.add(event.toSummary());
        }
        return List.copyOf(summaries);
    }

    @Override
    public MarketEventDetails getEventDetails(int eventId) throws EngineOperationException {
        return requireEvent(eventId).toDetails();
    }

    @Override
    public PurchaseReceipt purchaseShares(int eventId, int optionNumber, int quantity)
            throws EngineOperationException {
        MarketEvent event = requireEvent(eventId);
        TradeRecord record = event.purchase(optionNumber, quantity);
        MarketEventDetails resultingDetails = event.toDetails();
        return new PurchaseReceipt(
                record.getOptionNumber(),
                record.getQuantity(),
                record.getBaseShareCost(),
                record.getPurchaseCommission(),
                record.getTotalPaid(),
                resultingDetails);
    }

    @Override
    public MarketEventDetails closeEvent(int eventId, int winningOptionNumber)
            throws EngineOperationException {
        MarketEvent event = requireEvent(eventId);
        event.close(winningOptionNumber);
        return event.toDetails();
    }

    @Override
    public void saveState(Path path) throws EngineOperationException {
        requireLoadedSystem();
        stateStore.save(path, events.values());
    }

    @Override
    public int restoreState(Path path) throws EngineOperationException {
        Map<Integer, MarketEvent> candidate = stateStore.restore(path);
        events = candidate;
        return candidate.size();
    }

    private MarketEvent requireEvent(int eventId) throws EngineOperationException {
        requireLoadedSystem();
        MarketEvent event = events.get(eventId);
        if (event == null) {
            throw new EngineOperationException(
                    EngineErrorCode.EVENT_NOT_FOUND,
                    "Event ID " + eventId + " does not exist",
                    "Choose an event from the displayed list",
                    null,
                    null,
                    eventId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        return event;
    }

    private void requireLoadedSystem() throws EngineOperationException {
        if (events.isEmpty()) {
            throw new EngineOperationException(
                    EngineErrorCode.NO_SYSTEM_LOADED,
                    "No system is loaded",
                    "Load XML or restore saved state first");
        }
    }

    private static StateStore adapt(SavedStateStore stateStore) {
        return new StateStore() {
            @Override
            public void save(Path path, Collection<MarketEvent> events)
                    throws EngineOperationException {
                stateStore.save(path, events);
            }

            @Override
            public Map<Integer, MarketEvent> restore(Path path)
                    throws EngineOperationException {
                return stateStore.restore(path);
            }
        };
    }

    @FunctionalInterface
    interface EventLoader {
        Map<Integer, MarketEvent> load(Path path) throws EngineOperationException;
    }

    interface StateStore {
        void save(Path path, Collection<MarketEvent> events) throws EngineOperationException;

        Map<Integer, MarketEvent> restore(Path path) throws EngineOperationException;
    }
}
