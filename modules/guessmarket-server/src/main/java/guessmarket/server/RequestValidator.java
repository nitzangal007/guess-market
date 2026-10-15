package guessmarket.server;
import guessmarket.dto.api.*;
import java.math.BigDecimal;
final class RequestValidator {
    private RequestValidator() {}
    static void uuid(String id,MarketApplication app){if(id==null||!id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw app.failure(400,"INVALID_REQUEST_ID","A UUID request ID is required.",id);}
    static BigDecimal decimal(String raw,MarketApplication app){
        try{if(raw==null||raw.length()>1000)throw new NumberFormatException();var value=new BigDecimal(raw);if(Math.abs((long)value.scale())>1000||!Double.isFinite(value.doubleValue()))throw new NumberFormatException();return value;}
        catch(NumberFormatException problem){throw app.failure(400,"INVALID_AMOUNT","Enter a finite decimal amount.",null);}
    }
    static void funds(FundsRequest request,MarketApplication app){if(request==null)throw app.failure(400,"INVALID_REQUEST","Funds input is required.",null);uuid(request.requestId(),app);if(decimal(request.amount(),app).signum()<=0)throw app.failure(400,"INVALID_AMOUNT","Funds must be strictly positive.",request.requestId());}
    static void action(ActionRequest r,MarketApplication app){
        if(r==null||r.action()==null)throw app.failure(400,"INVALID_ACTION","Choose OPEN, LMSR_BUY, ORDER or CLOSE.",null);
        boolean valid=switch(r.action()){
            case OPEN->r.optionNumber()==null&&r.quantity()==null&&r.side()==null&&r.limitPrice()==null;
            case CLOSE->option(r.optionNumber())&&r.quantity()==null&&r.side()==null&&r.limitPrice()==null;
            case LMSR_BUY->option(r.optionNumber())&&r.quantity()!=null&&r.quantity()>0&&r.side()==null&&r.limitPrice()==null;
            case ORDER->option(r.optionNumber())&&r.quantity()!=null&&r.quantity()>0&&r.side()!=null&&r.limitPrice()!=null;
        };
        if(!valid)throw app.failure(400,"INVALID_ACTION_FIELDS","Supply only the fields required for this action, with option 1 or 2 and positive quantity.",null);
        if(r.limitPrice()!=null)decimal(r.limitPrice(),app);
    }
    private static boolean option(Integer option){return option!=null&&(option==1||option==2);}
}
