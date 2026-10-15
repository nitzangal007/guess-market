package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record PublicUserDto(String name, String balance, boolean marketMaker) {}
