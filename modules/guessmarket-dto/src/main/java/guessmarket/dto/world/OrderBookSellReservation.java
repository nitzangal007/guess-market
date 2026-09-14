package guessmarket.dto.world;

import java.util.Objects;

/** One waiting SELL commitment within a single event. */
public record OrderBookSellReservation(long orderSequence,String userName,int optionNumber,int quantity) {
    public OrderBookSellReservation {
        Objects.requireNonNull(userName);
        if(orderSequence<=0||(optionNumber!=1&&optionNumber!=2)||quantity<=0)
            throw new IllegalArgumentException("Invalid sell reservation");
    }
}

