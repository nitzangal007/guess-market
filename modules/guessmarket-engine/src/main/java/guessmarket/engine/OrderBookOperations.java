package guessmarket.engine;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import java.math.BigDecimal;
import java.util.*;
import static guessmarket.engine.WorldCommandException.Code.*;
import static guessmarket.engine.OrderBookLedger.Kind.*;

/** A command builds a private candidate; the Engine publishes it only after its snapshot succeeds. */
final class OrderBookOperations {
    record Candidate(MarketWorld world,OrderPreview preview) {}
    private final Map<Integer,WorldEvent> events;
    private final Map<String,MarketUser> users;
    private final Map<String,Account> accounts=new LinkedHashMap<>();
    private static final class Account {
        final MarketUser original;
        BigDecimal balance;
        boolean blocked;
        Account(MarketUser user){original=user;balance=BigDecimal.valueOf(user.currentBalance());blocked=user.blocked();}
        void debit(BigDecimal value){cash(value);balance=balance.subtract(value);blocked|=balance.signum()<0;}
        void receive(BigDecimal value){cash(value);balance=balance.add(value);if(value.signum()>0&&balance.signum()>0)blocked=false;}
        MarketUser result(){
            double converted=cash(balance);
            if(balance.compareTo(BigDecimal.valueOf(original.currentBalance()))!=0
                    &&Double.compare(converted,original.currentBalance())==0)
                throw new ArithmeticException("The net account change is below the cash representation");
            return new MarketUser(original.name(),converted,original.ownedEventIds(),blocked);
        }
    }
    OrderBookOperations(Map<Integer,WorldEvent> events,Map<String,MarketUser> users){
        this.events=new LinkedHashMap<>(events);this.users=new LinkedHashMap<>(users);
        users.forEach((name,user)->accounts.put(name,new Account(user)));
    }
    Candidate order(OrderRequest request,long revision)throws WorldCommandException{
        if(request==null)throw new WorldCommandException(INVALID_ORDER,"An order is required.");
        var actor=users.get(request.userName());
        if(actor==null)throw new WorldCommandException(USER_NOT_FOUND,"The acting user does not exist.");
        if(actor.blocked())throw new WorldCommandException(USER_BLOCKED,"This user is blocked. Inspection and owner settlement remain available.");
        var event=active(request.eventId());
        var config=(OrderBookConfiguration)event.pricing();
        if(request.optionNumber()!=1&&request.optionNumber()!=2)throw new WorldCommandException(INVALID_OPTION,"Choose option 1 or 2.");
        if(request.side()==null)throw new WorldCommandException(INVALID_ORDER,"Choose BUY or SELL.");
        if(request.quantity()<=0)throw new WorldCommandException(INVALID_QUANTITY,"Quantity must be a positive whole number.");
        BigDecimal limit=request.limitPrice();
        if(limit==null||limit.signum()<0||limit.compareTo(BigDecimal.valueOf(config.d()).subtract(new BigDecimal("0.01")))>0)
            throw new WorldCommandException(INVALID_ORDER,"Price must be from 0 through "+BigDecimal.valueOf(config.d()).subtract(new BigDecimal("0.01"))+".");
        try {
            cash(limit);
            long sequence=Math.incrementExact(revision);
            var state=event.orderBook();
            if(request.side()==OrderSide.SELL)state=state.reserveSell(sequence,request.userName(),request.optionNumber(),request.quantity());
            var original=state.ledger();
            var orders=new LinkedHashMap<Long,OrderBookOrder>();original.orders().forEach(o->orders.put(o.sequence(),o));
            var same=original.orders().stream().filter(o->o.optionNumber()==request.optionNumber()&&o.side()!=request.side()).map(OrderBookOperations::submitted).toList();
            var opposite=original.orders().stream().filter(o->o.optionNumber()!=request.optionNumber()&&o.side()==OrderSide.BUY).map(OrderBookOperations::submitted).toList();
            var plan=OrderBookMatcher.matchThenMint(OrderBookMatcher.Side.valueOf(request.side().name()),
                    new OrderBookMatcher.Quote(request.quantity(),limit),same,opposite,config.d(),config.allowMint());
            var used=new ArrayList<Long>();plan.ordinary().fills().forEach(f->used.add(f.sequence()));plan.mints().forEach(m->used.add(m.sequence()));
            if(used.stream().anyMatch(id->orders.get(id).userName().equals(request.userName())))
                throw new WorldCommandException(SELF_MATCH_REJECTED,"This order would trade or mint against your own order. The entire new order was rejected.");
            var fills=new ArrayList<OrderBookTrade>();
            var flows=new ArrayList<>(original.flows());
            for(var fill:plan.ordinary().fills()){
                var resting=orders.get(fill.sequence());
                String buyer=request.side()==OrderSide.BUY?request.userName():resting.userName();
                String seller=request.side()==OrderSide.SELL?request.userName():resting.userName();
                long sellSequence=request.side()==OrderSide.SELL?sequence:resting.sequence();
                state=state.fillSell(sellSequence,buyer,fill.quantity());
                trade(event,buyer,seller,request.optionNumber(),fill.quantity(),fill.price(),false,fills,flows,original.trades().size());
                reduce(orders,resting,fill.quantity());
            }
            for(var mint:plan.mints()){
                var resting=orders.get(mint.sequence());var fill=mint.fill();
                String one=request.optionNumber()==1?request.userName():resting.userName();
                String two=request.optionNumber()==2?request.userName():resting.userName();
                state=state.mintPairs(one,two,fill.quantity());
                trade(event,request.userName(),resting.userName(),request.optionNumber(),fill.quantity(),fill.incomingPrice(),true,fills,flows,original.trades().size());
                trade(event,resting.userName(),request.userName(),3-request.optionNumber(),fill.quantity(),fill.restingPrice(),true,fills,flows,original.trades().size());
                reduce(orders,resting,fill.quantity());
            }
            if(plan.incomingRemaining()>0)orders.put(sequence,new OrderBookOrder(sequence,request.userName(),request.optionNumber(),request.side(),plan.incomingRemaining(),limit));
            var history=new ArrayList<>(original.trades());history.addAll(fills);
            var participants=new LinkedHashSet<>(original.participants());participants.add(request.userName());
            state=state.withLedger(new OrderBookLedger(List.copyOf(orders.values()),history,flows,List.copyOf(participants),Optional.empty()));
            BigDecimal backing=BigDecimal.valueOf(state.issuedPairs()).multiply(BigDecimal.valueOf(config.d()));
            events.put(event.id(),replace(event,state,WorldEventStatus.ACTIVE,cash(backing)));
            accounts.forEach((name,account)->users.put(name,account.result()));
            int[] cancellations=cancelBlocked(events,users);
            var finalUser=users.get(request.userName());
            var preview=new OrderPreview(revision,request,fills,plan.incomingRemaining(),actor.currentBalance(),finalUser.currentBalance(),
                    finalUser.blocked(),cancellations[0],cancellations[1]);
            return new Candidate(new MarketWorld(events,users),preview);
        }catch(ArithmeticException|IllegalArgumentException failure){
            throw new WorldCommandException(FINANCIAL_CALCULATION_FAILED,"This order cannot be represented safely. No state was changed.");
        }
    }
    private void trade(WorldEvent event,String buyer,String counterparty,int option,int quantity,BigDecimal price,boolean mint,
                       List<OrderBookTrade> fills,List<OrderBookLedger.Flow> flows,int previousCount){
        BigDecimal principal=price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee=fee(event,principal,CommissionMode.ON_PURCHASE);
        accounts.get(buyer).debit(principal.add(fee));
        flows.add(new OrderBookLedger.Flow(buyer,option,PURCHASE,principal));
        flows.add(new OrderBookLedger.Flow(buyer,option,FEE_PAID,fee));
        if(!mint){
            accounts.get(counterparty).receive(principal);
            flows.add(new OrderBookLedger.Flow(counterparty,option,SALE,principal));
        }
        accounts.get(event.marketMakerName()).receive(fee);
        flows.add(new OrderBookLedger.Flow(event.marketMakerName(),option,FEE_RECEIVED,fee));
        fills.add(new OrderBookTrade((long)previousCount+fills.size()+1,buyer,counterparty,option,quantity,price,principal,fee,mint));
    }
    ClosePreview previewClose(String actor,int id,int winner,long revision)throws WorldCommandException{
        if(!users.containsKey(actor))throw new WorldCommandException(USER_NOT_FOUND,"The acting user does not exist.");
        var event=active(id);
        if(!event.marketMakerName().equals(actor))throw new WorldCommandException(NOT_OWNER,"Only this event's owner may settle it.");
        if(winner!=1&&winner!=2)throw new WorldCommandException(INVALID_OPTION,"Choose option 1 or 2.");
        try{
            int d=((OrderBookConfiguration)event.pricing()).d();
            var payments=new ArrayList<SettlementPayment>();BigDecimal grossTotal=BigDecimal.ZERO,feeTotal=BigDecimal.ZERO;
            for(var holding:event.orderBook().holdings()){
                int quantity=winner==1?holding.optionOneShares():holding.optionTwoShares();
                if(quantity==0)continue;
                BigDecimal gross=BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(d)),fee=fee(event,gross,CommissionMode.ON_CLOSE);
                payments.add(new SettlementPayment(holding.userName(),quantity,cash(gross),cash(fee),cash(gross.subtract(fee))));
                accounts.get(holding.userName()).receive(gross.subtract(fee));
                grossTotal=grossTotal.add(gross);feeTotal=feeTotal.add(fee);
            }
            // Backing is derived exactly from issued whole pairs, never forgiven with an epsilon.
            if(grossTotal.compareTo(BigDecimal.valueOf(event.orderBook().issuedPairs()).multiply(BigDecimal.valueOf(d)))!=0
                    ||Double.compare(cash(grossTotal),event.contractBalance())!=0)throw new ArithmeticException("Invalid contract backing");
            accounts.get(actor).receive(feeTotal);
            accounts.values().forEach(Account::result);
            return new ClosePreview(revision,id,event.name(),actor,winner,event.options().get(winner-1),payments,cash(grossTotal),cash(feeTotal),0);
        }catch(ArithmeticException|IllegalArgumentException failure){
            throw new WorldCommandException(FINANCIAL_CALCULATION_FAILED,"The settlement cannot be represented safely. No state was changed.");
        }
    }
    MarketWorld close(String actor,int id,int winner,long revision)throws WorldCommandException{
        var preview=previewClose(actor,id,winner,revision);
        var event=events.get(id);var state=event.orderBook();var ledger=state.ledger();
        var flows=new ArrayList<>(ledger.flows());
        for(var payment:preview.payments()){
            BigDecimal gross=BigDecimal.valueOf(payment.winningShares()).multiply(BigDecimal.valueOf(((OrderBookConfiguration)event.pricing()).d()));
            flows.add(new OrderBookLedger.Flow(payment.userName(),winner,SETTLEMENT,gross));
            flows.add(new OrderBookLedger.Flow(payment.userName(),winner,FEE_PAID,fee(event,gross,CommissionMode.ON_CLOSE)));
        }
        BigDecimal gross=BigDecimal.valueOf(state.issuedPairs()).multiply(BigDecimal.valueOf(((OrderBookConfiguration)event.pricing()).d()));
        flows.add(new OrderBookLedger.Flow(actor,winner,FEE_RECEIVED,fee(event,gross,CommissionMode.ON_CLOSE)));
        state=state.releaseAllReservations().withLedger(new OrderBookLedger(List.of(),ledger.trades(),flows,ledger.participants(),Optional.of(preview)));
        events.put(id,replace(event,state,WorldEventStatus.CLOSED,0));
        accounts.forEach((name,account)->users.put(name,account.result()));
        return new MarketWorld(events,users);
    }
    private WorldEvent active(int id)throws WorldCommandException{
        var event=events.get(id);
        if(event==null)throw new WorldCommandException(EVENT_NOT_FOUND,"The event does not exist.");
        if(!(event.pricing() instanceof OrderBookConfiguration))throw new WorldCommandException(WRONG_METHOD,"This operation requires an Order Book event.");
        if(event.status()!=WorldEventStatus.ACTIVE)throw new WorldCommandException(WRONG_STATUS,"Only active events accept this operation.");
        return event;
    }
    static int[] cancelBlocked(Map<Integer,WorldEvent> events,Map<String,MarketUser> users){
        var blocked=new HashSet<String>();users.values().stream().filter(MarketUser::blocked).forEach(u->blocked.add(u.name()));
        int count=0,released=0;
        for(var event:List.copyOf(events.values())){
            var book=event.orderBook();if(book==null)continue;
            count=Math.addExact(count,(int)book.ledger().orders().stream().filter(o->blocked.contains(o.userName())).count());
            for(var reservation:book.sellReservations())if(blocked.contains(reservation.userName()))released=Math.addExact(released,reservation.quantity());
            events.put(event.id(),replace(event,book.cancel(blocked),event.status(),event.contractBalance()));
        }
        return new int[]{count,released};
    }
    private static OrderBookMatcher.SubmittedOrder submitted(OrderBookOrder o){
        return new OrderBookMatcher.SubmittedOrder(o.sequence(),o.userName(),new OrderBookMatcher.Quote(o.quantity(),o.limitPrice()));
    }
    private static void reduce(Map<Long,OrderBookOrder> orders,OrderBookOrder order,int filled){
        if(filled==order.quantity())orders.remove(order.sequence());
        else orders.put(order.sequence(),new OrderBookOrder(order.sequence(),order.userName(),order.optionNumber(),order.side(),order.quantity()-filled,order.limitPrice()));
    }
    static WorldEvent replace(WorldEvent e,OrderBookState state,WorldEventStatus status,double balance){
        return new WorldEvent(e.id(),e.name(),e.description(),e.options(),e.commission(),e.commissionMode(),e.marketMakerName(),e.pricing(),status,balance,null,state);
    }
    private static BigDecimal fee(WorldEvent event,BigDecimal principal,CommissionMode mode){
        return event.commissionMode()==mode?principal.multiply(BigDecimal.valueOf(event.commission())).movePointLeft(2):BigDecimal.ZERO;
    }
    private static double cash(BigDecimal value){
        double result=value.doubleValue();
        if(!Double.isFinite(result)||(value.signum()!=0&&result==0))throw new ArithmeticException("Unrepresentable cash");
        return result;
    }
}
