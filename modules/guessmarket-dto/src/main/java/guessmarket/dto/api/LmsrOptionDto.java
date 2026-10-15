package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record LmsrOptionDto(int option, String label, String price, int sharesBought) {}
