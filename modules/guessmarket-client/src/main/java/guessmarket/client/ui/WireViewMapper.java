package guessmarket.client.ui;
import java.math.*;
import guessmarket.dto.api.EventSummaryDto;
public final class WireViewMapper {
    private WireViewMapper() {}
    public static String money(String raw){return raw==null?"Unavailable":new BigDecimal(raw).setScale(2,RoundingMode.HALF_UP).toPlainString();}
    public static String eventName(EventSummaryDto event){return event.name().isEmpty()?"[unnamed event #"+event.id()+"]":event.name();}
    public static String option(int number,java.util.List<String> labels){String label=labels.get(number-1);return number+": "+(label.isEmpty()?"(empty label)":label);}
    public static String words(String raw){return raw==null?"Not applicable":raw.replace('_',' ');}
    public static String adjustment(String raw){if(raw!=null){var value=new BigDecimal(raw);if(value.signum()!=0&&value.abs().compareTo(new BigDecimal(".01"))<0)return (value.signum()<0?"-":"+")+"<0.01";}return money(raw);}
}
