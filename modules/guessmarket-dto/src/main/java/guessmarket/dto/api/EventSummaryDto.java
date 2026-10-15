package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record EventSummaryDto(int id, String name, String description, String method,
                       String status, String owner, int commissionPercentage,
                       String commissionMode, String contractBalance) {}
