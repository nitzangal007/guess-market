package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record EventDetailDto(EventSummaryDto summary, List<String> optionLabels,
                      LmsrDetailDto lmsr, BookDetailDto book,
                      PositionDto ownPosition, SettlementDto settlement,
                      boolean canOpen, boolean canTrade, boolean canClose) {
    public EventDetailDto {
        optionLabels = List.copyOf(optionLabels);
    }
}
