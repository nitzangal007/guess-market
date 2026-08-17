package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static guessmarket.engine.EngineDtoAssertions.assertDetailsEqual;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.MarketEventSummary;
import guessmarket.engine.xml.XmlMarketLoader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuessMarketEnginePersistenceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesCaseInsensitiveSuffixWithoutChangingPathsWithDotsOrSpaces() throws Exception {
        Path directory = temporaryDirectory.resolve("a directory.with dots");
        Files.createDirectory(directory);

        assertEquals(directory.resolve("market state.ser"),
                SavedStateStore.resolveStatePath(directory.resolve("market state")));
        assertEquals(directory.resolve("market state.SER"),
                SavedStateStore.resolveStatePath(directory.resolve("market state.SER")));
        assertEquals(directory.resolve("market.state.ser"),
                SavedStateStore.resolveStatePath(directory.resolve("market.state")));
    }

    @Test
    void rejectsInvalidAndMissingStatePathsWithStructuredFailures() {
        EngineOperationException nullPath = assertThrows(EngineOperationException.class,
                () -> SavedStateStore.resolveStatePath(null));
        EngineOperationException directoryPath = assertThrows(EngineOperationException.class,
                () -> SavedStateStore.resolveStatePath(temporaryDirectory));
        EngineOperationException missingParent = assertThrows(EngineOperationException.class,
                () -> SavedStateStore.resolveStatePath(
                        temporaryDirectory.resolve("missing").resolve("market")));

        assertEquals(EngineErrorCode.INVALID_STATE_PATH, nullPath.getCode());
        assertEquals(EngineErrorCode.INVALID_STATE_PATH, directoryPath.getCode());
        assertEquals(EngineErrorCode.INVALID_STATE_PATH, missingParent.getCode());
    }

    @Test
    void savesAndRestoresAValidatedOrderedDomainGraph() throws Exception {
        SavedStateStore store = new SavedStateStore();
        Path basePath = temporaryDirectory.resolve("state with spaces");
        MarketEvent minimum = event(Integer.MIN_VALUE, CommissionMode.ON_PURCHASE, 5, 100);
        minimum.purchase(1, 3);
        MarketEvent maximum = event(Integer.MAX_VALUE, CommissionMode.ON_CLOSE, 90, 1);
        maximum.purchase(1, 100);
        maximum.purchase(2, 1);
        maximum.close(2);

        store.save(basePath, List.of(minimum, maximum));
        Path statePath = temporaryDirectory.resolve("state with spaces.ser");
        LinkedHashMap<Integer, MarketEvent> restored = store.restore(basePath);

        assertTrue(Files.isRegularFile(statePath));
        assertIterableEquals(List.of(Integer.MIN_VALUE, Integer.MAX_VALUE), restored.keySet());
        assertDetailsEqual(minimum.toDetails(), restored.get(Integer.MIN_VALUE).toDetails());
        assertDetailsEqual(maximum.toDetails(), restored.get(Integer.MAX_VALUE).toDetails());
        assertEquals(Double.doubleToLongBits(
                        minimum.getPurchaseHistory().get(0).getBaseShareCost()),
                Double.doubleToLongBits(restored.get(Integer.MIN_VALUE)
                        .getPurchaseHistory().get(0).getBaseShareCost()));
    }

    @Test
    void overwritesExistingStateAndFallsBackWhenAtomicPublicationIsUnsupported() throws Exception {
        Path basePath = temporaryDirectory.resolve("state");
        SavedStateStore normalStore = new SavedStateStore();
        normalStore.save(basePath, List.of(event(1, CommissionMode.ON_CLOSE, 0, 5)));

        ArrayList<java.nio.file.CopyOption[]> publicationAttempts = new ArrayList<>();
        SavedStateStore fallbackStore = new SavedStateStore((source, target, options) -> {
            publicationAttempts.add(options.clone());
            if (publicationAttempts.size() == 1) {
                throw new java.nio.file.AtomicMoveNotSupportedException(
                        source.toString(), target.toString(), "test fallback");
            }
            Files.move(source, target, options);
        });
        fallbackStore.save(basePath, List.of(event(2, CommissionMode.ON_PURCHASE, 5, 5)));

        assertEquals(2, publicationAttempts.size());
        assertArrayEquals(new StandardCopyOption[] {StandardCopyOption.ATOMIC_MOVE},
                publicationAttempts.get(0));
        assertArrayEquals(new StandardCopyOption[] {StandardCopyOption.REPLACE_EXISTING},
                publicationAttempts.get(1));
        assertIterableEquals(List.of(2), fallbackStore.restore(basePath).keySet());
        try (var files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().contains(".tmp")));
        }
    }

    @Test
    void doesNotRetryPublicationWhenFailedAtomicMoveConsumesTheTemporaryFile() throws Exception {
        Path basePath = temporaryDirectory.resolve("state");
        ArrayList<java.nio.file.CopyOption[]> publicationAttempts = new ArrayList<>();
        SavedStateStore store = new SavedStateStore((source, target, options) -> {
            publicationAttempts.add(options.clone());
            Files.delete(source);
            throw new IOException("publication state unknown");
        });

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> store.save(basePath, List.of(event(1, CommissionMode.ON_CLOSE, 0, 5))));

        assertEquals(EngineErrorCode.STATE_FILE_ACCESS_FAILED, exception.getCode());
        assertEquals(1, publicationAttempts.size());
    }

    @Test
    void cleansTemporaryStateWhenFallbackPublicationFails() throws Exception {
        Path basePath = temporaryDirectory.resolve("state");
        SavedStateStore store = new SavedStateStore((source, target, options) -> {
            throw new IOException("publication failure");
        });

        assertFailure(EngineErrorCode.STATE_FILE_ACCESS_FAILED,
                () -> store.save(basePath, List.of(event(1, CommissionMode.ON_CLOSE, 0, 5))));
        try (var files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().contains(".tmp")));
        }
    }

    @Test
    void mapsMissingCorruptWrongRootAndFilteredFilesToSavedStateFailures() throws Exception {
        SavedStateStore store = new SavedStateStore();
        Path missing = temporaryDirectory.resolve("missing");
        assertFailure(EngineErrorCode.STATE_FILE_NOT_FOUND, () -> store.restore(missing));

        Path corrupt = temporaryDirectory.resolve("corrupt.ser");
        Files.write(corrupt, new byte[] {1, 2, 3});
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(corrupt));

        Path wrongRoot = temporaryDirectory.resolve("wrong-root.ser");
        writeObject(wrongRoot, new ArrayList<>());
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(wrongRoot));

        Path filtered = temporaryDirectory.resolve("filtered.ser");
        writeObject(filtered, new DisallowedPayload());
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(filtered));
    }

    @Test
    void validatorRejectsWrongVersionEmptyDuplicateAndInvalidHistoryCandidates() throws Exception {
        SavedStateValidator validator = new SavedStateValidator();
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> validator.validate(new SavedState(2, List.of(event(1, CommissionMode.ON_CLOSE, 0, 5)))));
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> validator.validate(new SavedState(1, List.of())));

        MarketEvent shared = event(1, CommissionMode.ON_CLOSE, 0, 5);
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> validator.validate(new SavedState(1, List.of(shared, shared))));
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> validator.validate(new SavedState(1, List.of(shared,
                        event(1, CommissionMode.ON_CLOSE, 0, 5)))));

        MarketEvent inconsistent = event(2, CommissionMode.ON_CLOSE, 0, 5);
        inconsistent.purchase(1, 2);
        inconsistent.getOptions().get(0).commitPurchase(1);
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> validator.validate(new SavedState(1, List.of(inconsistent))));
    }

    @Test
    void restoreQuarantinesSerializedVersionAndSemanticViolations() throws Exception {
        SavedStateStore store = new SavedStateStore();

        Path wrongVersion = temporaryDirectory.resolve("wrong-version.ser");
        writeObject(wrongVersion, new SavedState(2,
                List.of(event(1, CommissionMode.ON_CLOSE, 0, 5))));
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(wrongVersion));

        Path empty = temporaryDirectory.resolve("empty.ser");
        writeObject(empty, new SavedState(1, List.of()));
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(empty));

        MarketEvent shared = event(1, CommissionMode.ON_CLOSE, 0, 5);
        Path alias = temporaryDirectory.resolve("alias.ser");
        writeObject(alias, new SavedState(1, List.of(shared, shared)));
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(alias));

        MarketEvent inconsistent = event(2, CommissionMode.ON_CLOSE, 0, 5);
        inconsistent.purchase(1, 2);
        inconsistent.getOptions().get(0).commitPurchase(1);
        Path inconsistentPath = temporaryDirectory.resolve("inconsistent.ser");
        writeObject(inconsistentPath, new SavedState(1, List.of(inconsistent)));
        assertFailure(EngineErrorCode.SAVED_STATE_INVALID, () -> store.restore(inconsistentPath));
    }

    @Test
    void validatorRejectsPurchaseCommissionThatDoesNotMatchItsPolicyFormula() {
        MarketEvent event = event(4, CommissionMode.ON_PURCHASE, 10, 5);
        TradeRecord record = new TradeRecord(1, "same", 1, 1.0, 0.01, 1.01);
        event.getOptions().get(0).commitPurchase(1);
        event.getPurchaseHistory().add(record);
        event.getAccount().commitPurchase(1.01, 0.01);

        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> new SavedStateValidator().validate(new SavedState(1, List.of(event))));
    }

    @Test
    void restoreRejectsSerializedEventNameThatIsNotMapperCanonical() throws Exception {
        MarketEvent event = new MarketEvent(5, "  team\talpha   beta  ", "", CommissionMode.ON_CLOSE,
                0, 5, "same", "same");
        Path path = temporaryDirectory.resolve("noncanonical-name.ser");
        writeObject(path, new SavedState(1, List.of(event)));

        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> new SavedStateStore().restore(path));
    }

    @Test
    void restoreRejectsSerializedDescriptionWithOuterWhitespace() throws Exception {
        MarketEvent event = new MarketEvent(6, "name", " description ", CommissionMode.ON_CLOSE,
                0, 5, "same", "same");
        Path path = temporaryDirectory.resolve("noncanonical-description.ser");
        writeObject(path, new SavedState(1, List.of(event)));

        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> new SavedStateStore().restore(path));
    }

    @Test
    void restoreRejectsSerializedOptionLabelWithOuterWhitespace() throws Exception {
        MarketEvent event = new MarketEvent(7, "name", "", CommissionMode.ON_CLOSE,
                0, 5, " first ", "second");
        Path path = temporaryDirectory.resolve("noncanonical-option.ser");
        writeObject(path, new SavedState(1, List.of(event)));

        assertFailure(EngineErrorCode.SAVED_STATE_INVALID,
                () -> new SavedStateStore().restore(path));
    }

    @Test
    void savesAndRestoresUsingBareRelativeBasePath() throws Exception {
        Path basePath = Path.of("task9-relative-" + UUID.randomUUID());
        Path statePath = basePath.toAbsolutePath().normalize().resolveSibling(
                basePath.getFileName() + ".ser");
        SavedStateStore store = new SavedStateStore();
        try {
            store.save(basePath, List.of(event(8, CommissionMode.ON_CLOSE, 0, 5)));

            assertTrue(Files.isRegularFile(statePath));
            assertIterableEquals(List.of(8), store.restore(basePath).keySet());
        } finally {
            Files.deleteIfExists(statePath);
        }
    }

    @Test
    void publicEngineRoundTripRestoresCompleteStateIntoANewInstanceAndContinues()
            throws Exception {
        MarketEvent minimum = event(Integer.MIN_VALUE, CommissionMode.ON_PURCHASE, 5, 100);
        MarketEvent maximum = event(Integer.MAX_VALUE, CommissionMode.ON_CLOSE, 90, 5);
        GuessMarketEngine source = engineWithEvents(new SavedStateStore(), minimum, maximum);
        source.purchaseShares(Integer.MIN_VALUE, 1, 3);
        source.purchaseShares(Integer.MIN_VALUE, 2, 2);
        source.purchaseShares(Integer.MAX_VALUE, 1, 7);
        source.closeEvent(Integer.MAX_VALUE, 1);
        Path statePath = temporaryDirectory.resolve("public restart state");

        source.saveState(statePath);
        GuessMarketEngine restarted = new GuessMarketEngineImpl();
        int restoredCount = restarted.restoreState(statePath);

        assertAll(
                () -> assertEquals(2, restoredCount),
                () -> assertIterableEquals(
                        List.of(Integer.MIN_VALUE, Integer.MAX_VALUE),
                        restarted.listEvents().stream()
                                .map(MarketEventSummary::getEventId)
                                .toList()));
        assertDetailsEqual(source.getEventDetails(Integer.MIN_VALUE),
                restarted.getEventDetails(Integer.MIN_VALUE));
        assertDetailsEqual(source.getEventDetails(Integer.MAX_VALUE),
                restarted.getEventDetails(Integer.MAX_VALUE));

        restarted.purchaseShares(Integer.MIN_VALUE, 1, 4);
        MarketEventDetails continued = restarted.closeEvent(Integer.MIN_VALUE, 2);

        assertAll(
                () -> assertEquals(7, continued.getOptions().get(0).getShareQuantity()),
                () -> assertEquals(2, continued.getOptions().get(1).getShareQuantity()),
                () -> assertEquals(3, continued.getPurchaseHistory().size()),
                () -> assertEquals(EventStatus.CLOSED, continued.getStatus()),
                () -> assertEquals(2, continued.getWinningOptionNumber().orElseThrow()));
    }

    @Test
    void successfulPublicRestoreReplacesRatherThanMergesThePriorLiveSystem() throws Exception {
        GuessMarketEngine savedSource = engineWithEvents(
                new SavedStateStore(),
                event(22, CommissionMode.ON_CLOSE, 0, 5));
        Path statePath = temporaryDirectory.resolve("replacement");
        savedSource.saveState(statePath);

        MarketEvent prior = event(11, CommissionMode.ON_PURCHASE, 5, 5);
        GuessMarketEngine target = engineWithEvents(new SavedStateStore(), prior);
        target.purchaseShares(11, 1, 2);

        int count = target.restoreState(statePath);

        assertEquals(1, count);
        assertIterableEquals(
                List.of(22),
                target.listEvents().stream().map(MarketEventSummary::getEventId).toList());
        EngineOperationException missingPrior = assertThrows(
                EngineOperationException.class,
                () -> target.getEventDetails(11));
        assertEquals(EngineErrorCode.EVENT_NOT_FOUND, missingPrior.getCode());
        assertEquals(0, target.getEventDetails(22).getPurchaseHistory().size());
    }

    @Test
    void everyFailedPublicRestorePreservesTheCompletePriorLiveSystem() throws Exception {
        MarketEvent prior = event(31, CommissionMode.ON_PURCHASE, 5, 100);
        GuessMarketEngine engine = engineWithEvents(new SavedStateStore(), prior);
        engine.purchaseShares(31, 1, 6);
        MarketEventDetails before = engine.getEventDetails(31);

        Path corrupt = temporaryDirectory.resolve("corrupt-public.ser");
        Files.write(corrupt, new byte[] {1, 2, 3});
        Path wrongRoot = temporaryDirectory.resolve("wrong-root-public.ser");
        writeObject(wrongRoot, new ArrayList<>());
        Path wrongVersion = temporaryDirectory.resolve("wrong-version-public.ser");
        writeObject(wrongVersion, new SavedState(2,
                List.of(event(9, CommissionMode.ON_CLOSE, 0, 5))));
        Path empty = temporaryDirectory.resolve("empty-public.ser");
        writeObject(empty, new SavedState(1, List.of()));

        List<RestoreFailure> failures = List.of(
                new RestoreFailure(temporaryDirectory, EngineErrorCode.INVALID_STATE_PATH),
                new RestoreFailure(
                        temporaryDirectory.resolve("missing-public"),
                        EngineErrorCode.STATE_FILE_NOT_FOUND),
                new RestoreFailure(corrupt, EngineErrorCode.SAVED_STATE_INVALID),
                new RestoreFailure(wrongRoot, EngineErrorCode.SAVED_STATE_INVALID),
                new RestoreFailure(wrongVersion, EngineErrorCode.SAVED_STATE_INVALID),
                new RestoreFailure(empty, EngineErrorCode.SAVED_STATE_INVALID));

        for (RestoreFailure restoreFailure : failures) {
            EngineOperationException exception = assertThrows(
                    EngineOperationException.class,
                    () -> engine.restoreState(restoreFailure.path()));

            assertEquals(restoreFailure.code(), exception.getCode());
            assertIterableEquals(
                    List.of(31),
                    engine.listEvents().stream().map(MarketEventSummary::getEventId).toList());
            assertDetailsEqual(before, engine.getEventDetails(31));
        }
    }

    @Test
    void failedPublicSavePreservesTheRunningSystem() throws Exception {
        SavedStateStore failingStore = new SavedStateStore((source, target, options) -> {
            throw new IOException("simulated publication failure");
        });
        MarketEvent prior = event(41, CommissionMode.ON_CLOSE, 0, 5);
        GuessMarketEngine engine = engineWithEvents(failingStore, prior);
        engine.purchaseShares(41, 2, 3);
        MarketEventDetails before = engine.getEventDetails(41);

        EngineOperationException exception = assertThrows(
                EngineOperationException.class,
                () -> engine.saveState(temporaryDirectory.resolve("failed-public-save")));

        assertEquals(EngineErrorCode.STATE_FILE_ACCESS_FAILED, exception.getCode());
        assertDetailsEqual(before, engine.getEventDetails(41));
    }

    @Test
    void stateFileAccessFailureFromInjectedStorePreservesTheCompletePriorPublicState()
            throws Exception {
        Path attemptedPath = temporaryDirectory.resolve("inaccessible-state.ser");
        EngineOperationException injectedFailure = new EngineOperationException(
                EngineErrorCode.STATE_FILE_ACCESS_FAILED,
                "Simulated restore access failure",
                "Use an accessible saved state file",
                attemptedPath,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        MarketEvent prior = event(51, CommissionMode.ON_PURCHASE, 9, 100);
        LinkedHashMap<Integer, MarketEvent> initialEvents = new LinkedHashMap<>();
        initialEvents.put(prior.getEventId(), prior);
        GuessMarketEngine engine = new GuessMarketEngineImpl(
                path -> {
                    throw new AssertionError("Unexpected XML load");
                },
                new GuessMarketEngineImpl.StateStore() {
                    @Override
                    public void save(Path path, java.util.Collection<MarketEvent> events) {
                        throw new AssertionError("Unexpected save");
                    }

                    @Override
                    public Map<Integer, MarketEvent> restore(Path path)
                            throws EngineOperationException {
                        throw injectedFailure;
                    }
                },
                initialEvents);
        engine.purchaseShares(51, 1, 4);
        MarketEventDetails before = engine.getEventDetails(51);

        EngineOperationException actual = assertThrows(
                EngineOperationException.class,
                () -> engine.restoreState(attemptedPath));

        assertAll(
                () -> assertEquals(EngineErrorCode.STATE_FILE_ACCESS_FAILED, actual.getCode()),
                () -> assertEquals(attemptedPath, actual.getPath().orElseThrow()),
                () -> assertIterableEquals(
                        List.of(51),
                        engine.listEvents().stream().map(MarketEventSummary::getEventId).toList()));
        assertDetailsEqual(before, engine.getEventDetails(51));
    }

    private static MarketEvent event(int eventId, CommissionMode mode, int percentage, int b) {
        return new MarketEvent(eventId, "", "", mode, percentage, b, "same", "same");
    }

    private static GuessMarketEngine engineWithEvents(
            SavedStateStore store,
            MarketEvent... events) {
        LinkedHashMap<Integer, MarketEvent> initialEvents = new LinkedHashMap<>();
        for (MarketEvent event : events) {
            initialEvents.put(event.getEventId(), event);
        }
        return new GuessMarketEngineImpl(new XmlMarketLoader(), store, initialEvents);
    }

    private static void writeObject(Path path, Object object) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
            output.writeObject(object);
        }
    }

    private static void assertFailure(EngineErrorCode expected, ThrowingOperation operation) {
        EngineOperationException exception = assertThrows(EngineOperationException.class, operation::run);
        assertEquals(expected, exception.getCode());
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }

    private record RestoreFailure(Path path, EngineErrorCode code) {
    }

    private static final class DisallowedPayload implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }
}
