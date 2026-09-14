package guessmarket.dto.world;

public record PurchaseResult(WorldSnapshot world, PurchaseEntry receipt, boolean newlyBlocked) {}

