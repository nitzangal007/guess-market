package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record BookOrderDto(long sequence, String user, int option, OrderSide side,
                    int remainingQuantity, String limitPrice) {}
