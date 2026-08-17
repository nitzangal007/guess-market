package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.dto.CommissionMode;
import guessmarket.dto.MarketEventDetails;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static MarketEvent event(int eventId, CommissionMode mode, int percentage, int b) {
        return new MarketEvent(eventId, "", "", mode, percentage, b, "same", "same");
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

    private static void assertDetailsEqual(MarketEventDetails expected, MarketEventDetails actual) {
        assertEquals(expected.getEventId(), actual.getEventId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getCommissionMode(), actual.getCommissionMode());
        assertEquals(expected.getCommissionPercentage(), actual.getCommissionPercentage());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getOptions().size(), actual.getOptions().size());
        for (int index = 0; index < expected.getOptions().size(); index++) {
            assertEquals(expected.getOptions().get(index).getOptionNumber(),
                    actual.getOptions().get(index).getOptionNumber());
            assertEquals(expected.getOptions().get(index).getLabel(),
                    actual.getOptions().get(index).getLabel());
            assertEquals(expected.getOptions().get(index).getShareQuantity(),
                    actual.getOptions().get(index).getShareQuantity());
        }
        assertEquals(Double.doubleToLongBits(expected.getEventAccountBalance()),
                Double.doubleToLongBits(actual.getEventAccountBalance()));
        assertEquals(Double.doubleToLongBits(expected.getTotalCommissionCollected()),
                Double.doubleToLongBits(actual.getTotalCommissionCollected()));
        assertEquals(expected.getWinningOptionNumber(), actual.getWinningOptionNumber());
        assertEquals(expected.getPurchaseHistory().size(), actual.getPurchaseHistory().size());
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }

    private static final class DisallowedPayload implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }
}
