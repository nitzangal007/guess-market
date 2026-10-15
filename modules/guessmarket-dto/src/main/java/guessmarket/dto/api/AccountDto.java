package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record AccountDto(String username, String balance, boolean blocked,
                  List<Integer> ownedEventIds, List<Integer> participatingEventIds,
                  List<AccountEntryDto> entries) {
    public AccountDto {
        ownedEventIds = List.copyOf(ownedEventIds);
        participatingEventIds = List.copyOf(participatingEventIds);
        entries = List.copyOf(entries);
    }
}
