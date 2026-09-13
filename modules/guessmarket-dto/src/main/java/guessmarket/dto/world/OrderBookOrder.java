package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderBookOrder(long sequence,String userName,int optionNumber,OrderSide side,int quantity,BigDecimal limitPrice) {}
