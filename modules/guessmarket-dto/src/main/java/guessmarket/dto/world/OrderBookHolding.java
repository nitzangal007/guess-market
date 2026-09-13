package guessmarket.dto.world;

import java.util.Objects;

/** Share inventory only; valuation and cost attribution are not part of opening. */
public record OrderBookHolding(String userName,int optionOneShares,int optionTwoShares) {
    public OrderBookHolding {
        Objects.requireNonNull(userName);
        if(optionOneShares<0||optionTwoShares<0)throw new IllegalArgumentException("Negative inventory");
    }
}

