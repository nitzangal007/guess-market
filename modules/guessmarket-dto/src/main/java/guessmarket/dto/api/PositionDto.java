package guessmarket.dto.api;
import java.util.List;
import guessmarket.dto.world.OrderSide;

public record PositionDto(int optionOneShares, int optionTwoShares,
                   int reservedOne, int reservedTwo, int availableOne, int availableTwo,
                   String paidOne, String paidTwo, String funding, String purchases,
                   String saleReceipts, String feesPaid, String feesReceived,
                   String payout, String profitLoss, String valueOne, String valueTwo) {}
