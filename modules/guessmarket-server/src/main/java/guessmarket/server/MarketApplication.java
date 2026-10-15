package guessmarket.server;
import guessmarket.dto.api.*;
import guessmarket.engine.*;
import java.util.*;
import java.util.function.Supplier;
public final class MarketApplication {
    private final String instance=UUID.randomUUID().toString();
    private final SharedMarketEngineImpl engine=new SharedMarketEngineImpl(instance);
    private final SessionRegistry sessions=new SessionRegistry();
    private final RequestRegistry requests=new RequestRegistry();
    public synchronized LoginResponse login(String session,String username){
        if(username==null||username.trim().isEmpty()||username.trim().length()>100||!username.matches("[ -~]+"))
            throw failure(400,"INVALID_USERNAME","Enter an English name of 1 to 100 characters.",null);
        String display=username.trim();String key=display.toLowerCase(Locale.ROOT);
        String existing=sessions.reserve(session,key,this);boolean created=existing==null;String actor=created?display:existing;
        try{engine.ensureAccount(actor);}catch(WorldCommandException problem){throw mapped(problem,null);}
        sessions.bind(session,key,actor);
        return new LoginResponse(instance,engine.revision(),actor,created);
    }
    public synchronized void logout(String session){sessions.revoke(session);}
    public synchronized DashboardResponse dashboard(String session,Integer event){
        String actor=actor(session);try{return engine.dashboard(actor,event);}catch(WorldCommandException problem){throw mapped(problem,null);}
    }
    public synchronized CommandResponse addFunds(String session,FundsRequest request){
        String actor=actor(session);RequestValidator.funds(request,this);
        String fingerprint="funds:"+RequestValidator.decimal(request.amount(),this).stripTrailingZeros().toString();
        return (CommandResponse)mutate(actor,request.requestId(),fingerprint,()->{
            try{return engine.addFunds(actor,request.amount(),request.requestId());}catch(WorldCommandException problem){throw mapped(problem,request.requestId());}
        });
    }
    public synchronized CommandStatusDto commandStatus(String session,String id){
        String actor=actor(session);RequestValidator.uuid(id,this);var entry=requests.lookup(actor,id);
        if(entry==null)throw failure(404,"REQUEST_NOT_FOUND","This request has no retained receipt yet. Retry only the same request.",id);
        return new CommandStatusDto(instance,engine.revision(),id,entry.error()!=null?ReceiptStatus.REJECTED:ReceiptStatus.COMPLETED,
                entry.result() instanceof CommandResponse c?c:null,entry.result() instanceof UploadResponse u?u:null,entry.error());
    }
    public synchronized UploadResponse upload(String session,String id,String digest,String filename,guessmarket.engine.xml.ex3.ParsedEventBatch batch){
        String actor=actor(session);RequestValidator.uuid(id,this);String fingerprint="upload:"+digest+":"+filename;
        return (UploadResponse)mutate(actor,id,fingerprint,()->{try{return engine.appendEvents(actor,batch,id);}catch(WorldCommandException problem){throw mapped(problem,id);}});
    }
    public synchronized ActionPreviewDto preview(String session,int eventId,ActionRequest request){
        String actor=actor(session);RequestValidator.action(request,this);try{return engine.preview(actor,eventId,request);}catch(WorldCommandException problem){throw mapped(problem,null);}
    }
    public synchronized CommandResponse execute(String session,int eventId,CommandRequest request){
        String actor=actor(session);if(request==null)throw failure(400,"INVALID_REQUEST","Command input required.",null);RequestValidator.uuid(request.requestId(),this);RequestValidator.action(request.action(),this);
        if(request.expectedRevision()<0)throw failure(400,"INVALID_REVISION","A nonnegative revision is required.",request.requestId());
        var action=request.action();String limit=action.limitPrice()==null?null:RequestValidator.decimal(action.limitPrice(),this).stripTrailingZeros().toString();
        var normalized=new ActionRequest(action.action(),action.optionNumber(),action.quantity(),action.side(),limit);
        String fingerprint="event:"+eventId+":"+request.expectedRevision()+":"+JsonCodec.gson().toJson(normalized);
        return (CommandResponse)mutate(actor,request.requestId(),fingerprint,()->{try{return engine.execute(actor,eventId,request);}catch(WorldCommandException problem){throw mapped(problem,request.requestId());}});
    }
    synchronized String actor(String session){String actor=sessions.actor(session);if(actor==null)throw failure(401,"UNAUTHENTICATED","Log in to use the market.",null);return actor;}
    Object mutate(String actor,String id,String fingerprint,Supplier<Object> operation){
        var prior=requests.lookup(actor,id);
        if(prior!=null){if(!prior.fingerprint().equals(fingerprint))throw failure(409,"REQUEST_ID_REUSED","This request ID was already used for different input.",id);if(prior.error()!=null)throw new ApiFailure(prior.status(),prior.error());return prior.result();}
        try{Object result=operation.get();requests.complete(actor,id,fingerprint,result);return result;}
        catch(ApiFailure problem){requests.reject(actor,id,fingerprint,problem);throw problem;}
    }
    ApiFailure mapped(WorldCommandException problem,String id){return ApiExceptionMapper.map(problem,id,instance,engine.revision());}
    ApiFailure failure(int status,String code,String message,String id){return new ApiFailure(status,new ApiError(code,message,null,"Correct the input or refresh and try again.",id,instance,engine.revision()));}
    public String instance(){return instance;}
    public synchronized long revision(){return engine.revision();}
}
