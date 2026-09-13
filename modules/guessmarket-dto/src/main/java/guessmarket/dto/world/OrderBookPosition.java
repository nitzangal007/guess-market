package guessmarket.dto.world;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public record OrderBookPosition(String userName,int optionOneShares,int optionTwoShares,int reservedOne,int reservedTwo,int availableOne,int availableTwo,BigDecimal optionOnePaid,BigDecimal optionTwoPaid,BigDecimal purchases,BigDecimal saleReceipts,BigDecimal commissionsPaid,BigDecimal commissionIncome,BigDecimal funding,BigDecimal settlementReceipts,BigDecimal profitLoss,Optional<BigDecimal> optionOneValue,Optional<BigDecimal> optionTwoValue,Optional<BigDecimal> estimatedValue) {}
