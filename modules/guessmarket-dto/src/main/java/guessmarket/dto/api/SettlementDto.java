package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record SettlementDto(int winningOption, String winningLabel,
                     String finalContractBalance, String commissionTotal,
                     String totalGrossPayout, String totalCommission, String subsidyRefund,
                     List<PayoutDto> payouts) {
    public SettlementDto {
        payouts = List.copyOf(payouts);
    }
}
