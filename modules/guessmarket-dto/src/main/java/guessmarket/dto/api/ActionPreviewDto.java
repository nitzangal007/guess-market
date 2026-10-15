package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record ActionPreviewDto(String serverInstanceId, long revision, int eventId,
                        String eventName, String actingUser, String winningLabel, ActionRequest action, String totalDebit, String totalCredit,
                        String balanceBefore, String balanceAfter, boolean blockedAfter,
                        List<CashEffectDto> ownCashEffects, List<ExecutionDto> executions,
                        Integer incomingRemaining, Integer cancelledOrderCount,
                        Integer releasedReservationQuantity, List<PayoutDto> payouts,
                        String totalGrossPayout, String totalCommission,
                        String subsidyRefund, String explanation) {
    public ActionPreviewDto {
        ownCashEffects = List.copyOf(ownCashEffects);
        executions = List.copyOf(executions);
        payouts = List.copyOf(payouts);
    }
}
