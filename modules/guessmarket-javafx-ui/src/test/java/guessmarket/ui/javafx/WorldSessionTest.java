package guessmarket.ui.javafx;

import guessmarket.engine.GuessMarketWorldEngineImpl;
import guessmarket.engine.GuessMarketWorldEngine;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class WorldSessionTest {
    @Test void openingRunsOffFxAndRejectsCompetingCommandsWhileBusy() throws Exception {
        var real = new GuessMarketWorldEngineImpl();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger opens = new AtomicInteger(), previews = new AtomicInteger();
        AtomicBoolean offFx = new AtomicBoolean(true);
        GuessMarketWorldEngine observed = (GuessMarketWorldEngine)Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{GuessMarketWorldEngine.class}, (proxy, method, args) -> {
                    if (method.getName().equals("previewLmsrOpening")) {
                        previews.incrementAndGet();
                        offFx.compareAndSet(true, !Platform.isFxApplicationThread());
                    }
                    if (method.getName().equals("openLmsrEvent")) {
                        opens.incrementAndGet();
                        offFx.compareAndSet(true, !Platform.isFxApplicationThread());
                        entered.countDown();
                        if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test release timeout");
                    }
                    try { return method.invoke(real, args); }
                    catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(observed));
        try {
            FxTestSupport.fx(() -> { session.load(fixture("small.xml")); return null; });
            FxTestSupport.awaitIdle(session);
            String path = FxTestSupport.fx(() -> session.loadedPathProperty().get());
            FxTestSupport.fx(() -> {
                session.previewOpening("Tikva", 1, preview -> session.completeOpening(true));
                return null;
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            FxTestSupport.fx(() -> {
                assertTrue(session.busyProperty().get());
                session.load(fixture("multiple.xml"));
                session.previewOpening("Tikva", 1, preview -> fail("Competing preview accepted"));
                session.completeOpening(true);
                return null;
            });
            release.countDown();
            FxTestSupport.awaitIdle(session);
            assertEquals(1, opens.get());
            assertEquals(1, previews.get());
            assertTrue(offFx.get());
            FxTestSupport.fx(() -> {
                assertEquals(path, session.loadedPathProperty().get());
                assertEquals(2, session.worldProperty().get().events().size());
                assertEquals(guessmarket.dto.world.WorldEventStatus.ACTIVE,
                        session.worldProperty().get().events().getFirst().status());
                return null;
            });
        } finally { release.countDown(); FxTestSupport.fx(() -> { session.close(); return null; }); }
    }
    @TempDir Path directory;
    @BeforeAll static void startFx() throws Exception { FxTestSupport.start(); }
    private Path fixture(String name) {
        return Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures", name);
    }
    @Test void validSpacedLoadThenInvalidReloadRetainsWorldAndPath() throws Exception {
        Path input = Files.copy(fixture("multiple.xml"), directory.resolve("valid world.xml"));
        GuessMarketWorldEngineImpl engine = new GuessMarketWorldEngineImpl();
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(engine));
        try {
            FxTestSupport.fx(() -> {session.load(input); return null;});
            assertTrue(FxTestSupport.fx(() -> session.busyProperty().get()));
            // FX event queue must keep serving work while the worker delay runs.
            assertTrue(FxTestSupport.fx(Platform::isFxApplicationThread));
            FxTestSupport.awaitIdle(session);
            var world = FxTestSupport.fx(() -> session.worldProperty().get());
            assertEquals(4, world.events().size());
            assertEquals(input.toAbsolutePath().normalize().toString(),
                    FxTestSupport.fx(() -> session.loadedPathProperty().get()));
            FxTestSupport.fx(() -> {session.load(fixture("error-3.xml")); return null;});
            FxTestSupport.awaitIdle(session);
            assertSame(world, FxTestSupport.fx(() -> session.worldProperty().get()));
            assertEquals(input.toAbsolutePath().normalize().toString(),
                    FxTestSupport.fx(() -> session.loadedPathProperty().get()));
            assertFalse(FxTestSupport.fx(() -> session.errorProperty().get()).isBlank());
            assertEquals(4, engine.getWorldSnapshot().events().size());
        } finally { FxTestSupport.fx(() -> {session.close(); return null;}); }
    }
    @Test void cancelDoesNothingAndDuplicateRequestCannotReplacePendingLoad() throws Exception {
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(new GuessMarketWorldEngineImpl()));
        try {
            FxTestSupport.fx(() -> {session.load(null); return null;});
            assertFalse(FxTestSupport.fx(() -> session.busyProperty().get()));
            assertTrue(FxTestSupport.fx(() -> session.loadedPathProperty().get()).isEmpty());
            FxTestSupport.fx(() -> {
                session.load(fixture("multiple.xml"));
                session.load(fixture("error-3.xml"));
                return null;
            });
            FxTestSupport.awaitIdle(session);
            assertEquals(4, FxTestSupport.fx(() -> session.worldProperty().get()).events().size());
            var world = FxTestSupport.fx(() -> session.worldProperty().get());
            FxTestSupport.fx(() -> {session.load(null); return null;});
            assertSame(world, FxTestSupport.fx(() -> session.worldProperty().get()));
            assertTrue(FxTestSupport.fx(() -> session.errorProperty().get()).isEmpty());
        } finally {FxTestSupport.fx(() -> {session.close(); return null;});}
    }
    @Test void closingDuringLoadPreventsCompletionAndRejectsFurtherLoads() throws Exception {
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(new GuessMarketWorldEngineImpl()));
        FxTestSupport.fx(() -> {session.load(fixture("multiple.xml")); session.close(); return null;});
        Thread.sleep(1400);
        assertTrue(FxTestSupport.fx(() -> session.worldProperty().get()).events().isEmpty());
        assertFalse(FxTestSupport.fx(() -> session.busyProperty().get()));
        FxTestSupport.fx(() -> {session.load(fixture("multiple.xml")); return null;});
        assertFalse(FxTestSupport.fx(() -> session.busyProperty().get()));
    }

    @Test void closeDuringEngineCallUsesDaemonWorkerAndSuppressesLateCompletion() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        CountDownLatch returned = new CountDownLatch(1);
        AtomicBoolean daemon = new AtomicBoolean();
        AtomicBoolean offFx = new AtomicBoolean();
        AtomicInteger loadCalls = new AtomicInteger();
        GuessMarketWorldEngineImpl real = new GuessMarketWorldEngineImpl();
        GuessMarketWorldEngine observed = (GuessMarketWorldEngine)Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{GuessMarketWorldEngine.class}, (proxy, method, args) -> {
                    if (method.getName().equals("loadWorldFromXml")) {
                        loadCalls.incrementAndGet();
                        daemon.set(Thread.currentThread().isDaemon());
                        offFx.set(!Platform.isFxApplicationThread());
                        entered.countDown();
                        // Simulate an I/O operation that does not honor interruption immediately.
                        boolean released = false;
                        while (!released) {
                            try { released = release.await(5, TimeUnit.SECONDS); }
                            catch (InterruptedException ignored) { /* release remains the owner */ }
                        }
                    }
                    try { return method.invoke(real, args); }
                    catch (InvocationTargetException failure) {throw failure.getCause();}
                    finally {if (method.getName().equals("loadWorldFromXml")) returned.countDown();}
                });
        WorldSession session = FxTestSupport.fx(() -> new WorldSession(observed));
        try {
            FxTestSupport.fx(() -> {session.load(fixture("multiple.xml")); return null;});
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            FxTestSupport.fx(() -> {session.load(fixture("error-3.xml")); session.close(); return null;});
            assertTrue(daemon.get());
            assertTrue(offFx.get());
            assertEquals(1, loadCalls.get());
            assertFalse(FxTestSupport.fx(() -> session.busyProperty().get()));
        } finally { release.countDown(); FxTestSupport.fx(() -> {session.close(); return null;}); }
        assertTrue(returned.await(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertTrue(FxTestSupport.fx(() -> session.worldProperty().get()).events().isEmpty());
    }
}
