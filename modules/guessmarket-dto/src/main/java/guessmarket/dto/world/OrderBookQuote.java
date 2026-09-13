package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderBookQuote(int optionNumber,Optional<BigDecimal> bid,Optional<BigDecimal> ask,Optional<BigDecimal> last,Optional<BigDecimal> mid,Optional<BigDecimal> spread,Optional<BigDecimal> estimate,String valuationBasis) {}
