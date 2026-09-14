package guessmarket.dto.world;

public record UserEventPosition(int eventId, int quantityOne, int quantityTwo,
        double totalShareCost, double totalPurchaseCommission, double totalPaid) {}

