package guessmarket.engine;

import guessmarket.dto.world.OrderBookHolding;
import guessmarket.dto.world.OrderBookSnapshot;
import guessmarket.dto.world.OrderBookSellReservation;
import java.util.List;
import static guessmarket.engine.WorldCommandException.Code.*;

/** Immutable opening inventory; no automatic orders or trading policies are introduced here. */
public record OrderBookState(int issuedPairs,List<OrderBookHolding> holdings,
                             List<OrderBookSellReservation> sellReservations,OrderBookLedger ledger) {
    public OrderBookState(int issuedPairs,List<OrderBookHolding> holdings,List<OrderBookSellReservation> reservations){
        this(issuedPairs,holdings,reservations,OrderBookLedger.empty());
    }
    public OrderBookState(int issuedPairs,List<OrderBookHolding> holdings){
        this(issuedPairs,holdings,List.of());
    }
    public OrderBookState {
        holdings=List.copyOf(holdings);
        sellReservations=List.copyOf(sellReservations);
        if(issuedPairs<0
                ||holdings.stream().mapToLong(OrderBookHolding::optionOneShares).sum()!=issuedPairs
                ||holdings.stream().mapToLong(OrderBookHolding::optionTwoShares).sum()!=issuedPairs
                ||holdings.stream().map(OrderBookHolding::userName).distinct().count()!=holdings.size())
            throw new IllegalArgumentException("Inventory must equal the issued pairs");
        if(sellReservations.stream().map(OrderBookSellReservation::orderSequence).distinct().count()!=sellReservations.size())
            throw new IllegalArgumentException("Duplicate sell reservation");
        for(var reservation:sellReservations){
            int held=holdings.stream().filter(h->h.userName().equals(reservation.userName()))
                    .mapToInt(h->reservation.optionNumber()==1?h.optionOneShares():h.optionTwoShares()).sum();
            long reserved=sellReservations.stream().filter(r->r.userName().equals(reservation.userName())
                    &&r.optionNumber()==reservation.optionNumber()).mapToLong(OrderBookSellReservation::quantity).sum();
            if(reserved>held)throw new IllegalArgumentException("Sell reservations exceed held shares");
        }
    }
    static OrderBookState empty(){return new OrderBookState(0,List.of());}
    static OrderBookState opened(String owner,int pairs){
        return new OrderBookState(pairs,List.of(new OrderBookHolding(owner,pairs,pairs)));
    }
    int availableShares(String user,int option){
        if(option!=1&&option!=2)throw new IllegalArgumentException("Invalid option");
        int held=holdings.stream().filter(h->h.userName().equals(user))
                .mapToInt(h->option==1?h.optionOneShares():h.optionTwoShares()).sum();
        int reserved=sellReservations.stream().filter(r->r.userName().equals(user)&&r.optionNumber()==option)
                .mapToInt(OrderBookSellReservation::quantity).sum();
        return held-reserved;
    }
    OrderBookState reserveSell(long sequence,String user,int option,int quantity) throws WorldCommandException {
        if(option!=1&&option!=2)throw new WorldCommandException(INVALID_OPTION,"Choose option 1 or 2.");
        if(sequence<=0||sellReservations.stream().anyMatch(r->r.orderSequence()==sequence))
            throw new WorldCommandException(INVALID_ORDER,"The sell order identifier is invalid or already reserved.");
        int available=availableShares(user,option);
        if(quantity<=0||quantity>available)throw new WorldCommandException(INVALID_QUANTITY,
                "Cannot reserve that sell quantity. Available shares: "+available+".");
        var next=new java.util.ArrayList<>(sellReservations);
        next.add(new OrderBookSellReservation(sequence,user,option,quantity));
        return new OrderBookState(issuedPairs,holdings,next,ledger);
    }
    OrderBookState fillSell(long sequence,String buyer,int quantity) throws WorldCommandException {
        var reservation=sellReservations.stream().filter(r->r.orderSequence()==sequence).findFirst()
                .orElseThrow(()->new WorldCommandException(INVALID_ORDER,"The sell reservation does not exist."));
        if(reservation.userName().equals(buyer))
            throw new WorldCommandException(SELF_MATCH_REJECTED,"A new order cannot trade against your own resting order.");
        if(quantity<=0||quantity>reservation.quantity())
            throw new WorldCommandException(INVALID_QUANTITY,"Fill quantity exceeds the reserved sell quantity.");
        var nextHoldings=new java.util.LinkedHashMap<String,OrderBookHolding>();
        for(var holding:holdings)nextHoldings.put(holding.userName(),holding);
        var seller=nextHoldings.get(reservation.userName());
        var target=nextHoldings.getOrDefault(buyer,new OrderBookHolding(buyer,0,0));
        boolean one=reservation.optionNumber()==1;
        nextHoldings.put(seller.userName(),new OrderBookHolding(seller.userName(),
                seller.optionOneShares()-(one?quantity:0),seller.optionTwoShares()-(one?0:quantity)));
        nextHoldings.put(buyer,new OrderBookHolding(buyer,
                target.optionOneShares()+(one?quantity:0),target.optionTwoShares()+(one?0:quantity)));
        var nextReservations=new java.util.ArrayList<OrderBookSellReservation>();
        for(var entry:sellReservations){
            if(entry.orderSequence()!=sequence)nextReservations.add(entry);
            else if(quantity<entry.quantity())nextReservations.add(new OrderBookSellReservation(
                    sequence,entry.userName(),entry.optionNumber(),entry.quantity()-quantity));
        }
        return new OrderBookState(issuedPairs,List.copyOf(nextHoldings.values()),nextReservations,ledger);
    }
    OrderBookState releaseAllReservations(){return new OrderBookState(issuedPairs,holdings,List.of(),ledger);}
    OrderBookState withLedger(OrderBookLedger next){return new OrderBookState(issuedPairs,holdings,sellReservations,next);}
    OrderBookState mintPairs(String one,String two,int quantity){
        int pairs=Math.addExact(issuedPairs,quantity);
        var next=new java.util.LinkedHashMap<String,OrderBookHolding>();
        for(var h:holdings)next.put(h.userName(),h);
        var a=next.getOrDefault(one,new OrderBookHolding(one,0,0));
        next.put(one,new OrderBookHolding(one,Math.addExact(a.optionOneShares(),quantity),a.optionTwoShares()));
        var b=next.getOrDefault(two,new OrderBookHolding(two,0,0));
        next.put(two,new OrderBookHolding(two,b.optionOneShares(),Math.addExact(b.optionTwoShares(),quantity)));
        return new OrderBookState(pairs,List.copyOf(next.values()),sellReservations,ledger);
    }
    OrderBookState cancel(java.util.Set<String> names){
        var nextLedger=new OrderBookLedger(ledger.orders().stream().filter(o->!names.contains(o.userName())).toList(),
                ledger.trades(),ledger.flows(),ledger.participants(),ledger.settlement());
        return new OrderBookState(issuedPairs,holdings,sellReservations.stream().filter(s->!names.contains(s.userName())).toList(),nextLedger);
    }
    OrderBookSnapshot snapshot(){return snapshot(1);}
    OrderBookSnapshot snapshot(int d){return ledger.snapshot(this,d);}
}
