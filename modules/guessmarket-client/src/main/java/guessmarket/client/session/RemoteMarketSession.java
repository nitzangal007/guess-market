package guessmarket.client.session;
import guessmarket.client.http.*;
import guessmarket.dto.api.*;
import javafx.application.Platform;
import javafx.beans.property.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** UI-thread state; blocking network/file work belongs to the command and polling workers. */
public final class RemoteMarketSession implements AutoCloseable {
    private final String baseUrl;
    private final Executor ui;
    private final ExecutorService commands=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"market-command");t.setDaemon(true);return t;});
    private final ReadOnlyObjectWrapper<DashboardResponse> dashboard=new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<Object> receipt=new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper loggedIn=new ReadOnlyBooleanWrapper(),connected=new ReadOnlyBooleanWrapper(),busy=new ReadOnlyBooleanWrapper(),unknown=new ReadOnlyBooleanWrapper(),loginUnknown=new ReadOnlyBooleanWrapper();
    private final ReadOnlyStringWrapper notice=new ReadOnlyStringWrapper(""),username=new ReadOnlyStringWrapper(""),lastRefresh=new ReadOnlyStringWrapper("");
    private final OperationGate operation=new OperationGate();
    private final DashboardGate views=new DashboardGate();
    private MarketHttpClient client,loginAttempt;
    private PollCoordinator poll;
    private long generation;
    private String instance;
    private Integer selectedEvent;
    private Attempt pending;
    private boolean closed;
    private interface Attempt {String id();Object send(MarketHttpClient client)throws IOException,ApiCallException;boolean matches(Object value);}
    private record Funds(FundsRequest request)implements Attempt{public String id(){return request.requestId();}public Object send(MarketHttpClient c)throws IOException,ApiCallException{return c.addFunds(request);}public boolean matches(Object value){return value instanceof CommandResponse c&&c.operation()==OperationKind.FUND_LOAD&&id().equalsIgnoreCase(c.requestId());}}
    private record Command(int event,CommandRequest request)implements Attempt{public String id(){return request.requestId();}public Object send(MarketHttpClient c)throws IOException,ApiCallException{return c.execute(event,request);}public boolean matches(Object value){return value instanceof CommandResponse c&&id().equalsIgnoreCase(c.requestId())&&Objects.equals(c.eventId(),event)&&c.operation()==OperationKind.valueOf(request.action().action().name())&&request.action().equals(c.confirmedAction())&&c.confirmedReview()!=null&&c.confirmedReview().revision()==request.expectedRevision();}}
    private record Upload(UploadAttempt attempt)implements Attempt{public String id(){return attempt.requestId();}public Object send(MarketHttpClient c)throws IOException,ApiCallException{return c.upload(attempt);}public boolean matches(Object value){return value instanceof UploadResponse u&&u.operation()==OperationKind.UPLOAD&&id().equalsIgnoreCase(u.requestId());}}
    public RemoteMarketSession(){this(MarketHttpClient.DEFAULT_URL,Platform::runLater);}
    public RemoteMarketSession(String baseUrl,Executor ui){this.baseUrl=baseUrl;this.ui=ui;}
    public ReadOnlyObjectProperty<DashboardResponse> dashboardProperty(){return dashboard.getReadOnlyProperty();}
    public DashboardResponse dashboard(){return dashboard.get();}
    public ReadOnlyObjectProperty<Object> receiptProperty(){return receipt.getReadOnlyProperty();}
    public ReadOnlyBooleanProperty loggedInProperty(){return loggedIn.getReadOnlyProperty();}
    public ReadOnlyBooleanProperty connectedProperty(){return connected.getReadOnlyProperty();}
    public ReadOnlyBooleanProperty busyProperty(){return busy.getReadOnlyProperty();}
    public ReadOnlyBooleanProperty unknownProperty(){return unknown.getReadOnlyProperty();}
    public ReadOnlyBooleanProperty loginUnknownProperty(){return loginUnknown.getReadOnlyProperty();}
    public ReadOnlyStringProperty noticeProperty(){return notice.getReadOnlyProperty();}
    public ReadOnlyStringProperty usernameProperty(){return username.getReadOnlyProperty();}
    public ReadOnlyStringProperty lastRefreshProperty(){return lastRefresh.getReadOnlyProperty();}
    public boolean mutationAllowed(){return loggedIn.get()&&connected.get()&&!busy.get()&&!operation.pending();}
    public boolean actionAllowed(ActionPreviewDto preview){
        if(!mutationAllowed()||dashboard.get()==null||!Objects.equals(instance,preview.serverInstanceId())||!username.get().equals(preview.actingUser()))return false;var detail=dashboard.get().selectedEvent();if(detail==null||detail.summary().id()!=preview.eventId())return false;
        return switch(preview.action().action()){case OPEN->detail.canOpen();case LMSR_BUY->detail.canTrade()&&detail.lmsr()!=null;case ORDER->detail.canTrade()&&detail.book()!=null;case CLOSE->detail.canClose();};
    }
    public void clearNotice(){notice.set("");receipt.set(null);}
    public void login(String name){
        long token=++generation;clearPrivate();busy.set(true);notice.set("Connecting...");
        var attempt=new MarketHttpClient(baseUrl);loginAttempt=attempt;
        commands.execute(()->{
            try{var result=attempt.login(name);ui.execute(()->promote(token,attempt,result.username(),result.serverInstanceId()));}
            catch(IOException failure){
                recoverLogin(token,attempt,failure.getMessage());
            }catch(ApiCallException failure){if(failure.status()>=500||failure.status()<400)recoverLogin(token,attempt,failure.getMessage());else ui.execute(()->{if(token!=generation){attempt.close();return;}busy.set(false);notice.set(failure.getMessage());attempt.close();loginAttempt=null;});}
            catch(RuntimeException failure){recoverLogin(token,attempt,"The login response could not be interpreted.");}
        });
    }
    private void recoverLogin(long token,MarketHttpClient attempt,String problem){
        try{var result=attempt.dashboard(null);ui.execute(()->promote(token,attempt,result.account().username(),result.serverInstanceId()));}
        catch(Exception check){ui.execute(()->{if(token!=generation){attempt.close();return;}busy.set(false);loginUnknown.set(true);notice.set("Login outcome is unknown. "+problem+" Check the retained session, or wait for the name to expire. Canceling a local request does not release it.");});}
    }
    public void checkLogin(){
        if(loginAttempt==null||busy.get())return;var attempt=loginAttempt;long token=generation;busy.set(true);
        commands.execute(()->{try{var value=attempt.dashboard(null);ui.execute(()->promote(token,attempt,value.account().username(),value.serverInstanceId()));}catch(Exception failure){ui.execute(()->{if(token==generation){busy.set(false);notice.set("Login is still uncertain. "+failure.getMessage());}});}});
    }
    private void promote(long token,MarketHttpClient attempt,String actor,String server){
        if(token!=generation){discard(attempt);return;}
        client=attempt;loginAttempt=null;instance=server;username.set(actor);operation.reset(token,server);views.reset(token,server);busy.set(false);loginUnknown.set(false);unknown.set(false);connected.set(true);notice.set("");loggedIn.set(true);
        poll=new PollCoordinator(attempt,(query,value)->ui.execute(()->{
            if(token!=generation)return;if(!server.equals(value.serverInstanceId())){expire("The server restarted. The previous market state is gone.");return;}
            boolean restored=!connected.get();connected.set(true);if(restored&&!unknown.get()&&!busy.get()&&notice.get().startsWith("Disconnected"))notice.set("Connection restored.");lastRefresh.set(java.time.LocalTime.now().withNano(0).toString());if(views.accept(token,query.selection(),value))dashboard.set(value);
        }),(query,failure)->ui.execute(()->{if(token!=generation)return;if(failure instanceof ApiCallException call&&call.status()==401){expire("Your session expired. Log in again.");return;}connected.set(false);if(!(unknown.get()||busy.get()))notice.set("Disconnected. Cached information may be stale. "+failure.getMessage());}));
        poll.start();
    }
    public void selectEvent(Integer event){selectedEvent=event;clearNotice();if(poll!=null){long selection=views.select(event);poll.selectEvent(event,selection);poll.requestNow();}}
    public void refresh(){if(poll!=null)poll.requestNow();}
    public void preview(int event,ActionRequest input,Consumer<ActionPreviewDto> accepted,Consumer<String> failed){
        if(!mutationAllowed()){failed.accept("Connect to the server and resolve the pending operation first.");return;}
        var transport=client;long token=generation,selection=views.selection();busy.set(true);clearNotice();
        commands.execute(()->{try{var value=transport.preview(event,input);ui.execute(()->{if(token!=generation)return;busy.set(false);if(!Objects.equals(instance,value.serverInstanceId())){expire("The server restarted.");return;}if(!username.get().equals(value.actingUser())){failed.accept("The review does not match this account.");return;}if(selection==views.selection()&&Objects.equals(selectedEvent,event))accepted.accept(value);});}
        catch(Exception problem){ui.execute(()->{if(token!=generation)return;busy.set(false);if(problem instanceof ApiCallException call&&call.status()==401){expire("Your session expired.");return;}if(problem instanceof IOException)connected.set(false);if(selection==views.selection())failed.accept(problem.getMessage());});}});
    }
    public void execute(ActionPreviewDto preview){if(!actionAllowed(preview))return;submit(new Command(preview.eventId(),new CommandRequest(UUID.randomUUID().toString(),preview.revision(),preview.action())));}
    public void addFunds(String amount){if(mutationAllowed())submit(new Funds(new FundsRequest(UUID.randomUUID().toString(),amount)));}
    public void upload(Path path){
        if(!mutationAllowed())return;long token=generation;busy.set(true);clearNotice();String id=UUID.randomUUID().toString();
        commands.execute(()->{try{var captured=UploadAttempt.capture(path,id);ui.execute(()->{if(token!=generation)return;busy.set(false);submit(new Upload(captured));});}catch(Exception failure){ui.execute(()->{if(token==generation){busy.set(false);notice.set(failure.getMessage());}});}});
    }
    private void submit(Attempt attempt){pending=attempt;operation.begin(attempt.id());busy.set(true);unknown.set(false);clearNotice();send(attempt,generation,client);}
    private void send(Attempt attempt,long token,MarketHttpClient transport){
        commands.execute(()->{try{Object value=attempt.send(transport);ui.execute(()->complete(token,attempt,value));}catch(ApiCallException failure){ui.execute(()->{
            if(token!=generation)return;if(failure.status()==401){expire("Your session expired. Check account activity after reconnecting.");return;}
            if(failure.error().serverInstanceId()!=null&&!Objects.equals(instance,failure.error().serverInstanceId())){expire("The server restarted.");return;}
            if(failure.status()>=500||failure.status()<400){uncertain(failure.getMessage());return;}
            resolve(token,instance,attempt.id());notice.set(failure.getMessage()+" "+Objects.toString(failure.error().recovery(),""));refresh();
        });}catch(IOException failure){ui.execute(()->{if(token==generation){connected.set(false);uncertain("Outcome unknown. Check or retry the same request. "+failure.getMessage());}});}catch(RuntimeException failure){ui.execute(()->{if(token==generation)uncertain("The client could not interpret the operation outcome. Check/retry the retained request.");});}});
    }
    private void uncertain(String message){busy.set(false);unknown.set(true);notice.set(message);}
    private void complete(long token,Attempt attempt,Object value){
        if(token!=generation)return;if(!attempt.matches(value)||(value instanceof CommandResponse command&&command.confirmedReview()!=null&&!username.get().equals(command.confirmedReview().actingUser()))){uncertain("The receipt does not match the retained request. Check its outcome again.");return;}
        if(token!=generation)return;String server=value instanceof CommandResponse c?c.serverInstanceId():((UploadResponse)value).serverInstanceId();String id=value instanceof CommandResponse c?c.requestId():((UploadResponse)value).requestId();
        if(!Objects.equals(instance,server)){expire("The server restarted. Do not retry the old request.");return;}
        if(!operation.resolve(token,server,id))return;
        pending=null;busy.set(false);unknown.set(false);connected.set(true);receipt.set(value);notice.set(value instanceof CommandResponse c?c.message():"Uploaded "+((UploadResponse)value).addedEvents().size()+" events.");refresh();
    }
    private void resolve(long token,String server,String id){if(operation.resolve(token,server,id)){pending=null;busy.set(false);unknown.set(false);}}
    public void checkOrRetryOutcome(){
        if(pending==null||busy.get())return;var attempt=pending;var transport=client;long token=generation;busy.set(true);notice.set("Checking outcome...");
        commands.execute(()->{try{var status=transport.commandStatus(attempt.id());ui.execute(()->{
            if(token!=generation)return;if(!Objects.equals(instance,status.serverInstanceId())){expire("The server restarted. The old request cannot be replayed.");return;}
            if(status.status()==ReceiptStatus.COMPLETED)complete(token,attempt,status.command()!=null?status.command():status.upload());
            else if(status.status()==ReceiptStatus.REJECTED){resolve(token,instance,attempt.id());notice.set(status.error().message());refresh();}
            else uncertain("The server is still processing the request. Check again.");
        });}catch(ApiCallException failure){
            if(failure.status()==404){try{var snapshot=transport.dashboard(null);ui.execute(()->{if(token!=generation)return;if(!Objects.equals(instance,snapshot.serverInstanceId())){expire("The server restarted. The old request cannot be replayed.");return;}notice.set("Retrying the exact retained request...");send(attempt,token,transport);});}catch(Exception check){ui.execute(()->{if(token==generation)uncertain("Could not verify the server instance. Check again after connecting.");});}}
            else ui.execute(()->{if(token!=generation)return;if(failure.status()==401)expire("Your session expired. Check account activity after reconnecting.");else uncertain(failure.getMessage());});
        }catch(IOException failure){ui.execute(()->{if(token==generation){connected.set(false);uncertain(failure.getMessage());}});}});
    }
    public void logout(){var prior=client!=null?client:loginAttempt;++generation;clearPrivate();notice.set("Logged out.");if(prior!=null)discard(prior);}
    private void expire(String message){var prior=client;++generation;clearPrivate();notice.set(message);if(prior!=null)prior.close();}
    private void clearPrivate(){if(poll!=null){poll.close();poll=null;}client=null;instance=null;selectedEvent=null;pending=null;operation.reset(generation,null);views.reset(generation,null);dashboard.set(null);receipt.set(null);username.set("");loggedIn.set(false);connected.set(false);busy.set(false);unknown.set(false);loginUnknown.set(false);lastRefresh.set("");}
    private void discard(MarketHttpClient prior){commands.execute(()->{try{prior.logout();}catch(Exception ignored){/* Best effort; expiry is the fallback. */}finally{prior.close();}});}
    @Override public void close(){if(closed)return;closed=true;logout();commands.shutdown();}
}
