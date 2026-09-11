package guessmarket.ui.javafx;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

final class FxTestSupport {
    private static boolean started;
    static synchronized void start() throws Exception {
        if (started) return;
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        if (!ready.await(10, TimeUnit.SECONDS)) throw new AssertionError("FX startup timeout");
        started = true;
    }
    static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
    static void awaitIdle(WorldSession session) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (fx(() -> session.busyProperty().get())) {
            if (System.nanoTime() > deadline) throw new AssertionError("Load timeout");
            Thread.sleep(20);
        }
    }
    static void chooseUser(javafx.scene.Parent root,int index){
        ((javafx.scene.control.Button)root.lookup("#changeUser")).fire();
        var pane=(javafx.scene.control.DialogPane)javafx.stage.Window.getWindows().stream()
                .filter(w->w.getScene()!=null&&"userPicker".equals(w.getScene().getRoot().getId()))
                .findFirst().orElseThrow().getScene().getRoot();
        ((javafx.scene.control.ListView<?>)pane.lookup("#userList")).getSelectionModel().select(index);
        var confirm=pane.getButtonTypes().stream().filter(b->b!=javafx.scene.control.ButtonType.CANCEL).findFirst().orElseThrow();
        ((javafx.scene.control.Button)pane.lookupButton(confirm)).fire();root.applyCss();root.layout();
    }
    static void viewUser(javafx.scene.Parent root,String section,int id){
        ((javafx.scene.control.ToggleButton)root.lookup("#"+section)).fire();
        ((javafx.scene.control.ToggleButton)root.lookup("#user-event-"+id)).fire();root.applyCss();root.layout();
    }
    static void userDetail(javafx.scene.Parent root,String section){
        var tabs=(javafx.scene.control.TabPane)root.lookup("#user-eventSections");
        tabs.getSelectionModel().select(tabs.getTabs().stream().filter(t->t.getText().equals(section)).findFirst().orElseThrow());
        root.applyCss();root.layout();
    }
}
