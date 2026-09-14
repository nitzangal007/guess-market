package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderBookTrade(long sequence,String buyer,String seller,int optionNumber,int quantity,BigDecimal price,BigDecimal principal,BigDecimal commission,boolean mint) {}
