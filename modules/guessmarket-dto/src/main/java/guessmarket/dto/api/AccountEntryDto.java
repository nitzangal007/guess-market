package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record AccountEntryDto(long entryId, String transactionId, String timestamp,
                       String kind, Integer eventId, String eventName,
                       Integer optionNumber, String amount, String journalRunningBalance,
                       String storedBalanceAfterTransaction) {}
