package guessmarket.engine.xml;

import guessmarket.dto.CommissionMode;
import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.MarketEvent;
import guessmarket.engine.xml.generated.GMEvent;
import guessmarket.engine.xml.generated.GuessMarket;
import java.util.LinkedHashMap;
import java.util.List;

final class JaxbMarketMapper {
    LinkedHashMap<Integer, MarketEvent> map(GuessMarket root)
            throws EngineOperationException {
        LinkedHashMap<Integer, MarketEvent> candidate = new LinkedHashMap<>();
        List<GMEvent> xmlEvents = root.getGMEvents().getGMEvent();

        for (int index = 0; index < xmlEvents.size(); index++) {
            GMEvent xmlEvent = xmlEvents.get(index);
            int eventNumber = index + 1;
            int eventId = xmlEvent.getId();

            if (candidate.containsKey(eventId)) {
                throw invalidData(eventNumber, eventId, "id",
                        "Each event ID must be unique within the loaded market.");
            }

            int commission = xmlEvent.getComision().getValue();
            if (commission < 0 || commission > 90) {
                throw invalidData(eventNumber, eventId, "comision",
                        "Commission must be between 0 and 90.");
            }

            List<String> options = xmlEvent.getGMOptions().getGMOption();
            if (options.size() != 2) {
                throw invalidData(eventNumber, eventId, "GM-options",
                        "Each event must contain exactly two options.");
            }

            int b = xmlEvent.getGMMethod().getGMLMSR().getB();
            if (b <= 0) {
                throw invalidData(eventNumber, eventId, "b",
                        "Liquidity parameter b must be positive.");
            }

            MarketEvent event = new MarketEvent(
                    eventId,
                    String.join(" ", xmlEvent.getName()).trim(),
                    xmlEvent.getDescription().trim(),
                    commissionMode(xmlEvent.getComision().getType()),
                    commission,
                    b,
                    options.get(0).trim(),
                    options.get(1).trim());
            candidate.put(eventId, event);
        }

        return candidate;
    }

    private static CommissionMode commissionMode(String xmlCommissionType) {
        return switch (xmlCommissionType) {
            case "on-purchase" -> CommissionMode.ON_PURCHASE;
            case "on-close" -> CommissionMode.ON_CLOSE;
            default -> throw new IllegalArgumentException(
                    "Unsupported schema commission type: " + xmlCommissionType);
        };
    }

    private static EngineOperationException invalidData(
            int eventNumber,
            int eventId,
            String fieldName,
            String detail) {
        return new EngineOperationException(
                EngineErrorCode.XML_DATA_INVALID,
                detail,
                "Correct the event data and try loading the XML file again.",
                null,
                eventNumber,
                eventId,
                fieldName,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
