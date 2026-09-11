package guessmarket.dto.world;

import java.util.List;
public record ClosePreview(long worldRevision, int eventId, String eventName, String actingUser,
        int winningOption, String winningLabel, List<SettlementPayment> payments,
        double totalGrossPayout, double totalCommission, double subsidyRefund) {
    public ClosePreview { payments=List.copyOf(payments); }
}

