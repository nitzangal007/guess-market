package guessmarket.dto.world;

public record PurchasePreview(long worldRevision, String actingUser, int eventId, String eventName,
        int optionNumber, String optionLabel, int quantity, double shareCost, double commission,
        double totalDebit, double commissionReceived, double balanceBefore, double balanceAfter,
        boolean becomesBlocked) {
    /** Final blocked eligibility is becomesBlocked; this describes the earlier debit stage. */
    public boolean overdraftBeforeReceipts() { return balanceBefore-totalDebit<0; }
}
