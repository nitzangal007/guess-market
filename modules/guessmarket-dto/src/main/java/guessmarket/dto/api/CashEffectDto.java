package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record CashEffectDto(String kind, Integer optionNumber, String amount,
                     String journalRunningBalance, String storedBalanceAfterTransaction) {}
