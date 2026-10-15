package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record LoginResponse(String serverInstanceId, long revision,
                     String username, boolean createdAccount) {}
