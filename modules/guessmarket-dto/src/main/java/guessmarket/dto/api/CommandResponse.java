package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record CommandResponse(String serverInstanceId, long revision, String requestId,
                       OperationKind operation, Integer eventId, ActionRequest confirmedAction,
                       String creditedAmount, ActionPreviewDto confirmedReview,
                       String balanceBefore, String balanceAfter, boolean blockedAfter,
                       List<AccountEntryDto> ownTransactionEntries, String message) {
    public CommandResponse {
        ownTransactionEntries = List.copyOf(ownTransactionEntries);
    }
}
