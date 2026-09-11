package guessmarket.engine.xml.ex2;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.LmsrConfiguration;
import guessmarket.dto.world.OrderBookConfiguration;
import guessmarket.dto.world.PricingConfiguration;
import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.MarketUser;
import guessmarket.engine.MarketWorld;
import guessmarket.engine.WorldEvent;
import guessmarket.engine.xml.ex2.generated.GMEvent;
import guessmarket.engine.xml.ex2.generated.GuessMarket;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

final class Ex2JaxbWorldMapper {
    MarketWorld map(GuessMarket root) throws EngineOperationException {
        var definitions = new LinkedHashMap<Integer, GMEvent>();
        var pricing = new LinkedHashMap<Integer, PricingConfiguration>();
        for (GMEvent event : root.getGMEvents().getGMEvent()) {
            int id = event.getId();
            if (definitions.putIfAbsent(id, event) != null)
                throw invalid("id", "Duplicate event ID " + id + ".");
            if (event.getCommission().getValue() < 0 || event.getCommission().getValue() > 90)
                throw invalid("commission", "Event " + id + ": commission must be between 0 and 90.");
            if (event.getGMOptions().getGMOption().size() != 2)
                throw invalid("GM-options", "Event " + id + " must have exactly two options.");
            var method = event.getGMMethod();
            if (method.getGMLMSR() != null) {
                int b = method.getGMLMSR().getB();
                if (b <= 0) throw invalid("b", "Event " + id + ": b must be positive.");
                pricing.put(id, new LmsrConfiguration(b));
            } else {
                var book = method.getGMOrderBook();
                if (book.getInitial() < 0) throw invalid("initial", "Event " + id + ": initial must be zero or positive.");
                if (book.getD() <= 0) throw invalid("d", "Event " + id + ": d must be positive.");
                pricing.put(id, new OrderBookConfiguration(book.getInitial(), book.getD(),
                        Boolean.parseBoolean(book.getAllowMint())));
            }
        }
        var users = new LinkedHashMap<String, MarketUser>();
        var owners = new LinkedHashMap<Integer, String>();
        for (var user : root.getGMUsers().getGMUser()) {
            String name = user.getName().trim();
            if (users.containsKey(name)) throw invalid("name", "Duplicate username '" + name + "'.");
            if (user.getInitialCash() <= 0)
                throw invalid("initial-cash", "User '" + name + "': initial-cash must be greater than zero.");
            var owned = new LinkedHashSet<Integer>();
            if (user.getGMMarketMaker() != null) {
                for (var reference : user.getGMMarketMaker().getEvent()) {
                    int id = reference.getId();
                    if (!definitions.containsKey(id))
                        throw invalid("GM-market-maker", "User '" + name + "' references nonexistent event " + id + ".");
                    String previous = owners.putIfAbsent(id, name);
                    if (previous != null && !previous.equals(name))
                        throw invalid("GM-market-maker", "Event " + id + " has two market makers: '" + previous + "' and '" + name + "'.");
                    owned.add(id); // Repeated links by the same owner are idempotent.
                }
            }
            users.put(name, new MarketUser(name, user.getInitialCash(), List.copyOf(owned)));
        }
        var events = new LinkedHashMap<Integer, WorldEvent>();
        for (var event : definitions.values()) {
            int id = event.getId();
            if (!owners.containsKey(id)) throw invalid("GM-market-maker", "Event " + id + " has no assigned market maker.");
            CommissionMode mode = switch (event.getCommission().getType()) {
                case "on-purchase" -> CommissionMode.ON_PURCHASE;
                case "on-close" -> CommissionMode.ON_CLOSE;
                default -> throw invalid("commission", "Unsupported commission method for event " + id + ".");
            };
            events.put(id, new WorldEvent(id, event.getName().trim(), event.getDescription().trim(),
                    event.getGMOptions().getGMOption().stream().map(String::trim).toList(), event.getCommission().getValue(),
                    mode, owners.get(id), pricing.get(id)));
        }
        return new MarketWorld(events, users);
    }

    private static EngineOperationException invalid(String field, String detail) {
        return new EngineOperationException(EngineErrorCode.XML_DATA_INVALID, detail,
                "Correct the EX2 XML data and try loading again.", null, null, null, field,
                null, null, null, null, null, null);
    }
}
