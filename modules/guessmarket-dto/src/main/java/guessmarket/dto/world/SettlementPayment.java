package guessmarket.dto.world;

public record SettlementPayment(String userName, int winningShares, double grossPayout,
                                double commission, double netPayout) {}

