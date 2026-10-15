package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record BookDetailDto(int d, int initial, boolean allowMint, int issuedPairs,
                     List<BookOrderDto> orders, List<ExecutionDto> trades,
                     List<BookQuoteDto> quotes, List<ParticipantDto> participants) {
    public BookDetailDto {
        orders = List.copyOf(orders);
        trades = List.copyOf(trades);
        quotes = List.copyOf(quotes);
        participants = List.copyOf(participants);
    }
}
