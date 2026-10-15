package guessmarket.engine;
import guessmarket.dto.api.*;
import guessmarket.dto.world.*;
import java.util.*;
import java.math.BigDecimal;
final class ApiSnapshotMapper {
    static EventSummaryDto summary(EventSnapshot e){return new EventSummaryDto(e.id(),e.name(),e.description(),e.pricing() instanceof LmsrConfiguration?"LMSR":"ORDER_BOOK",e.status().name(),e.marketMakerName(),e.commissionPercentage(),e.commissionMode().name(),Double.toString(e.contractBalance()));}
    static EventDetailDto detail(EventSnapshot e,String actor,boolean blocked){
        LmsrDetailDto lmsr=null;BookDetailDto book=null;PositionDto position=null;SettlementDto settlement=null;
        if(e.pricing() instanceof LmsrConfiguration config){
            var t=e.lmsrTrading().orElseThrow();
            lmsr=new LmsrDetailDto(config.b(),List.of(new LmsrOptionDto(1,e.optionLabels().get(0),d(t.priceOne()),t.quantityOne()),new LmsrOptionDto(2,e.optionLabels().get(1),d(t.priceTwo()),t.quantityTwo())),d(t.totalPurchaseCommission()),t.newestFirstHistory().stream().map(p->new LmsrTradeDto(p.sequence(),p.userName(),p.optionNumber(),p.quantity(),d(p.shareCost()),d(p.commission()),d(p.totalPaid()))).toList());
            var own=t.newestFirstHistory().stream().filter(p->p.userName().equals(actor)).toList();
            int one=own.stream().filter(p->p.optionNumber()==1).mapToInt(PurchaseEntry::quantity).sum(),two=own.stream().filter(p->p.optionNumber()==2).mapToInt(PurchaseEntry::quantity).sum();
            double paidOne=own.stream().filter(p->p.optionNumber()==1).mapToDouble(PurchaseEntry::shareCost).sum(),paidTwo=own.stream().filter(p->p.optionNumber()==2).mapToDouble(PurchaseEntry::shareCost).sum(),fees=own.stream().mapToDouble(PurchaseEntry::commission).sum();
            double payout=t.settlement().stream().flatMap(p->p.payments().stream()).filter(p->p.userName().equals(actor)).mapToDouble(SettlementPayment::netPayout).sum();
            position=new PositionDto(one,two,0,0,one,two,d(paidOne),d(paidTwo),null,d(paidOne+paidTwo),null,d(fees),null,d(payout),null,e.status()==WorldEventStatus.ACTIVE?d(one*t.priceOne()):null,e.status()==WorldEventStatus.ACTIVE?d(two*t.priceTwo()):null);
            settlement=t.settlement().map(p->settlement(p,d(e.contractBalance()),d(t.totalPurchaseCommission()+p.totalCommission()))).orElse(null);
        }else{
            var config=(OrderBookConfiguration)e.pricing();var t=e.orderBook().orElseThrow();
            var participants=t.positions().stream().map(p->new ParticipantDto(p.userName(),p.optionOneShares(),p.optionTwoShares(),optional(p.optionOneValue()),optional(p.optionTwoValue()))).toList();
            book=new BookDetailDto(config.d(),config.initial(),config.allowMint(),t.issuedPairs(),t.orders().stream().map(o->new BookOrderDto(o.sequence(),o.userName(),o.optionNumber(),o.side(),o.quantity(),o.limitPrice().toPlainString())).toList(),t.trades().stream().map(ApiSnapshotMapper::execution).toList(),t.quotes().stream().map(q->new BookQuoteDto(q.optionNumber(),optional(q.bid()),optional(q.ask()),optional(q.last()),optional(q.mid()),optional(q.spread()),optional(q.estimate()),q.valuationBasis())).toList(),participants);
            position=t.positions().stream().filter(p->p.userName().equals(actor)).map(p->new PositionDto(p.optionOneShares(),p.optionTwoShares(),p.reservedOne(),p.reservedTwo(),p.availableOne(),p.availableTwo(),p.optionOnePaid().toPlainString(),p.optionTwoPaid().toPlainString(),p.funding().toPlainString(),p.purchases().toPlainString(),p.saleReceipts().toPlainString(),p.commissionsPaid().toPlainString(),p.commissionIncome().toPlainString(),p.settlementReceipts().toPlainString(),p.profitLoss().toPlainString(),optional(p.optionOneValue()),optional(p.optionTwoValue()))).findFirst().orElse(new PositionDto(0,0,0,0,0,0,"0","0","0","0","0","0","0","0","0",null,null));
            String commission=t.positions().stream().map(OrderBookPosition::commissionsPaid).reduce(BigDecimal.ZERO,BigDecimal::add).toPlainString();
            settlement=t.settlement().map(p->settlement(p,d(e.contractBalance()),commission)).orElse(null);
        }
        boolean owner=e.marketMakerName().equals(actor);
        return new EventDetailDto(summary(e),e.optionLabels(),lmsr,book,position,settlement,owner&&!blocked&&e.status()==WorldEventStatus.NOT_STARTED,!blocked&&e.status()==WorldEventStatus.ACTIVE,owner&&e.status()==WorldEventStatus.ACTIVE);
    }
    static ExecutionDto execution(OrderBookTrade t){return new ExecutionDto(t.sequence(),t.optionNumber(),t.buyer(),t.seller(),t.mint()?CounterpartyRole.OPPOSITE_BUYER:CounterpartyRole.SELLER,t.quantity(),t.price().toPlainString(),t.principal().toPlainString(),t.commission().toPlainString(),t.mint());}
    static PayoutDto payout(SettlementPayment p){return new PayoutDto(p.userName(),p.winningShares(),d(p.grossPayout()),d(p.commission()),d(p.netPayout()));}
    static SettlementDto settlement(ClosePreview p,String balance,String commission){return new SettlementDto(p.winningOption(),p.winningLabel(),balance,commission,d(p.totalGrossPayout()),d(p.totalCommission()),d(p.subsidyRefund()),p.payments().stream().map(ApiSnapshotMapper::payout).toList());}
    static String d(double value){return Double.toString(value);}
    private static String optional(Optional<BigDecimal> value){return value.map(BigDecimal::toPlainString).orElse(null);}
}
