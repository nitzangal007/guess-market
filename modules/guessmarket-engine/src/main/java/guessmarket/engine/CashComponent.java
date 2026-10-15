package guessmarket.engine;
import java.math.BigDecimal;
record CashComponent(String username,String kind,Integer eventId,String eventName,Integer optionNumber,BigDecimal signedAmount) {}
