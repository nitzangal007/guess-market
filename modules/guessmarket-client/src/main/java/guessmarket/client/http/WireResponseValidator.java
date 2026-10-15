package guessmarket.client.http;
import com.google.gson.*;
import guessmarket.dto.api.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
/** Validates wire shape and receipt metadata, without calculating trading rules. */
final class WireResponseValidator {
    record Expected(String requestId,OperationKind operation,Integer eventId,ActionRequest action,Long reviewedRevision) {}
    private static final Set<String> NULLABLE=Set.of(
        "ApiError.field","ApiError.recovery","ApiError.requestId","ApiError.serverInstanceId","ApiError.revision",
        "DashboardResponse.selectedEvent","EventDetailDto.lmsr","EventDetailDto.book","EventDetailDto.ownPosition","EventDetailDto.settlement",
        "ActionRequest.optionNumber","ActionRequest.quantity","ActionRequest.side","ActionRequest.limitPrice",
        "AccountEntryDto.eventId","AccountEntryDto.eventName","AccountEntryDto.optionNumber","CashEffectDto.optionNumber",
        "ActionPreviewDto.winningLabel","ActionPreviewDto.incomingRemaining","ActionPreviewDto.cancelledOrderCount","ActionPreviewDto.releasedReservationQuantity","ActionPreviewDto.totalGrossPayout","ActionPreviewDto.totalCommission","ActionPreviewDto.subsidyRefund",
        "CommandResponse.eventId","CommandResponse.confirmedAction","CommandResponse.creditedAmount","CommandResponse.confirmedReview",
        "CommandStatusDto.command","CommandStatusDto.upload","CommandStatusDto.error",
        "BookQuoteDto.bid","BookQuoteDto.ask","BookQuoteDto.last","BookQuoteDto.mid","BookQuoteDto.spread","BookQuoteDto.estimate",
        "PositionDto.funding","PositionDto.saleReceipts","PositionDto.feesReceived","PositionDto.profitLoss","PositionDto.valueOne","PositionDto.valueTwo","ParticipantDto.optionOneValue","ParticipantDto.optionTwoValue");
    private static final Set<String> MONEY=Set.of("balance","price","principal","commission","limitPrice","shareCost","totalPaid","totalDebit","totalCredit","balanceBefore","balanceAfter","amount","journalRunningBalance","storedBalanceAfterTransaction","creditedAmount","contractBalance","finalContractBalance","commissionTotal","totalGrossPayout","totalCommission","subsidyRefund","grossPayout","netPayout","bid","ask","last","mid","spread","estimate","paidOne","paidTwo","funding","purchases","saleReceipts","feesPaid","feesReceived","payout","profitLoss","valueOne","valueTwo","optionOneValue","optionTwoValue");
    private WireResponseValidator() {}
    static void shape(JsonElement value,Type type){
        if(type instanceof ParameterizedType parameter&&parameter.getRawType()==List.class){require(value.isJsonArray());for(var element:value.getAsJsonArray())shape(element,parameter.getActualTypeArguments()[0]);return;}
        Class<?> clazz=(Class<?>)type;
        if(clazz.isRecord()){
            require(value.isJsonObject());var object=value.getAsJsonObject();for(var component:clazz.getRecordComponents()){
                var field=object.get(component.getName());if(field==null||field.isJsonNull()){require(NULLABLE.contains(clazz.getSimpleName()+"."+component.getName()));continue;}
                shape(field,component.getGenericType());if(MONEY.contains(component.getName())&&component.getType()==String.class){var number=new BigDecimal(field.getAsString());require(field.getAsString().length()<=1000&&Math.abs((long)number.scale())<=1000&&Double.isFinite(number.doubleValue()));}
            }return;
        }
        require(value.isJsonPrimitive());var scalar=value.getAsJsonPrimitive();
        if(clazz==String.class){require(scalar.isString());return;}
        if(clazz==boolean.class||clazz==Boolean.class){require(scalar.isBoolean());return;}
        if(clazz==int.class||clazz==Integer.class){require(scalar.isNumber());scalar.getAsBigDecimal().intValueExact();return;}
        if(clazz==long.class||clazz==Long.class){require(scalar.isNumber());scalar.getAsBigDecimal().longValueExact();return;}
        if(clazz.isEnum()){require(scalar.isString());require(Arrays.stream(clazz.getEnumConstants()).anyMatch(constant->((Enum<?>)constant).name().equals(scalar.getAsString())));return;}
        throw new JsonSyntaxException("Unsupported wire value");
    }
    static void metadata(Object value,Expected expected){
        if(value instanceof LoginResponse r){common(r.serverInstanceId(),r.revision());require(nonblank(r.username()));}
        else if(value instanceof DashboardResponse r){common(r.serverInstanceId(),r.revision());require(r.account()!=null&&nonblank(r.account().username()));for(var event:r.events()){require(event.id()>0&&nonblank(event.owner())&&Set.of("LMSR","ORDER_BOOK").contains(event.method())&&Set.of("NOT_STARTED","ACTIVE","CLOSED").contains(event.status()));}if(r.selectedEvent()!=null){var detail=r.selectedEvent();require(r.events().stream().anyMatch(e->e.equals(detail.summary())));require(detail.optionLabels().size()==2);require(detail.summary().method().equals("LMSR")?detail.lmsr()!=null&&detail.book()==null:detail.book()!=null&&detail.lmsr()==null);if(detail.lmsr()!=null)require(detail.lmsr().options().size()==2);if(detail.settlement()!=null)require(detail.settlement().winningOption()==1||detail.settlement().winningOption()==2);}}
        else if(value instanceof ActionPreviewDto r){common(r.serverInstanceId(),r.revision());require(r.eventId()>0&&nonblank(r.actingUser())&&r.action()!=null);if(expected!=null){require(Objects.equals(expected.eventId(),r.eventId())&&Objects.equals(expected.action(),r.action()));}if(r.action().action()==ActionKind.ORDER)require(r.incomingRemaining()!=null&&r.cancelledOrderCount()!=null&&r.releasedReservationQuantity()!=null);if(r.action().action()==ActionKind.CLOSE)require(r.winningLabel()!=null&&r.totalGrossPayout()!=null&&r.totalCommission()!=null&&r.subsidyRefund()!=null);}
        else if(value instanceof CommandResponse r){common(r.serverInstanceId(),r.revision());require(nonblank(r.requestId())&&r.operation()!=null&&r.operation()!=OperationKind.UPLOAD);if(expected!=null){require(sameId(expected.requestId(),r.requestId()));if(expected.operation()!=null)require(expected.operation()==r.operation());}
            if(r.operation()==OperationKind.FUND_LOAD){require(r.eventId()==null&&r.confirmedAction()==null&&r.confirmedReview()==null&&r.creditedAmount()!=null&&new BigDecimal(r.creditedAmount()).signum()>0);}
            else{require(r.eventId()!=null&&r.eventId()>0&&r.confirmedAction()!=null&&r.confirmedReview()!=null&&r.creditedAmount()==null);require(r.operation().name().equals(r.confirmedAction().action().name()));var review=r.confirmedReview();metadata(review,new Expected(null,null,r.eventId(),r.confirmedAction(),null));require(r.serverInstanceId().equals(review.serverInstanceId())&&review.revision()<r.revision());if(expected!=null){if(expected.eventId()!=null)require(expected.eventId().equals(r.eventId()));if(expected.action()!=null)require(expected.action().equals(r.confirmedAction()));if(expected.reviewedRevision()!=null)require(expected.reviewedRevision()==review.revision());}}
        }
        else if(value instanceof UploadResponse r){common(r.serverInstanceId(),r.revision());require(nonblank(r.requestId())&&r.operation()==OperationKind.UPLOAD&&!r.addedEvents().isEmpty());require(r.addedEvents().stream().allMatch(e->e.id()>0&&e.name()!=null));if(expected!=null)require(sameId(expected.requestId(),r.requestId()));}
        else if(value instanceof CommandStatusDto r){common(r.serverInstanceId(),r.revision());require(nonblank(r.requestId())&&r.status()!=null);if(expected!=null)require(sameId(expected.requestId(),r.requestId()));
            switch(r.status()){
                case COMPLETED->{require((r.command()==null)!=(r.upload()==null));require(r.error()==null);Object receipt=r.command()!=null?r.command():r.upload();metadata(receipt,new Expected(r.requestId(),null,null,null,null));String server=r.command()!=null?r.command().serverInstanceId():r.upload().serverInstanceId();long revision=r.command()!=null?r.command().revision():r.upload().revision();require(r.serverInstanceId().equals(server)&&revision<=r.revision());}
                case REJECTED->{require(r.command()==null&&r.upload()==null&&r.error()!=null&&nonblank(r.error().code())&&nonblank(r.error().message()));if(r.error().requestId()!=null)require(sameId(r.requestId(),r.error().requestId()));}
                case PENDING->require(r.command()==null&&r.upload()==null&&r.error()==null);
            }
        }
    }
    private static void common(String instance,long revision){require(nonblank(instance)&&revision>=0);}
    private static boolean nonblank(String value){return value!=null&&!value.isBlank();}
    private static boolean sameId(String expected,String actual){return expected!=null&&actual!=null&&expected.equalsIgnoreCase(actual);}
    private static void require(boolean valid){if(!valid)throw new JsonSyntaxException("Required wire content or identity is missing or inconsistent");}
}
