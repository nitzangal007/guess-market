package guessmarket.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;

/**
 * Pure arithmetic for already validated quotes, not an order submission command.
 * The caller must resolve identities, inventory and account eligibility.
 * No cash, fees, lifecycle or book insertion is applied here.
 */
final class OrderBookMatcher {
    enum Side { BUY, SELL }
    enum MintStatus { MATCHED, NO_MATCH }

    record Quote(int quantity,BigDecimal limit) {
        Quote {
            Objects.requireNonNull(limit);
            if(quantity<=0||limit.signum()<0)throw new IllegalArgumentException("Invalid arithmetic quote");
        }
    }
    record Fill(int restingIndex,int quantity,BigDecimal price) {}
    record OrdinaryResult(List<Fill> fills,int incomingRemaining,List<Quote> restingRemaining,BigDecimal principal) {
        OrdinaryResult {fills=List.copyOf(fills);restingRemaining=List.copyOf(restingRemaining);}
    }
    record MintFill(int quantity,BigDecimal incomingPrice,BigDecimal restingPrice,
                    int incomingRemaining,int restingRemaining) {
        BigDecimal incomingPrincipal(){return incomingPrice.multiply(BigDecimal.valueOf(quantity));}
        BigDecimal restingPrincipal(){return restingPrice.multiply(BigDecimal.valueOf(quantity));}
        BigDecimal contractCredit(){return incomingPrincipal().add(restingPrincipal());}
    }
    record MintAttempt(MintStatus status,Optional<MintFill> fill) {}
    record SubmittedOrder(long sequence,String userName,Quote quote) {
        SubmittedOrder {
            Objects.requireNonNull(quote);
            Objects.requireNonNull(userName);
            if(sequence<=0)throw new IllegalArgumentException("Positive submission sequence required");
        }
    }
    record SubmittedFill(long sequence,int quantity,BigDecimal price) {}
    record OrderMatchResult(List<SubmittedFill> fills,int incomingRemaining,
                            List<SubmittedOrder> restingRemaining,BigDecimal principal) {
        OrderMatchResult {fills=List.copyOf(fills);restingRemaining=List.copyOf(restingRemaining);}
    }
    record SubmittedMint(long sequence,MintFill fill) {}
    record CombinedResult(OrderMatchResult ordinary,List<SubmittedMint> mints,int incomingRemaining,
                          List<SubmittedOrder> oppositeRemaining) {
        CombinedResult {mints=List.copyOf(mints);oppositeRemaining=List.copyOf(oppositeRemaining);}
    }

    private OrderBookMatcher() {}

    /** Approved ordinary-first scheduling, then highest opposite buy price and FIFO for mint. */
    static CombinedResult matchThenMint(Side side,Quote incoming,List<SubmittedOrder> sameOptionResting,
                                       List<SubmittedOrder> oppositeBuys,int denominator,boolean allowMint) {
        var ordinary=matchNewOrder(side,incoming,sameOptionResting);
        List<SubmittedOrder> opposite=List.copyOf(oppositeBuys);
        int remaining=ordinary.incomingRemaining();
        if(side==Side.SELL||!allowMint||remaining==0)
            return new CombinedResult(ordinary,List.of(),remaining,opposite);
        if(denominator<=0)throw new IllegalArgumentException("Positive denominator required");
        if(opposite.stream().map(SubmittedOrder::sequence).distinct().count()!=opposite.size())
            throw new IllegalArgumentException("Duplicate opposite submission sequence");
        BigDecimal base=BigDecimal.valueOf(denominator);
        var eligible=opposite.stream().filter(order->incoming.limit().add(order.quote().limit()).compareTo(base)>=0)
                .sorted(Comparator.comparing((SubmittedOrder order)->order.quote().limit()).reversed()
                        .thenComparingLong(SubmittedOrder::sequence)).toList();

        var minted=new ArrayList<SubmittedMint>();
        var quantities=new java.util.HashMap<Long,Integer>();
        for(var order:eligible){
            if(remaining==0)break;
            var fill=mint(new Quote(remaining,incoming.limit()),order.quote(),denominator).fill().orElseThrow();
            minted.add(new SubmittedMint(order.sequence(),fill));
            remaining=fill.incomingRemaining();
            quantities.put(order.sequence(),fill.restingRemaining());
        }
        var residual=new ArrayList<SubmittedOrder>();
        for(var order:opposite){
            int quantity=quantities.getOrDefault(order.sequence(),order.quote().quantity());
            if(quantity>0)residual.add(new SubmittedOrder(order.sequence(),order.userName(),new Quote(quantity,order.quote().limit())));
        }
        return new CombinedResult(ordinary,minted,remaining,residual);
    }

    /** Reject the entire ordinary plan before any caller may apply fills. Owners may trade with others. */
    static OrderMatchResult planNewOrder(String user,Side side,Quote incoming,List<SubmittedOrder> resting)
            throws WorldCommandException {
        Objects.requireNonNull(user);
        var result=matchNewOrder(side,incoming,resting);
        var ownSequences=resting.stream().filter(order->order.userName().equals(user))
                .map(SubmittedOrder::sequence).collect(java.util.stream.Collectors.toSet());
        if(result.fills().stream().anyMatch(fill->ownSequences.contains(fill.sequence())))
            throw new WorldCommandException(WorldCommandException.Code.SELF_MATCH_REJECTED,
                    "This order would trade against your own resting order. The entire new order was rejected.");
        return result;
    }

    /** Immediately evaluates a new order using approved best price, then oldest submission sequence. */
    static OrderMatchResult matchNewOrder(Side incomingSide,Quote incoming,List<SubmittedOrder> restingOrders) {
        Objects.requireNonNull(incomingSide);
        List<SubmittedOrder> resting=List.copyOf(restingOrders);
        if(resting.stream().map(SubmittedOrder::sequence).distinct().count()!=resting.size())
            throw new IllegalArgumentException("Duplicate submission sequence");
        Comparator<SubmittedOrder> prices=Comparator.comparing(order->order.quote().limit());
        if(incomingSide==Side.SELL)prices=prices.reversed();
        var ordered=resting.stream().sorted(prices.thenComparingLong(SubmittedOrder::sequence)).toList();
        var result=ordinary(incomingSide,incoming,ordered.stream().map(SubmittedOrder::quote).toList());
        var fills=new ArrayList<SubmittedFill>();
        int[] consumed=new int[ordered.size()];
        for(var fill:result.fills()){
            fills.add(new SubmittedFill(ordered.get(fill.restingIndex()).sequence(),fill.quantity(),fill.price()));
            consumed[fill.restingIndex()]=fill.quantity();
        }
        var residual=new ArrayList<SubmittedOrder>();
        for(int i=0;i<ordered.size();i++){
            var order=ordered.get(i);
            int quantity=order.quote().quantity()-consumed[i];
            if(quantity>0)residual.add(new SubmittedOrder(order.sequence(),order.userName(),new Quote(quantity,order.quote().limit())));
        }
        return new OrderMatchResult(fills,result.incomingRemaining(),residual,result.principal());
    }

    /**
     * Consumes opposite-side quotes in the caller's explicit priority order.
     * Best price ordering is checked. Equal-price order is supplied, not inferred as FIFO here.
     */
    static OrdinaryResult ordinary(Side incomingSide,Quote incoming,List<Quote> orderedResting) {
        Objects.requireNonNull(incomingSide);Objects.requireNonNull(incoming);
        List<Quote> resting=List.copyOf(orderedResting);
        for(int i=1;i<resting.size();i++){
            int order=resting.get(i-1).limit().compareTo(resting.get(i).limit());
            if((incomingSide==Side.BUY&&order>0)||(incomingSide==Side.SELL&&order<0))
                throw new IllegalArgumentException("Caller must supply best-price priority order");
        }
        int remaining=incoming.quantity();
        BigDecimal principal=BigDecimal.ZERO;
        var fills=new ArrayList<Fill>();
        var residual=new ArrayList<Quote>();
        for(int i=0;i<resting.size();i++){
            Quote quote=resting.get(i);
            int comparison=incoming.limit().compareTo(quote.limit());
            boolean crosses=incomingSide==Side.BUY?comparison>=0:comparison<=0;
            if(remaining==0||!crosses){residual.add(quote);continue;}
            int quantity=Math.min(remaining,quote.quantity());
            fills.add(new Fill(i,quantity,quote.limit()));
            principal=principal.add(quote.limit().multiply(BigDecimal.valueOf(quantity)));
            remaining-=quantity;
            int rest=quote.quantity()-quantity;
            if(rest>0)residual.add(new Quote(rest,quote.limit()));
        }
        return new OrdinaryResult(fills,remaining,residual,principal);
    }

    /**
     * User-approved September 11: opposite buy limits summing to at least d mint.
     * Resting price is honored; incoming pays its complement.
     */
    static MintAttempt mint(Quote incoming,Quote resting,int denominator) {
        Objects.requireNonNull(incoming);Objects.requireNonNull(resting);
        if(denominator<=0)throw new IllegalArgumentException("Positive denominator required");
        BigDecimal base=BigDecimal.valueOf(denominator);
        if(incoming.limit().compareTo(base)>0||resting.limit().compareTo(base)>0)
            throw new IllegalArgumentException("Quote exceeds the binary arithmetic range");
        int comparison=incoming.limit().add(resting.limit()).compareTo(base);
        if(comparison<0)return new MintAttempt(MintStatus.NO_MATCH,Optional.empty());
        int quantity=Math.min(incoming.quantity(),resting.quantity());
        var fill=new MintFill(quantity,base.subtract(resting.limit()),resting.limit(),
                incoming.quantity()-quantity,resting.quantity()-quantity);
        return new MintAttempt(MintStatus.MATCHED,Optional.of(fill));
    }
}
