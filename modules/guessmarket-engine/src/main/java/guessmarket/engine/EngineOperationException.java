package guessmarket.engine;

import guessmarket.dto.EventStatus;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public class EngineOperationException extends Exception {
    private static final long serialVersionUID = 1L;

    private final EngineErrorCode code;
    private final String detail;
    private final String recoveryHint;
    private final transient Path path;
    private final Integer xmlEventNumber;
    private final Integer eventId;
    private final String fieldName;
    private final Integer optionNumber;
    private final Integer quantity;
    private final EventStatus eventStatus;
    private final Integer lineNumber;
    private final Integer columnNumber;

    public EngineOperationException(
            EngineErrorCode code,
            String detail,
            String recoveryHint) {
        this(
                code,
                detail,
                recoveryHint,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public EngineOperationException(
            EngineErrorCode code,
            String detail,
            String recoveryHint,
            Path path,
            Integer xmlEventNumber,
            Integer eventId,
            String fieldName,
            Integer optionNumber,
            Integer quantity,
            EventStatus eventStatus,
            Integer lineNumber,
            Integer columnNumber,
            Throwable cause) {
        super(Objects.requireNonNull(detail, "detail"), cause);
        this.code = Objects.requireNonNull(code, "code");
        this.detail = detail;
        this.recoveryHint = Objects.requireNonNull(recoveryHint, "recoveryHint");
        this.path = path;
        this.xmlEventNumber = xmlEventNumber;
        this.eventId = eventId;
        this.fieldName = fieldName;
        this.optionNumber = optionNumber;
        this.quantity = quantity;
        this.eventStatus = eventStatus;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public EngineErrorCode getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }

    public String getRecoveryHint() {
        return recoveryHint;
    }

    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    public OptionalInt getXmlEventNumber() {
        return optionalInt(xmlEventNumber);
    }

    public OptionalInt getEventId() {
        return optionalInt(eventId);
    }

    public Optional<String> getFieldName() {
        return Optional.ofNullable(fieldName);
    }

    public OptionalInt getOptionNumber() {
        return optionalInt(optionNumber);
    }

    public OptionalInt getQuantity() {
        return optionalInt(quantity);
    }

    public Optional<EventStatus> getEventStatus() {
        return Optional.ofNullable(eventStatus);
    }

    public OptionalInt getLineNumber() {
        return optionalInt(lineNumber);
    }

    public OptionalInt getColumnNumber() {
        return optionalInt(columnNumber);
    }

    private static OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }
}
