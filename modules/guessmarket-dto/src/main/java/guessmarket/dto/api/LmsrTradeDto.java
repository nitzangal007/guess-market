package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record LmsrTradeDto(long sequence, String user, int option, int quantity,
                   String shareCost, String commission, String totalPaid) {}
