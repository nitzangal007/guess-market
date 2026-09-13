package guessmarket.engine;

import guessmarket.dto.world.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import static guessmarket.engine.WorldCommandException.Code.*;

public final class MarketWorld {
    private final Map<Integer, WorldEvent> events;
    private final Map<String, MarketUser> users;

    public MarketWorld(Map<Integer, WorldEvent> events, Map<String, MarketUser> users) {
        this.events = Collections.unmodifiableMap(new LinkedHashMap<>(events));
        this.users = Collections.unmodifiableMap(new LinkedHashMap<>(users));
    }
    OrderBookOperations.Candidate planOrder(OrderRequest request,long revision)throws WorldCommandException {
        return new OrderBookOperations(events,users).order(request,revision);
    }
    ClosePreview previewOrderBookClose(String actor,int eventId,int winner,long revision)throws WorldCommandException {
        return new OrderBookOperations(events,users).previewClose(actor,eventId,winner,revision);
    }
    MarketWorld closeOrderBookEvent(String actor,int eventId,int winner,long revision)throws WorldCommandException {
        return new OrderBookOperations(events,users).close(actor,eventId,winner,revision);
    }
    OpeningPreview previewOrderBookOpening(String actingUser,int eventId,long revision)
            throws WorldCommandException {
        MarketUser user=users.get(actingUser);
        if(user==null)throw new WorldCommandException(USER_NOT_FOUND,"The acting user does not exist.");
        requireUnblocked(user);
        WorldEvent event=events.get(eventId);
        if(event==null)throw new WorldCommandException(EVENT_NOT_FOUND,"The event does not exist.");
        if(!event.marketMakerName().equals(actingUser))
            throw new WorldCommandException(NOT_OWNER,"Only this event's market maker may open it.");
        if(!(event.pricing() instanceof OrderBookConfiguration book))
            throw new WorldCommandException(WRONG_METHOD,"This opening flow supports Order Book events only.");
        if(event.status()!=WorldEventStatus.NOT_STARTED)
            throw new WorldCommandException(WRONG_STATUS,"This event has already been opened or closed.");
        if(book.initial()%book.d()!=0)
            throw new WorldCommandException(INVALID_QUANTITY,
                    "Opening requires whole pairs. Initial funding must be a multiple of "+book.d()+".");
        if(user.currentBalance()<book.initial())
            throw new WorldCommandException(INSUFFICIENT_FUNDS,"The market maker has insufficient funds to open this event.");
        double balanceAfter=representedDebit(user.currentBalance(),book.initial());
        return new OpeningPreview(revision,eventId,event.name(),actingUser,user.currentBalance(),
                book.initial(),balanceAfter);
    }
    MarketWorld openOrderBookEvent(String actingUser,int eventId) throws WorldCommandException {
        OpeningPreview preview=previewOrderBookOpening(actingUser,eventId,0);
        MarketUser user=users.get(actingUser);
        WorldEvent event=events.get(eventId);
        var book=(OrderBookConfiguration)event.pricing();
        var nextUsers=new LinkedHashMap<>(users);
        var nextEvents=new LinkedHashMap<>(events);
        nextUsers.put(actingUser,new MarketUser(actingUser,preview.balanceAfterOpening(),user.ownedEventIds(),user.blocked()));
        nextEvents.put(eventId,new WorldEvent(event.id(),event.name(),event.description(),event.options(),
                event.commission(),event.commissionMode(),event.marketMakerName(),event.pricing(),
                WorldEventStatus.ACTIVE,preview.requiredFunding(),null,
                OrderBookState.opened(actingUser,book.initial()/book.d()).withLedger(new OrderBookLedger(
                    java.util.List.of(),java.util.List.of(),java.util.List.of(new OrderBookLedger.Flow(actingUser,0,
                    OrderBookLedger.Kind.FUNDING,java.math.BigDecimal.valueOf(book.initial()))),
                    java.util.List.of(actingUser),java.util.Optional.empty()))));
        return new MarketWorld(nextEvents,nextUsers);
    }
    OpeningPreview previewLmsrOpening(String actingUser, int eventId, long revision)
            throws WorldCommandException {
        MarketUser user = users.get(actingUser);
        if (user == null) throw new WorldCommandException(USER_NOT_FOUND, "The acting user does not exist.");
        requireUnblocked(user);
        WorldEvent event = events.get(eventId);
        if (event == null) throw new WorldCommandException(EVENT_NOT_FOUND, "The event does not exist.");
        if (!event.marketMakerName().equals(actingUser))
            throw new WorldCommandException(NOT_OWNER, "Only this event's market maker may open it.");
        if (!(event.pricing() instanceof LmsrConfiguration lmsr))
            throw new WorldCommandException(WRONG_METHOD, "Order Book opening is not available yet.");
        if (event.status() != WorldEventStatus.NOT_STARTED)
            throw new WorldCommandException(WRONG_STATUS, "This event has already been opened or closed.");
        double funding = LmsrCalculator.totalCost(0, 0, lmsr.b());
        if (user.currentBalance() < funding)
            throw new WorldCommandException(INSUFFICIENT_FUNDS, "The market maker has insufficient funds to open this event.");
        return new OpeningPreview(revision, eventId, event.name(), actingUser,
                user.currentBalance(), funding, representedDebit(user.currentBalance(),funding));
    }
    MarketWorld openLmsrEvent(String actingUser, int eventId) throws WorldCommandException {
        OpeningPreview preview = previewLmsrOpening(actingUser, eventId, 0);
        MarketUser user = users.get(actingUser);
        WorldEvent event = events.get(eventId);
        var nextUsers = new LinkedHashMap<>(users);
        var nextEvents = new LinkedHashMap<>(events);
        nextUsers.put(actingUser, new MarketUser(actingUser, preview.balanceAfterOpening(), user.ownedEventIds(), user.blocked()));
        nextEvents.put(eventId, new WorldEvent(event.id(), event.name(), event.description(), event.options(),
                event.commission(), event.commissionMode(), event.marketMakerName(), event.pricing(),
                WorldEventStatus.ACTIVE, event.contractBalance() + preview.requiredFunding()));
        return new MarketWorld(nextEvents, nextUsers);
    }
    PurchasePreview previewLmsrPurchase(String actingUser, int eventId, int option, int quantity, long revision)
            throws WorldCommandException {
        MarketUser buyer = users.get(actingUser);
        if (buyer == null) throw new WorldCommandException(USER_NOT_FOUND, "The acting user does not exist.");
        requireUnblocked(buyer);
        WorldEvent event = events.get(eventId);
        if (event == null) throw new WorldCommandException(EVENT_NOT_FOUND, "The event does not exist.");
        if (!(event.pricing() instanceof LmsrConfiguration lmsr))
            throw new WorldCommandException(WRONG_METHOD, "This purchase flow supports LMSR events only.");
        if (event.status() != WorldEventStatus.ACTIVE)
            throw new WorldCommandException(WRONG_STATUS, "Only active events accept purchases.");
        if (option != 1 && option != 2)
            throw new WorldCommandException(INVALID_OPTION, "Select option 1 or 2.");
        if (quantity <= 0) throw new WorldCommandException(INVALID_QUANTITY, "Share quantity must be positive.");
        var trading = event.trading();
        int selected = option == 1 ? trading.quantityOne() : trading.quantityTwo();
        int other = option == 1 ? trading.quantityTwo() : trading.quantityOne();
        try { Math.addExact(selected,quantity); }
        catch (ArithmeticException failure) {
            throw new WorldCommandException(INVALID_QUANTITY, "This purchase would overflow the share quantity.");
        }
        try {
            double base = LmsrCalculator.purchaseCost(selected,other,quantity,lmsr.b());
            double fee = new CommissionPolicy(event.commissionMode(),event.commission()).purchaseCommission(base);
            if (fee < 0 || (event.commissionMode()==guessmarket.dto.CommissionMode.ON_PURCHASE
                    && event.commission()>0 && fee<=0)) throw new ArithmeticException("Commission underflow");
            double total = finite(base+fee);
            boolean self = actingUser.equals(event.marketMakerName());
            double receipt = self ? fee : 0;
            double afterDebit = representedDebit(buyer.currentBalance(),total);
            double after = finite(afterDebit + receipt);
            // User revision September 11: gross debit blocks, then a positive-cash receipt restores access.
            boolean becomesBlocked = afterDebit < 0 && !(receipt > 0 && after > 0);
            finite(event.contractBalance()+base);
            finite(trading.totalCommission()+fee);
            if (!self) finite(users.get(event.marketMakerName()).currentBalance()+fee);
            int nextSelected = selected+quantity;
            LmsrCalculator.priceForOption(nextSelected,other,lmsr.b());
            LmsrCalculator.priceForOption(other,nextSelected,lmsr.b());
            return new PurchasePreview(revision,actingUser,eventId,event.name(),option,event.options().get(option-1),
                    quantity,base,fee,total,receipt,buyer.currentBalance(),after,becomesBlocked);
        } catch (ArithmeticException | IllegalArgumentException failure) {
            throw new WorldCommandException(FINANCIAL_CALCULATION_FAILED,
                    "This purchase cannot be represented safely. Choose a different quantity.");
        }
    }
    MarketWorld purchaseLmsrShares(String actingUser, int eventId, int option, int quantity)
            throws WorldCommandException {
        PurchasePreview preview = previewLmsrPurchase(actingUser,eventId,option,quantity,0);
        MarketUser buyer=users.get(actingUser);
        WorldEvent event=events.get(eventId);
        var nextUsers=new LinkedHashMap<>(users);
        double afterDebit=buyer.currentBalance()-preview.totalDebit();
        nextUsers.put(actingUser,new MarketUser(actingUser,afterDebit,buyer.ownedEventIds(),
                buyer.blocked()||afterDebit<0).received(preview.commissionReceived()));
        if (!actingUser.equals(event.marketMakerName())) {
            MarketUser owner=users.get(event.marketMakerName());
            nextUsers.put(owner.name(),owner.received(preview.commission()));
        }
        var nextEvents=new LinkedHashMap<>(events);
        nextEvents.put(eventId,new WorldEvent(event.id(),event.name(),event.description(),event.options(),
                event.commission(),event.commissionMode(),event.marketMakerName(),event.pricing(),event.status(),
                event.contractBalance()+preview.shareCost(),
                event.trading().purchased(actingUser,option,quantity,preview.shareCost(),preview.commission())));
        OrderBookOperations.cancelBlocked(nextEvents,nextUsers);
        return new MarketWorld(nextEvents,nextUsers);
    }
    ClosePreview previewLmsrClose(String actingUser,int eventId,int winner,long revision) throws WorldCommandException {
        MarketUser owner=users.get(actingUser);
        if (owner==null) throw new WorldCommandException(USER_NOT_FOUND,"The acting user does not exist.");
        // User-approved narrow exception: a blocked MM may still settle their own event.
        WorldEvent event=events.get(eventId);
        if (event==null) throw new WorldCommandException(EVENT_NOT_FOUND,"The event does not exist.");
        if (!event.marketMakerName().equals(actingUser))
            throw new WorldCommandException(NOT_OWNER,"Only this event's market maker may close it.");
        if (!(event.pricing() instanceof LmsrConfiguration))
            throw new WorldCommandException(WRONG_METHOD,"This closing flow supports LMSR only.");
        if (event.status()!=WorldEventStatus.ACTIVE)
            throw new WorldCommandException(WRONG_STATUS,"Only an active event can be closed.");
        if (winner!=1 && winner!=2) throw new WorldCommandException(INVALID_OPTION,"Choose a winning option.");
        var holdings=new LinkedHashMap<String,Integer>();
        for (var trade:event.trading().history()) if (trade.optionNumber()==winner)
            holdings.merge(trade.userName(),trade.quantity(),Math::addExact);
        var payments=new java.util.ArrayList<SettlementPayment>();
        double grossTotal=0,feeTotal=0;
        try {
            CommissionPolicy policy=new CommissionPolicy(event.commissionMode(),event.commission());
            for (var entry:holdings.entrySet()) {
                double gross=entry.getValue();
                double fee=finite(policy.closingCommission(gross));
                double net=finite(gross-fee);
                payments.add(new SettlementPayment(entry.getKey(),entry.getValue(),gross,fee,net));
                grossTotal=finite(grossTotal+gross);
                feeTotal=finite(feeTotal+fee);
            }
            double refund=settlementRefund(event,grossTotal);
            var deltas=new LinkedHashMap<String,Double>();
            for (var payment:payments) deltas.merge(payment.userName(),payment.netPayout(),Double::sum);
            deltas.merge(actingUser,feeTotal+refund,Double::sum);
            for(var delta:deltas.entrySet()) finite(users.get(delta.getKey()).currentBalance()+delta.getValue());
            return new ClosePreview(revision,eventId,event.name(),actingUser,winner,event.options().get(winner-1),
                    payments,grossTotal,feeTotal,refund);
        } catch(ArithmeticException | IllegalArgumentException failure) {
            throw new WorldCommandException(FINANCIAL_CALCULATION_FAILED,
                    "The settlement cannot be represented safely. No balances or status were changed.");
        }
    }
    MarketWorld closeLmsrEvent(String actingUser,int eventId,int winner) throws WorldCommandException {
        return closeLmsrEvent(actingUser,eventId,winner,0);
    }
    MarketWorld closeLmsrEvent(String actingUser,int eventId,int winner,long revision) throws WorldCommandException {
        ClosePreview preview=previewLmsrClose(actingUser,eventId,winner,revision);
        var deltas=new LinkedHashMap<String,Double>();
        for(var payment:preview.payments()) deltas.merge(payment.userName(),payment.netPayout(),Double::sum);
        deltas.merge(actingUser,preview.totalCommission()+preview.subsidyRefund(),Double::sum);
        var nextUsers=new LinkedHashMap<>(users);
        for(var delta:deltas.entrySet()) {
            MarketUser user=users.get(delta.getKey());
            nextUsers.put(user.name(),user.received(delta.getValue()));
        }
        WorldEvent event=events.get(eventId);
        var trading=event.trading();
        var settled=new LmsrTradingState(trading.quantityOne(),trading.quantityTwo(),trading.totalCommission(),
                trading.history(),java.util.Optional.of(preview));
        var nextEvents=new LinkedHashMap<>(events);
        nextEvents.put(eventId,new WorldEvent(event.id(),event.name(),event.description(),event.options(),
                event.commission(),event.commissionMode(),event.marketMakerName(),event.pricing(),
                WorldEventStatus.CLOSED,0,settled));
        return new MarketWorld(nextEvents,nextUsers);
    }
    private static double settlementRefund(WorldEvent event,double grossPayout) {
        double refund=finite(event.contractBalance()-grossPayout);
        if(refund>=0)return refund;
        var proof=LmsrSettlementRounding.verify(event);
        var deficit=new java.math.BigDecimal(grossPayout).subtract(new java.math.BigDecimal(event.contractBalance()));
        if(deficit.compareTo(proof.maximumAdjustment())>0)
            throw new ArithmeticException("Shortfall exceeds the trace's numerical error bound");
        // Reconcile only the demonstrated shortfall. Fixed winnings and fees are unchanged.
        return 0;
    }
    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite account result");
        return value;
    }
    private static double representedDebit(double balance,double funding) throws WorldCommandException {
        double after=finite(balance-funding);
        if(funding>0&&Double.compare(after,balance)==0)
            throw new WorldCommandException(FINANCIAL_CALCULATION_FAILED,
                    "The debit cannot be represented against this account balance. No cash or shares were transferred.");
        return after;
    }
    public WorldSnapshot snapshot() {
        return new WorldSnapshot(events.values().stream().map(WorldEvent::snapshot).toList(),
                users.values().stream().map(this::userSnapshot).toList());
    }
    private static void requireUnblocked(MarketUser user) throws WorldCommandException {
        if (user.blocked()) throw new WorldCommandException(USER_BLOCKED,
                "This user is blocked after a purchase made their balance negative. Inspection is still available.");
    }
    private UserSnapshot userSnapshot(MarketUser user) {
        var positions = new java.util.ArrayList<UserEventPosition>();
        for (WorldEvent event : events.values()) {
            if(event.orderBook()!=null){
                var book=event.orderBook().snapshot(((OrderBookConfiguration)event.pricing()).d());
                if(book.participants().contains(user.name())){
                    var position=book.positions().stream().filter(p->p.userName().equals(user.name())).findFirst().orElseThrow();
                    double paid=position.purchases().add(position.funding()).doubleValue();
                    double fee=position.commissionsPaid().doubleValue();
                    positions.add(new UserEventPosition(event.id(),position.optionOneShares(),position.optionTwoShares(),paid,fee,paid+fee));
                }
            }
            if (event.trading() == null) continue;
            int one=0, two=0; double base=0, fee=0;
            for (var trade : event.trading().history()) {
                if (!trade.userName().equals(user.name())) continue;
                if (trade.optionNumber()==1) one=Math.addExact(one,trade.quantity());
                else two=Math.addExact(two,trade.quantity());
                base+=trade.shareCost(); fee+=trade.commission();
            }
            if (one!=0 || two!=0) positions.add(new UserEventPosition(event.id(),one,two,base,fee,base+fee));
        }
        var active = positions.stream().filter(position -> events.get(position.eventId()).status()==WorldEventStatus.ACTIVE)
                .map(UserEventPosition::eventId).toList();
        return new UserSnapshot(user.name(),user.currentBalance(),user.ownedEventIds(),active,user.blocked(),positions);
    }
}
