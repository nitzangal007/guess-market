package guessmarket.engine.xml.ex3;
import guessmarket.dto.CommissionMode;
import guessmarket.dto.world.*;
import guessmarket.engine.*;
import guessmarket.engine.xml.ex3.generated.GuessMarket;
import java.util.*;
final class Ex3JaxbEventMapper {
    ParsedEventBatch map(GuessMarket root)throws EngineOperationException {
        var definitions=new ArrayList<EventDefinition>();var names=new HashSet<String>();
        for(var event:root.getGMEvents().getGMEvent()){
            String name=event.getName().trim();if(!names.add(name))throw invalid("name","Duplicate event name in this file.");
            int commission=event.getCommission().getValue();if(commission<0||commission>90)throw invalid("commission","Commission must be between 0 and 90.");
            var options=event.getGMOptions().getGMOption().stream().map(String::trim).toList();if(options.size()!=2)throw invalid("GM-options","Exactly two ordered options are required.");
            PricingConfiguration pricing;
            if(event.getGMMethod().getGMLMSR()!=null){int b=event.getGMMethod().getGMLMSR().getB();if(b<=0)throw invalid("b","LMSR b must be positive.");pricing=new LmsrConfiguration(b);}
            else{var book=event.getGMMethod().getGMOrderBook();if(book.getInitial()<0||book.getD()<=0)throw invalid("GM-order-book","Initial must be nonnegative and d positive.");pricing=new OrderBookConfiguration(book.getInitial(),book.getD(),Boolean.parseBoolean(book.getAllowMint()));}
            var mode=event.getCommission().getType().equals("on-purchase")?CommissionMode.ON_PURCHASE:CommissionMode.ON_CLOSE;
            definitions.add(new EventDefinition(name,event.getDescription().trim(),options,commission,mode,pricing));
        }
        return new ParsedEventBatch(definitions);
    }
    private static EngineOperationException invalid(String field,String detail){return new EngineOperationException(EngineErrorCode.XML_DATA_INVALID,detail,"Correct the XML and upload again.",null,null,null,field,null,null,null,null,null,null);}
}
