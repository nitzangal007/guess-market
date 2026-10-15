package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record BookQuoteDto(int option, String bid, String ask, String last,
                    String mid, String spread, String estimate, String valuationBasis) {}
