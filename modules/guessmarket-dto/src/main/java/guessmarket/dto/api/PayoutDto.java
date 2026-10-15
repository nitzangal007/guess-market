package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record PayoutDto(String username, int winningShares, String grossPayout,
                 String commission, String netPayout) {}
