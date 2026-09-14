package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class OpeningFlowTest {
    @BeforeAll static void startFx() throws Exception { FxTestSupport.start(); }
    private Parent open(WorldSession session, AtomicReference<Stage> ref) throws Exception {
        return FxTestSupport.fx(() -> {
            var loader = new FXMLLoader(getClass().getResource("/guessmarket/ui/javafx/main.fxml"));
            loader.setControllerFactory(type -> {
                try { return type.getDeclaredConstructor(WorldSession.class).newInstance(session); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            });
            Parent root = loader.load();
            Stage stage = new Stage(); ref.set(stage);
            stage.setScene(new Scene(root, 1120, 820)); stage.show();
            return root;
        });
    }
    private static DialogPane dialog() {
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.getScene() != null
                    && window.getScene().getRoot() instanceof DialogPane pane
                    && "openingDialog".equals(pane.getId())) return pane;
        }
        return null;
    }
    private static DialogPane awaitDialog() throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            DialogPane pane = FxTestSupport.fx(OpeningFlowTest::dialog);
            if (pane != null) return pane;
            Thread.sleep(20);
        }
        throw new AssertionError("Opening dialog did not appear");
    }
    private static String text(Parent root, String id) { return ((Label)root.lookup("#" + id)).getText(); }
    private static void save(Parent root, String name) throws Exception {
        root.applyCss(); root.layout();
        var image = root.snapshot(null, null);
        var png = new java.awt.image.BufferedImage((int)image.getWidth(), (int)image.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++)
            for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        Path path = Path.of("private/coordination/assignment-2/ui-evidence-terminal", name);
        java.nio.file.Files.createDirectories(path.getParent());
        assertTrue(javax.imageio.ImageIO.write(png, "png", path.toFile()));
    }
    private static void users(Parent root) {
        ((TabPane)root.lookup("#navigation")).getSelectionModel().select(1);
        root.applyCss(); root.layout();
        FxTestSupport.chooseUser(root,1);
        FxTestSupport.viewUser(root,"ownedSection",1);
        root.applyCss(); root.layout();
    }
    @Test void dialogCancelEscapeCloseAndConfirmAreRealAndRefreshBothViews() throws Exception {
        var engine = new GuessMarketWorldEngineImpl();
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(engine));
        AtomicReference<Stage> stage = new AtomicReference<>();
        try {
            Parent root = open(session, stage);
            FxTestSupport.fx(() -> { session.load(Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/small.xml")); return null; });
            FxTestSupport.awaitIdle(session);
            var before = engine.getWorldSnapshot();
            FxTestSupport.fx(() -> { ((ToggleButton)root.lookup("#event-card-2")).fire(); users(root); return null; });
            for (int cancelMode = 0; cancelMode < 3; cancelMode++) {
                FxTestSupport.fx(() -> { ((Button)root.lookup("#openEvent")).fire(); return null; });
                DialogPane pane = awaitDialog();
                int mode = cancelMode;
                FxTestSupport.fx(() -> {
                    assertTrue(session.busyProperty().get());
                    assertTrue(root.lookup("#loadButton").isDisabled());
                    assertTrue(root.lookup("#changeUser").isDisabled());
                    assertEquals("69.31", text(pane, "openingFunding"));
                    assertEquals("9,930.69", text(pane, "openingAfter"));
                    assertEquals("Tikva", text(pane, "openingUser"));
                    if (mode == 0) ((Button)pane.lookupButton(ButtonType.CANCEL)).fire();
                    if (mode == 1) Event.fireEvent(pane, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
                    if (mode == 2) Event.fireEvent(pane.getScene().getWindow(), new WindowEvent(pane.getScene().getWindow(), WindowEvent.WINDOW_CLOSE_REQUEST));
                    return null;
                });
                FxTestSupport.awaitIdle(session);
                assertEquals(before, engine.getWorldSnapshot());
            }
            FxTestSupport.fx(() -> { ((Button)root.lookup("#openEvent")).fire(); return null; });
            DialogPane pane = awaitDialog();
            FxTestSupport.fx(() -> {
                save(pane, "confirmation.png");
                var confirm = pane.getButtonTypes().stream().filter(b -> b != ButtonType.CANCEL).findFirst().orElseThrow();
                ((Button)pane.lookupButton(confirm)).fire();
                return null;
            });
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(() -> {
                root.applyCss(); root.layout();
                assertEquals("Tikva", text(root, "userName"));
                assertEquals("9,930.69", text(root, "userBalance"));
                assertEquals("Mujtaba is Dead", text(root, "user-detailName"));
                assertEquals("69.31", text(root, "user-contractBalance"));
                assertNull(root.lookup("#openEvent"));
                save(root, "opened-users.png");
                ((TabPane)root.lookup("#navigation")).getSelectionModel().select(0);
                root.applyCss(); root.layout();
                assertEquals("World Cap Winner", text(root, "detailName"));
                ((Button)root.lookup("#eventsBack")).fire();
                ((ToggleButton)root.lookup("#event-card-1")).fire();
                assertEquals("Mujtaba is Dead", text(root, "detailName"));
                assertEquals("69.31", text(root, "contractBalance"));
                assertTrue(text(root, "eventState").contains("Active"));
                return null;
            });
        } finally { FxTestSupport.fx(() -> { session.close(); if (stage.get() != null) stage.get().close(); return null; }); }
    }
    @Test void staleConfirmationShowsRejectionAndPreservesSelections() throws Exception {
        var engine = new GuessMarketWorldEngineImpl();
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(engine));
        AtomicReference<Stage> stage = new AtomicReference<>();
        try {
            Parent root = open(session, stage);
            Path file = Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures/multiple.xml");
            FxTestSupport.fx(() -> { session.load(file); return null; });
            FxTestSupport.awaitIdle(session);
            FxTestSupport.fx(() -> { users(root); ((ToggleButton)root.lookup("#user-event-4")).fire(); ((Button)root.lookup("#openEvent")).fire(); return null; });
            DialogPane pane = awaitDialog();
            var sameWorld = engine.loadWorldFromXml(file); // External API replacement makes preview stale.
            FxTestSupport.fx(() -> {
                ((Button)pane.lookupButton(pane.getButtonTypes().stream().filter(b -> b != ButtonType.CANCEL).findFirst().orElseThrow())).fire();
                return null;
            });
            FxTestSupport.awaitIdle(session);
            assertEquals(sameWorld, engine.getWorldSnapshot());
            FxTestSupport.fx(() -> {
                assertEquals("Tikva", text(root, "userName"));
                assertEquals("Will it rain tomorrow ?", text(root, "user-detailName"));
                assertTrue(session.errorProperty().get().contains("changed"));
                return null;
            });
        } finally { FxTestSupport.fx(() -> { session.close(); if (stage.get() != null) stage.get().close(); return null; }); }
    }
}
