package guessmarket.dto.world;

import java.util.List;
import java.util.Optional;

/** Immutable Order Book inspection data, including historical cash flows and settlement. */
public record OrderBookSnapshot(int issuedPairs,List<OrderBookHolding> holdings,
        List<OrderBookSellReservation> sellReservations,List<OrderBookOrder> orders,
        List<OrderBookTrade> trades,List<OrderBookQuote> quotes,List<OrderBookPosition> positions,
        List<String> participants,Optional<ClosePreview> settlement) {
    public OrderBookSnapshot(int pairs,List<OrderBookHolding> holdings,List<OrderBookSellReservation> reservations){
        this(pairs,holdings,reservations,List.of(),List.of(),List.of(),List.of(),List.of(),Optional.empty());
    }
    public OrderBookSnapshot(int issuedPairs,List<OrderBookHolding> holdings){
        this(issuedPairs,holdings,List.of());
    }
    public OrderBookSnapshot {
        holdings=List.copyOf(holdings);
        sellReservations=List.copyOf(sellReservations);
        orders=List.copyOf(orders);trades=List.copyOf(trades);quotes=List.copyOf(quotes);
        positions=List.copyOf(positions);participants=List.copyOf(participants);
        if(issuedPairs<0)throw new IllegalArgumentException("Negative issued pairs");
    }
}
