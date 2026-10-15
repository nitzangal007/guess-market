package guessmarket.engine;
import guessmarket.dto.api.AccountEntryDto;
import guessmarket.dto.api.CashEffectDto;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
final class AccountJournal {
    private final long sequence;
    private final Map<String,List<AccountEntryDto>> rows;
    AccountJournal() {this(0);}
    AccountJournal(long sequence) {this(sequence,Map.of());}
    private AccountJournal(long sequence,Map<String,List<AccountEntryDto>> rows){this.sequence=sequence;this.rows=Map.copyOf(rows);}
    AccountJournal prepare(List<AccountCashEffect> effects,String transactionId){
        var next=new LinkedHashMap<>(rows);
        long id=sequence;
        String time=Instant.now().toString();
        for(var effect:effects){
            var components=new ArrayList<>(effect.components());
            BigDecimal running=BigDecimal.valueOf(effect.storedBefore());
            BigDecimal end=BigDecimal.valueOf(effect.storedAfter());
            BigDecimal total=components.stream().map(CashComponent::signedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal adjustment=end.subtract(running).subtract(total);
            if(adjustment.signum()!=0)components.add(new CashComponent(effect.username(),"REPRESENTATION_ADJUSTMENT",null,null,null,adjustment));
            var entries=new ArrayList<>(entries(effect.username()));
            for(var component:components){
                if(component.signedAmount().signum()==0)continue;
                id=Math.incrementExact(id);
                running=running.add(component.signedAmount());
                entries.add(new AccountEntryDto(id,transactionId,time,component.kind(),component.eventId(),component.eventName(),component.optionNumber(),component.signedAmount().toPlainString(),running.toPlainString(),end.toPlainString()));
            }
            if(running.compareTo(end)!=0)throw new ArithmeticException("Journal reconciliation failed");
            next.put(effect.username(),List.copyOf(entries));
        }
        return new AccountJournal(id,next);
    }
    List<AccountEntryDto> entries(String actor){return rows.getOrDefault(actor,List.of());}
    static List<CashEffectDto> explain(AccountCashEffect effect){
        var components=new ArrayList<>(effect.components());BigDecimal running=BigDecimal.valueOf(effect.storedBefore()),end=BigDecimal.valueOf(effect.storedAfter());
        BigDecimal total=components.stream().map(CashComponent::signedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);BigDecimal adjustment=end.subtract(running).subtract(total);
        if(adjustment.signum()!=0)components.add(new CashComponent(effect.username(),"REPRESENTATION_ADJUSTMENT",null,null,null,adjustment));
        var result=new ArrayList<CashEffectDto>();for(var c:components){if(c.signedAmount().signum()==0)continue;running=running.add(c.signedAmount());result.add(new CashEffectDto(c.kind(),c.optionNumber(),c.signedAmount().toPlainString(),running.toPlainString(),end.toPlainString()));}return List.copyOf(result);
    }
}
