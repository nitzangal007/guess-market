package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record ActionRequest(ActionKind action, Integer optionNumber,
                     Integer quantity, OrderSide side, String limitPrice) {}
