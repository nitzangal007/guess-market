package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record CommandRequest(String requestId, long expectedRevision, ActionRequest action) {}
