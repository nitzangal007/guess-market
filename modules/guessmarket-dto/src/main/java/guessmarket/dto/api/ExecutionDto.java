package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record ExecutionDto(long sequence, int option, String buyer, String counterparty,
                    CounterpartyRole counterpartyRole, int quantity, String price,
                    String principal, String commission, boolean mint) {}
