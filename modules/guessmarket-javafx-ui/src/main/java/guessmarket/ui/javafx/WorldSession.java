package guessmarket.ui.javafx;

import guessmarket.dto.world.OpeningPreview;
import guessmarket.dto.world.WorldSnapshot;
import guessmarket.dto.world.PurchasePreview;
import guessmarket.dto.world.PurchaseResult;
import guessmarket.dto.world.ClosePreview;
import guessmarket.dto.world.CloseResult;
import guessmarket.dto.world.OrderRequest;
import guessmarket.dto.world.OrderPreview;
import guessmarket.dto.world.OrderResult;
import guessmarket.dto.world.OrderBookConfiguration;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.GuessMarketWorldEngine;
import guessmarket.engine.WorldCommandException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

/** UI-owned serialized commands. All public operations and observations belong to the FX thread. */
final class WorldSession implements AutoCloseable {
    private final GuessMarketWorldEngine engine;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "guess-market-world-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyStringWrapper loadedPath = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper error = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<WorldSnapshot> world =
            new ReadOnlyObjectWrapper<>(new WorldSnapshot(List.of(), List.of()));
    private Task<?> current;
    private OpeningPreview pendingOpening;
    private PurchasePreview pendingPurchase;
    private ClosePreview pendingClose;
    private OrderPreview pendingOrder;
    private final ReadOnlyStringWrapper notice = new ReadOnlyStringWrapper("");
    private boolean closed;
    private boolean preserveSelections;
    private boolean transientError;

    WorldSession(GuessMarketWorldEngine engine) { this.engine = Objects.requireNonNull(engine); }
    void load(Path path) {
        requireFxThread();
        if (path == null || !begin(false)) return;
        Path absolute = path.toAbsolutePath().normalize();
        Task<WorldSnapshot> task = new Task<>() {
            @Override protected WorldSnapshot call() throws Exception {
                for (int step = 0; step < 12; step++) {
                    if (isCancelled()) return null;
                    updateProgress(step, 12);
                    Thread.sleep(100);
                }
                if (isCancelled()) return null;
                updateProgress(-1, 1);
                return engine.loadWorldFromXml(absolute);
            }
        };
        execute(task, snapshot -> {
            loadedPath.set(absolute.toString());
            world.set(snapshot);
            finish();
        });
    }
    void previewOpening(String actingUser, int eventId, Consumer<OpeningPreview> showDialog) {
        requireFxThread();
        if (!begin(true)) return;
        boolean book=isOrderBook(eventId);
        execute(new Task<OpeningPreview>() {
            @Override protected OpeningPreview call() throws Exception {
                return book?engine.previewOrderBookOpening(actingUser,eventId):engine.previewLmsrOpening(actingUser, eventId);
            }
        }, preview -> {
            pendingOpening = preview;
            // Keep busy through the confirmation so load and selection controls cannot compete.
            showDialog.accept(preview);
        });
    }
    void completeOpening(boolean confirmed) {
        requireFxThread();
        if (closed || pendingOpening == null) return;
        OpeningPreview preview = pendingOpening;
        pendingOpening = null;
        if (!confirmed) { finish(); return; }
        boolean book=isOrderBook(preview.eventId());
        execute(new Task<WorldSnapshot>() {
            @Override protected WorldSnapshot call() throws Exception {
                return book?engine.openOrderBookEvent(preview.actingUser(),preview.eventId(),preview.worldRevision()):
                        engine.openLmsrEvent(preview.actingUser(), preview.eventId(), preview.worldRevision());
            }
        }, snapshot -> {
            preserveSelections = true;
            try { world.set(snapshot); }
            finally { preserveSelections = false; finish(); }
        });
    }
    boolean preserveSelections() { return preserveSelections; }
    void previewPurchase(String user,int eventId,int option,int quantity,Consumer<PurchasePreview> showDialog) {
        requireFxThread();
        if (!begin(true)) return;
        execute(new Task<PurchasePreview>() {
            @Override protected PurchasePreview call() throws Exception {
                return engine.previewLmsrPurchase(user,eventId,option,quantity);
            }
        }, preview -> { pendingPurchase=preview; showDialog.accept(preview); });
    }
    void completePurchase(boolean confirmed) {
        requireFxThread();
        if (closed || pendingPurchase==null) return;
        PurchasePreview preview=pendingPurchase;
        pendingPurchase=null;
        if (!confirmed) { finish(); return; }
        execute(new Task<PurchaseResult>() {
            @Override protected PurchaseResult call() throws Exception {
                return engine.purchaseLmsrShares(preview.actingUser(),preview.eventId(),preview.optionNumber(),
                        preview.quantity(),preview.worldRevision());
            }
        }, result -> {
            notice.set("Purchase completed for "+result.receipt().userName()+": "
                    +result.receipt().quantity()+" shares, total paid "+EventDetailsView.money(result.receipt().totalPaid())
                    +(result.newlyBlocked()?". Buying and opening are blocked until a receipt leaves the balance above zero. You can inspect the account and close an event you own.":"."));
            publishMutation(result.world());
        });
    }
    void previewClose(String user,int eventId,int winner,Consumer<ClosePreview> showDialog) {
        requireFxThread();
        if (!begin(true)) return;
        boolean book=isOrderBook(eventId);
        execute(new Task<ClosePreview>() {
            @Override protected ClosePreview call() throws Exception { return book?engine.previewOrderBookClose(user,eventId,winner):engine.previewLmsrClose(user,eventId,winner); }
        }, preview -> { pendingClose=preview; showDialog.accept(preview); });
    }
    void completeClose(boolean confirmed) {
        requireFxThread();
        if (closed || pendingClose==null) return;
        ClosePreview preview=pendingClose;
        pendingClose=null;
        if (!confirmed) { finish(); return; }
        boolean book=isOrderBook(preview.eventId());
        execute(new Task<CloseResult>() {
            @Override protected CloseResult call() throws Exception {
                return book?engine.closeOrderBookEvent(preview.actingUser(),preview.eventId(),preview.winningOption(),preview.worldRevision()):
                        engine.closeLmsrEvent(preview.actingUser(),preview.eventId(),preview.winningOption(),preview.worldRevision());
            }
        }, result -> {
            notice.set("Event closed. Winner: "+result.settlement().winningLabel()+". Payouts and market-maker receipts are complete.");
            publishMutation(result.world());
        });
    }
    private void publishMutation(WorldSnapshot snapshot) {
        preserveSelections=true;
        try { world.set(snapshot); }
        finally { preserveSelections=false; finish(); }
    }
    void previewOrder(OrderRequest request,Consumer<OrderPreview> showDialog){
        requireFxThread();if(!begin(true))return;
        execute(new Task<OrderPreview>(){
            @Override protected OrderPreview call()throws Exception{return engine.previewOrder(request);}
        },preview->{pendingOrder=preview;showDialog.accept(preview);});
    }
    void completeOrder(boolean confirmed){
        requireFxThread();if(closed||pendingOrder==null)return;
        var preview=pendingOrder;pendingOrder=null;
        if(!confirmed){finish();return;}
        execute(new Task<OrderResult>(){
            @Override protected OrderResult call()throws Exception{return engine.submitOrder(preview.request(),preview.worldRevision());}
        },result->{
            var execution=result.execution();
            notice.set("Order completed for "+execution.request().userName()+". Final cash: "+EventDetailsView.money(execution.balanceAfter())
                    +". Unfilled quantity: "+execution.incomingRemaining()+". Cancelled waiting orders across affected accounts: "+execution.cancelledOrderCount()
                    +(execution.blockedAfter()?". This account is blocked until a positive receipt leaves cash above zero.":"."));
            publishMutation(result.world());
        });
    }
    private boolean isOrderBook(int id){return world.get().events().stream().anyMatch(e->e.id()==id&&e.pricing() instanceof OrderBookConfiguration);}
    private boolean begin(boolean commandFeedback) {
        if (busy.get() || closed) return false;
        error.set("");
        notice.set("");
        transientError=commandFeedback;
        busy.set(true);
        return true;
    }
    private <T> void execute(Task<T> task, Consumer<T> success) {
        current = task;
        progress.unbind();
        progress.bind(task.progressProperty());
        task.setOnSucceeded(event -> {
            if (closed) return;
            try { success.accept(task.getValue()); }
            catch (RuntimeException failure) { fail(failure); }
        });
        task.setOnFailed(event -> { if (!closed) fail(task.getException()); });
        task.setOnCancelled(event -> { if (!closed) finish(); });
        worker.execute(task);
    }
    private void fail(Throwable failure) {
        if (failure instanceof EngineOperationException operation)
            error.set(operation.getDetail() + "\n" + operation.getRecoveryHint());
        else if (failure instanceof WorldCommandException command)
            error.set(command.getMessage());
        else {
            error.set("Unable to complete this operation. Please try again.");
            failure.printStackTrace(System.err);
        }
        finish();
    }
    private void finish() {
        progress.unbind();
        progress.set(0);
        busy.set(false);
        current = null;
        pendingOpening = null;
        pendingPurchase = null;
        pendingClose = null;
        pendingOrder = null;
    }
    @Override public void close() {
        requireFxThread();
        if (closed) return;
        closed = true;
        if (current != null) current.cancel(true);
        worker.shutdownNow();
        finish();
    }
    ReadOnlyBooleanProperty busyProperty() { return busy.getReadOnlyProperty(); }
    ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    ReadOnlyStringProperty loadedPathProperty() { return loadedPath.getReadOnlyProperty(); }
    ReadOnlyStringProperty errorProperty() { return error.getReadOnlyProperty(); }
    ReadOnlyStringProperty noticeProperty() { return notice.getReadOnlyProperty(); }
    void dismissNotice() { requireFxThread(); notice.set(""); }
    void dismissTransientFeedback() {
        requireFxThread();
        notice.set("");
        if(transientError)error.set("");
    }
    ReadOnlyObjectProperty<WorldSnapshot> worldProperty() { return world.getReadOnlyProperty(); }
    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("Use the FX application thread");
    }
}
