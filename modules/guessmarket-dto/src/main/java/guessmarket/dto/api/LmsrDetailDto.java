package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record LmsrDetailDto(int b, List<LmsrOptionDto> options,
                    String commissionTotal, List<LmsrTradeDto> history) {
    public LmsrDetailDto {
        options = List.copyOf(options);
        history = List.copyOf(history);
    }
}
