package guessmarket.dto.world;

public record PurchaseEntry(long sequence, String userName, int eventId, int optionNumber,
        String optionLabel, int quantity, double shareCost, double commission, double totalPaid) {}

