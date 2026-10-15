package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record ParticipantDto(String username, int optionOneShares, int optionTwoShares,
                       String optionOneValue, String optionTwoValue) {}
