package guessmarket.engine;
import guessmarket.dto.world.*;
import java.math.BigDecimal;
import java.util.*;

/** Historical cash flows remain separate from current inventory. */
record OrderBookLedger(List<OrderBookOrder> orders,List<OrderBookTrade> trades,
        List<Flow> flows,List<String> participants,Optional<ClosePreview> settlement) {
    enum Kind { FUNDING, PURCHASE, SALE, FEE_PAID, FEE_RECEIVED, SETTLEMENT }
    record Flow(String user,int option,Kind kind,BigDecimal amount) {}
    OrderBookLedger {
        orders=List.copyOf(orders);trades=List.copyOf(trades);flows=List.copyOf(flows);
        participants=List.copyOf(participants);settlement=Objects.requireNonNull(settlement);
    }
    static OrderBookLedger empty(){return new OrderBookLedger(List.of(),List.of(),List.of(),List.of(),Optional.empty());}
    OrderBookSnapshot snapshot(OrderBookState state,int d){
        var quotes=new ArrayList<OrderBookQuote>();
        for(int option=1;option<=2;option++){
            final int selected=option;
            var bid=orders.stream().filter(o->o.optionNumber()==selected&&o.side()==OrderSide.BUY)
                    .map(OrderBookOrder::limitPrice).max(BigDecimal::compareTo);
            var ask=orders.stream().filter(o->o.optionNumber()==selected&&o.side()==OrderSide.SELL)
                    .map(OrderBookOrder::limitPrice).min(BigDecimal::compareTo);
            var last=trades.reversed().stream().filter(t->t.optionNumber()==selected).map(OrderBookTrade::price).findFirst();
            Optional<BigDecimal> mid=bid.isPresent()&&ask.isPresent()?Optional.of(bid.get().add(ask.get()).divide(BigDecimal.TWO)):Optional.empty();
            Optional<BigDecimal> spread=bid.isPresent()&&ask.isPresent()?Optional.of(ask.get().subtract(bid.get())):Optional.empty();
            var estimate=settlement.isPresent()?Optional.of(BigDecimal.valueOf(settlement.get().winningOption()==option?d:0)):mid.isPresent()?mid:last;
            String basis=settlement.isPresent()?"SETTLEMENT":mid.isPresent()?"MID":last.isPresent()?"LAST":"UNAVAILABLE";
            quotes.add(new OrderBookQuote(option,bid,ask,last,mid,spread,estimate,basis));
        }
        var positions=new ArrayList<OrderBookPosition>();
        var names=new LinkedHashSet<>(participants);
        state.holdings().forEach(h->names.add(h.userName()));flows.forEach(f->names.add(f.user()));
        for(String name:names){
            var h=state.holdings().stream().filter(v->v.userName().equals(name)).findFirst().orElse(new OrderBookHolding(name,0,0));
            BigDecimal funding=sum(name,Kind.FUNDING),purchases=sum(name,Kind.PURCHASE),sales=sum(name,Kind.SALE),
                    fees=sum(name,Kind.FEE_PAID),income=sum(name,Kind.FEE_RECEIVED),payout=sum(name,Kind.SETTLEMENT);
            BigDecimal one=paid(name,1).add(funding.divide(BigDecimal.TWO)),two=paid(name,2).add(funding.divide(BigDecimal.TWO));
            Optional<BigDecimal> oneValue=value(h.optionOneShares(),quotes.getFirst());
            Optional<BigDecimal> twoValue=value(h.optionTwoShares(),quotes.get(1));
            Optional<BigDecimal> value=oneValue.isPresent()&&twoValue.isPresent()
                    ?Optional.of(oneValue.orElseThrow().add(twoValue.orElseThrow())):Optional.empty();
            int a=state.availableShares(name,1),b=state.availableShares(name,2);
            positions.add(new OrderBookPosition(name,h.optionOneShares(),h.optionTwoShares(),h.optionOneShares()-a,
                    h.optionTwoShares()-b,a,b,one,two,purchases,sales,fees,income,funding,payout,
                    sales.add(income).add(payout).subtract(funding).subtract(purchases).subtract(fees),oneValue,twoValue,value));
        }
        return new OrderBookSnapshot(state.issuedPairs(),state.holdings(),state.sellReservations(),orders,trades,quotes,positions,participants,settlement);
    }
    private BigDecimal sum(String name,Kind kind){return flows.stream().filter(f->f.user().equals(name)&&f.kind()==kind)
            .map(Flow::amount).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal paid(String name,int option){return flows.stream().filter(f->f.user().equals(name)&&f.kind()==Kind.PURCHASE&&f.option()==option)
            .map(Flow::amount).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private static Optional<BigDecimal> value(int shares,OrderBookQuote quote){
        return shares==0?Optional.of(BigDecimal.ZERO):quote.estimate().map(price->price.multiply(BigDecimal.valueOf(shares)));
    }
}
