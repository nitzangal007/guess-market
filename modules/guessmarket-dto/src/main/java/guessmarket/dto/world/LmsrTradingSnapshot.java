package guessmarket.dto.world;

import java.util.List;
import java.util.Optional;
public record LmsrTradingSnapshot(int quantityOne, int quantityTwo, double priceOne, double priceTwo,
        double totalPurchaseCommission, List<PurchaseEntry> newestFirstHistory, Optional<ClosePreview> settlement) {
    public LmsrTradingSnapshot { newestFirstHistory = List.copyOf(newestFirstHistory); }
}
