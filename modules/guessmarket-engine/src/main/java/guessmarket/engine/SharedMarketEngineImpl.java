package guessmarket.engine;
import guessmarket.dto.api.*;
import java.util.*;
import java.math.BigDecimal;
import guessmarket.dto.world.*;
public final class SharedMarketEngineImpl implements SharedMarketEngine {
    private final String instance;
    private MarketWorld world=MarketWorld.empty();
    private AccountJournal journal=new AccountJournal();
    private long revision;
    private int nextEventId=1;
    public SharedMarketEngineImpl(String instance) {this.instance=Objects.requireNonNull(instance);}
    public long revision(){return revision;}
    public ActionPreviewDto preview(String actor,int eventId,ActionRequest action)throws WorldCommandException {return plan(actor,eventId,action).preview();}
    public CommandResponse execute(String actor,int eventId,CommandRequest request)throws WorldCommandException {
        if(request.expectedRevision()!=revision)throw new WorldCommandException(WorldCommandException.Code.STALE_WORLD,"The market changed after this review. Review again before confirming.");
        var candidate=plan(actor,eventId,request.action());
        try{
            long next=Math.incrementExact(revision);String transactionId=instance+":"+next;
            var nextJournal=journal.prepare(candidate.transition().accountEffects(),transactionId);
            var after=candidate.transition().world().users().get(actor);var before=account(actor);
            var receipt=new CommandResponse(instance,next,request.requestId(),OperationKind.valueOf(request.action().action().name()),eventId,request.action(),null,candidate.preview(),Double.toString(before.currentBalance()),Double.toString(after.currentBalance()),after.blocked(),newRows(nextJournal,actor),"Command completed.");
            candidate.transition().world().snapshot();world=candidate.transition().world();journal=nextJournal;revision=next;return receipt;
        }catch(ArithmeticException problem){throw new WorldCommandException(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,"This transaction cannot prepare its complete journal. Nothing changed.");}
    }
    private record Planned(WorldTransition transition,ActionPreviewDto preview) {}
    private Planned plan(String actor,int eventId,ActionRequest action)throws WorldCommandException {
        var before=account(actor);var event=requireEvent(eventId);
        if(action==null||action.action()==null)throw new WorldCommandException(WorldCommandException.Code.INVALID_ORDER,"Choose an action.");
        MarketWorld next;var components=new ArrayList<CashComponent>();var executions=List.<ExecutionDto>of();var payouts=List.<PayoutDto>of();
        Integer remainder=null,cancelled=null,released=null;String gross=null,fee=null,refund=null,winningLabel=null;
        switch(action.action()){
            case OPEN->{
                var p=event.pricing() instanceof LmsrConfiguration?world.previewLmsrOpening(actor,eventId,revision):world.previewOrderBookOpening(actor,eventId,revision);
                next=event.pricing() instanceof LmsrConfiguration?world.openLmsrEvent(actor,eventId):world.openOrderBookEvent(actor,eventId);
                components.add(component(actor,"OPENING_FUNDING",event,null,BigDecimal.valueOf(p.requiredFunding()).negate()));
            }
            case LMSR_BUY->{
                requireInputs(action.optionNumber(),action.quantity());var p=world.previewLmsrPurchase(actor,eventId,action.optionNumber(),action.quantity(),revision);
                next=world.purchaseLmsrShares(actor,eventId,action.optionNumber(),action.quantity());
                components.add(component(actor,"SHARE_PURCHASE",event,action.optionNumber(),BigDecimal.valueOf(p.shareCost()).negate()));
                components.add(component(actor,"COMMISSION_PAID",event,action.optionNumber(),BigDecimal.valueOf(p.commission()).negate()));
                components.add(component(event.marketMakerName(),"COMMISSION_RECEIVED",event,action.optionNumber(),BigDecimal.valueOf(p.commission())));
            }
            case ORDER->{
                requireInputs(action.optionNumber(),action.quantity());BigDecimal limit;try{limit=new BigDecimal(action.limitPrice());}catch(RuntimeException problem){throw new WorldCommandException(WorldCommandException.Code.INVALID_ORDER,"Enter a decimal limit price.");}
                var p=world.planOrder(new OrderRequest(actor,eventId,action.optionNumber(),action.side(),action.quantity(),limit),revision);next=p.world();
                executions=p.preview().fills().stream().map(ApiSnapshotMapper::execution).toList();remainder=p.preview().incomingRemaining();cancelled=p.preview().cancelledOrderCount();released=p.preview().releasedReservationQuantity();
                observeBookFlows(event,next.events().get(eventId),components);
            }
            case CLOSE->{
                requireInputs(action.optionNumber(),1);var p=event.pricing() instanceof LmsrConfiguration?world.previewLmsrClose(actor,eventId,action.optionNumber(),revision):world.previewOrderBookClose(actor,eventId,action.optionNumber(),revision);
                next=event.pricing() instanceof LmsrConfiguration?world.closeLmsrEvent(actor,eventId,action.optionNumber(),revision):world.closeOrderBookEvent(actor,eventId,action.optionNumber(),revision);
                payouts=p.payments().stream().map(ApiSnapshotMapper::payout).toList();gross=Double.toString(p.totalGrossPayout());fee=Double.toString(p.totalCommission());refund=Double.toString(p.subsidyRefund());winningLabel=p.winningLabel();
                if(event.pricing() instanceof OrderBookConfiguration)observeBookFlows(event,next.events().get(eventId),components);
                else{
                    for(var payment:p.payments()){
                        components.add(component(payment.userName(),"WINNING_GROSS",event,action.optionNumber(),BigDecimal.valueOf(payment.grossPayout())));
                        components.add(component(payment.userName(),"COMMISSION_PAID",event,action.optionNumber(),BigDecimal.valueOf(payment.commission()).negate()));
                    }
                    components.add(component(actor,"COMMISSION_RECEIVED",event,action.optionNumber(),BigDecimal.valueOf(p.totalCommission())));
                    components.add(component(actor,"SUBSIDY_REFUND",event,action.optionNumber(),BigDecimal.valueOf(p.subsidyRefund())));
                }
            }
            default->throw new IllegalStateException("Unknown action");
        }
        var effects=new ArrayList<AccountCashEffect>();var affected=new LinkedHashSet<String>();components.forEach(c->affected.add(c.username()));
        for(String name:affected)effects.add(new AccountCashEffect(name,world.users().get(name).currentBalance(),next.users().get(name).currentBalance(),components.stream().filter(c->c.username().equals(name)).toList()));
        var own=effects.stream().filter(e->e.username().equals(actor)).findFirst().orElse(new AccountCashEffect(actor,before.currentBalance(),next.users().get(actor).currentBalance(),List.of()));
        var debit=own.components().stream().map(CashComponent::signedAmount).filter(v->v.signum()<0).reduce(BigDecimal.ZERO,BigDecimal::add).negate();var credit=own.components().stream().map(CashComponent::signedAmount).filter(v->v.signum()>0).reduce(BigDecimal.ZERO,BigDecimal::add);
        var after=next.users().get(actor);var preview=new ActionPreviewDto(instance,revision,eventId,event.name(),actor,winningLabel,action,debit.toPlainString(),credit.toPlainString(),Double.toString(before.currentBalance()),Double.toString(after.currentBalance()),after.blocked(),AccountJournal.explain(own),executions,remainder,cancelled,released,payouts,gross,fee,refund,"Review the server-calculated effects before confirming.");
        return new Planned(new WorldTransition(next,effects),preview);
    }
    private static CashComponent component(String actor,String kind,WorldEvent event,Integer option,BigDecimal amount){return new CashComponent(actor,kind,event.id(),event.name(),option,amount);}
    private static void requireInputs(Integer option,Integer quantity)throws WorldCommandException {if(option==null||quantity==null)throw new WorldCommandException(WorldCommandException.Code.INVALID_ORDER,"Option and quantity are required.");}
    private static void observeBookFlows(WorldEvent before,WorldEvent after,List<CashComponent> components){
        var prior=before.orderBook().ledger().flows();var current=after.orderBook().ledger().flows();
        for(var flow:current.subList(prior.size(),current.size())){
            String kind=switch(flow.kind()){case FUNDING->"OPENING_FUNDING";case PURCHASE->"SHARE_PURCHASE";case SALE->"SALE_RECEIPT";case FEE_PAID->"COMMISSION_PAID";case FEE_RECEIVED->"COMMISSION_RECEIVED";case SETTLEMENT->"WINNING_GROSS";};
            boolean debit=flow.kind()==OrderBookLedger.Kind.FUNDING||flow.kind()==OrderBookLedger.Kind.PURCHASE||flow.kind()==OrderBookLedger.Kind.FEE_PAID;
            components.add(component(flow.user(),kind,before,flow.option()==0?null:flow.option(),debit?flow.amount().negate():flow.amount()));
        }
    }
    public guessmarket.dto.api.UploadResponse appendEvents(String actor,guessmarket.engine.xml.ex3.ParsedEventBatch batch,String requestId)throws WorldCommandException{
        account(actor);var names=new HashSet<String>();world.events().values().forEach(e->names.add(e.name()));
        var added=new ArrayList<WorldEvent>();int id=nextEventId;
        for(var definition:batch.events()){
            if(!names.add(definition.name()))throw new WorldCommandException(WorldCommandException.Code.DUPLICATE_EVENT_NAME,"An event named '"+definition.name()+"' already exists.");
            added.add(new WorldEvent(id,definition.name(),definition.description(),definition.options(),definition.commissionPercentage(),definition.commissionMode(),actor,definition.pricing()));
            id=Math.incrementExact(id);
        }
        if(added.isEmpty())throw new WorldCommandException(WorldCommandException.Code.INVALID_ORDER,"Upload at least one event.");
        var candidate=world.append(added,actor);candidate.snapshot();long next=Math.incrementExact(revision);
        var result=new UploadResponse(instance,next,requestId,OperationKind.UPLOAD,added.stream().map(e->new AddedEventDto(e.id(),e.name())).toList());
        world=candidate;revision=next;nextEventId=id;return result;
    }
    public void ensureAccount(String actor) throws WorldCommandException {
        if(world.users().containsKey(actor))return;
        long next=Math.incrementExact(revision);world=world.withAccount(actor);revision=next;
    }
    public DashboardResponse dashboard(String actor,Integer event) throws WorldCommandException {
        var user=account(actor);
        var snapshot=world.snapshot();
        var own=snapshot.users().stream().filter(u->u.name().equals(actor)).findFirst().orElseThrow();
        return new DashboardResponse(instance,revision,snapshot.events().stream().map(ApiSnapshotMapper::summary).toList(),
                snapshot.users().stream().map(u->new PublicUserDto(u.name(),Double.toString(u.currentBalance()),!u.ownedEventIds().isEmpty())).toList(),
                new AccountDto(actor,Double.toString(user.currentBalance()),user.blocked(),user.ownedEventIds(),own.positions().stream().map(UserEventPosition::eventId).toList(),journal.entries(actor)),
                event==null?null:ApiSnapshotMapper.detail(requireEvent(event).snapshot(),actor,user.blocked()));
    }
    public CommandResponse addFunds(String actor,String amount,String requestId) throws WorldCommandException {
        double value;
        try{value=new BigDecimal(amount).doubleValue();}catch(RuntimeException failure){throw new WorldCommandException(WorldCommandException.Code.INVALID_ORDER,"Enter a positive finite amount.");}
        var before=account(actor);var next=world.credit(actor,value);var after=next.users().get(actor);
        var component=new CashComponent(actor,"FUND_LOAD",null,null,null,BigDecimal.valueOf(value));
        long nextRevision=Math.incrementExact(revision);
        AccountJournal candidate;
        try{candidate=journal.prepare(List.of(new AccountCashEffect(actor,before.currentBalance(),after.currentBalance(),List.of(component))),instance+":"+nextRevision);}
        catch(ArithmeticException problem){throw new WorldCommandException(WorldCommandException.Code.FINANCIAL_CALCULATION_FAILED,"This credit cannot prepare its complete journal. Nothing changed.");}
        var receipt=new CommandResponse(instance,nextRevision,requestId,OperationKind.FUND_LOAD,null,null,Double.toString(value),null,
                Double.toString(before.currentBalance()),Double.toString(after.currentBalance()),after.blocked(),newRows(candidate,actor),"Funds loaded.");
        world=next;journal=candidate;revision=nextRevision;return receipt;
    }
    private MarketUser account(String actor)throws WorldCommandException{
        var user=world.users().get(actor);if(user==null)throw new WorldCommandException(WorldCommandException.Code.USER_NOT_FOUND,"The account does not exist.");return user;
    }
    private List<AccountEntryDto> newRows(AccountJournal candidate,String actor){return List.copyOf(candidate.entries(actor).subList(journal.entries(actor).size(),candidate.entries(actor).size()));}
    private WorldEvent requireEvent(int id)throws WorldCommandException{
        var event=world.events().get(id);if(event==null)throw new WorldCommandException(WorldCommandException.Code.EVENT_NOT_FOUND,"The event does not exist.");return event;
    }
}
