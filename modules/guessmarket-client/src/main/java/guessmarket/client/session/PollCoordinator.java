package guessmarket.client.session;
import guessmarket.client.http.MarketHttpClient;
import guessmarket.dto.api.DashboardResponse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
public final class PollCoordinator implements AutoCloseable {
    public record Query(Integer eventId,long selection) {}
    private final MarketHttpClient client;
    private final BiConsumer<Query,DashboardResponse> accepted;
    private final BiConsumer<Query,Exception> failed;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"market-poll");t.setDaemon(true);return t;});
    private final AtomicBoolean queued=new AtomicBoolean();
    private volatile Query query=new Query(null,0);
    public PollCoordinator(MarketHttpClient client,BiConsumer<Query,DashboardResponse> accepted,BiConsumer<Query,Exception> failed){this.client=client;this.accepted=accepted;this.failed=failed;}
    public void selectEvent(Integer eventId,long selection){query=new Query(eventId,selection);}
    public void start(){worker.scheduleWithFixedDelay(this::poll,0,1,TimeUnit.SECONDS);}
    public void requestNow(){if(worker.isShutdown()||!queued.compareAndSet(false,true))return;worker.execute(()->{queued.set(false);poll();});}
    private void poll(){var captured=query;try{accepted.accept(captured,client.dashboard(captured.eventId()));}catch(Exception problem){failed.accept(captured,problem);}}
    @Override public void close(){worker.shutdownNow();}
}
