package guessmarket.engine;

import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;

final class SavedStateStore {
    private final MoveOperation moveOperation;
    private final SavedStateValidator validator;

    SavedStateStore() {
        this(Files::move);
    }

    SavedStateStore(MoveOperation moveOperation) {
        this.moveOperation = Objects.requireNonNull(moveOperation, "moveOperation");
        validator = new SavedStateValidator();
    }

    void save(Path basePath, Collection<MarketEvent> events)
            throws EngineOperationException {
        Path target = resolveStatePath(basePath);
        SavedState state;
        try {
            state = new SavedState(events);
        } catch (NullPointerException exception) {
            throw invalid("The state to save is invalid.", exception);
        }
        validator.validate(state);

        Path temporary = null;
        try {
            temporary = Files.createTempFile(target.getParent(), target.getFileName().toString() + ".",
                    ".tmp");
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                output.writeObject(state);
            }
            publish(temporary, target);
        } catch (EngineOperationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw accessFailed(target, "The saved state file could not be written or published.", exception);
        } finally {
            deleteTemporaryIfPresent(temporary);
        }
    }

    LinkedHashMap<Integer, MarketEvent> restore(Path basePath)
            throws EngineOperationException {
        Path target = resolveStatePath(basePath);
        validateRestoreTarget(target);
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(target))) {
            input.setObjectInputFilter(SavedStateStore::filter);
            Object root = input.readObject();
            if (!(root instanceof SavedState state)) {
                throw invalid("The saved state file has an unsupported root object.", null);
            }
            requireNoTrailingObject(input);
            return validator.validate(state);
        } catch (EngineOperationException exception) {
            throw exception;
        } catch (ClassNotFoundException | InvalidClassException | StreamCorruptedException exception) {
            throw invalid("The saved state file is corrupt or incompatible.", exception);
        } catch (IOException exception) {
            throw invalid("The saved state file is corrupt or incomplete.", exception);
        }
    }

    static Path resolveStatePath(Path basePath)
            throws EngineOperationException {
        if (basePath == null) {
            throw invalidPath(null, "A saved state path is required.");
        }
        Path fileName = basePath.getFileName();
        if (fileName == null || (Files.exists(basePath) && Files.isDirectory(basePath))) {
            throw invalidPath(basePath, "The saved state path must name a file.");
        }
        String name = fileName.toString();
        Path target = name.toLowerCase(Locale.ROOT).endsWith(".ser")
                ? basePath : basePath.resolveSibling(name + ".ser");
        target = target.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw invalidPath(target, "The saved state parent directory must already exist.");
        }
        if (Files.exists(target) && Files.isDirectory(target)) {
            throw invalidPath(target, "The saved state path must name a file.");
        }
        return target;
    }

    private void publish(Path temporary, Path target) throws EngineOperationException {
        try {
            moveOperation.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (IOException atomicFailure) {
            if (!Files.exists(temporary)) {
                throw accessFailed(target,
                        "Saved state publication failed and its disk state is unknown.", atomicFailure);
            }
            try {
                moveOperation.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackFailure) {
                fallbackFailure.addSuppressed(atomicFailure);
                throw accessFailed(target, "Saved state publication failed.", fallbackFailure);
            }
        }
    }

    private static void validateRestoreTarget(Path target) throws EngineOperationException {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(target, BasicFileAttributes.class);
        } catch (NoSuchFileException exception) {
            throw new EngineOperationException(
                    EngineErrorCode.STATE_FILE_NOT_FOUND,
                    "The saved state file does not exist.",
                    "Choose an existing saved state file and try again.",
                    target, null, null, null, null, null, null, null, null, exception);
        } catch (IOException exception) {
            throw accessFailed(target, "The saved state file cannot be inspected.", exception);
        }
        if (!attributes.isRegularFile()) {
            throw invalidPath(target, "The saved state path must name a regular file.");
        }
        if (!Files.isReadable(target)) {
            throw accessFailed(target, "The saved state file cannot be read.", null);
        }
    }

    private static void requireNoTrailingObject(ObjectInputStream input)
            throws IOException, ClassNotFoundException, EngineOperationException {
        try {
            Object trailing = input.readObject();
            throw invalid("The saved state file contains unexpected trailing data: "
                    + trailing.getClass().getName(), null);
        } catch (EOFException expectedEnd) {
            // A single completed root object is the whole file format.
        }
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo information) {
        if (information.depth() > 32) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> serialClass = information.serialClass();
        if (serialClass == null || serialClass.isPrimitive()) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        if (serialClass.isArray()) {
            Class<?> component = serialClass.getComponentType();
            return component.isPrimitive() || component == Object.class
                    ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
        }
        if (serialClass == SavedState.class || serialClass == MarketEvent.class
                || serialClass == MarketOption.class || serialClass == EventAccount.class
                || serialClass == CommissionPolicy.class || serialClass == TradeRecord.class
                || serialClass == String.class || serialClass == Integer.class
                || serialClass == Number.class || serialClass == Enum.class
                || serialClass == java.util.ArrayList.class
                || serialClass == guessmarket.dto.CommissionMode.class
                || serialClass == guessmarket.dto.EventStatus.class) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }

    private static void deleteTemporaryIfPresent(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // The original publication failure is the caller-visible failure.
        }
    }

    private static EngineOperationException invalidPath(Path path, String detail) {
        return new EngineOperationException(
                EngineErrorCode.INVALID_STATE_PATH,
                detail,
                "Choose a file path in an existing directory and try again.",
                path, null, null, null, null, null, null, null, null, null);
    }

    private static EngineOperationException accessFailed(Path path, String detail, Throwable cause) {
        return new EngineOperationException(
                EngineErrorCode.STATE_FILE_ACCESS_FAILED,
                detail,
                "Check the file permissions and try again.",
                path, null, null, null, null, null, null, null, null, cause);
    }

    private static EngineOperationException invalid(String detail, Throwable cause) {
        return new EngineOperationException(
                EngineErrorCode.SAVED_STATE_INVALID,
                detail,
                "Restore a valid saved state file and try again.",
                null, null, null, null, null, null, null, null, null, cause);
    }

    @FunctionalInterface
    interface MoveOperation {
        void move(Path source, Path target, CopyOption... options) throws IOException;
    }
}
