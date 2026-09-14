package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderRequest(String userName,int eventId,int optionNumber,OrderSide side,int quantity,BigDecimal limitPrice) {}
