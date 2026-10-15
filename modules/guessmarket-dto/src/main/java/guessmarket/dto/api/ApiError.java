package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record ApiError(String code, String message, String field, String recovery,
                String requestId, String serverInstanceId, Long revision) {}
