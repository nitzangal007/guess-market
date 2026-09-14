package guessmarket.engine;

import guessmarket.dto.world.LmsrTradingSnapshot;
import guessmarket.dto.world.ClosePreview;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

record LmsrTradingState(int quantityOne, int quantityTwo, double totalCommission,
                       List<LmsrPurchaseRecord> history, Optional<ClosePreview> settlement) {
    LmsrTradingState(int one, int two, double commission, List<LmsrPurchaseRecord> history) {
        this(one,two,commission,history,Optional.empty());
    }
    LmsrTradingState {
        history = List.copyOf(history);
        if (quantityOne < 0 || quantityTwo < 0 || !Double.isFinite(totalCommission) || totalCommission < 0)
            throw new IllegalArgumentException("Invalid LMSR trading state");
    }
    static LmsrTradingState empty() { return new LmsrTradingState(0,0,0,List.of()); }
    LmsrTradingState purchased(String user, int option, int quantity, double base, double fee) {
        int one = option == 1 ? Math.addExact(quantityOne, quantity) : quantityOne;
        int two = option == 2 ? Math.addExact(quantityTwo, quantity) : quantityTwo;
        var next = new ArrayList<>(history);
        next.add(new LmsrPurchaseRecord((long)history.size()+1, user, option, quantity, base, fee));
        return new LmsrTradingState(one, two, totalCommission+fee, next);
    }
    LmsrTradingSnapshot snapshot(int eventId, List<String> labels, int b) {
        var entries = history.reversed().stream().map(record ->
                record.snapshot(eventId, labels.get(record.optionNumber()-1))).toList();
        return new LmsrTradingSnapshot(quantityOne, quantityTwo,
                LmsrCalculator.priceForOption(quantityOne,quantityTwo,b),
                LmsrCalculator.priceForOption(quantityTwo,quantityOne,b), totalCommission, entries, settlement);
    }
}
