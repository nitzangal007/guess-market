package guessmarket.engine;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.dto.EventStatus;
import guessmarket.dto.MarketEventDetails;
import guessmarket.dto.PurchaseReceipt;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GuessMarketEngineUseCaseTest {

    @Test
    void engineInterfaceDeclaresSevenCheckedOperations() throws NoSuchMethodException {
        assertTrue(GuessMarketEngine.class.isInterface());

        Method load = GuessMarketEngine.class.getDeclaredMethod("loadEventsFromXml", Path.class);
        Method list = GuessMarketEngine.class.getDeclaredMethod("listEvents");
        Method details = GuessMarketEngine.class.getDeclaredMethod("getEventDetails", int.class);
        Method purchase = GuessMarketEngine.class.getDeclaredMethod(
                "purchaseShares", int.class, int.class, int.class);
        Method close = GuessMarketEngine.class.getDeclaredMethod("closeEvent", int.class, int.class);
        Method save = GuessMarketEngine.class.getDeclaredMethod("saveState", Path.class);
        Method restore = GuessMarketEngine.class.getDeclaredMethod("restoreState", Path.class);

        assertAll(
                () -> assertEquals(7, GuessMarketEngine.class.getDeclaredMethods().length),
                () -> assertMethod(load, int.class),
                () -> assertMethod(list, List.class),
                () -> assertMethod(details, MarketEventDetails.class),
                () -> assertMethod(purchase, PurchaseReceipt.class),
                () -> assertMethod(close, MarketEventDetails.class),
                () -> assertMethod(save, void.class),
                () -> assertMethod(restore, int.class));
    }

    @Test
    void errorCodeContainsExactlyApprovedValues() {
        assertArrayEquals(
                new String[] {
                    "INVALID_XML_PATH",
                    "XML_FILE_NOT_FOUND",
                    "XML_FILE_ACCESS_FAILED",
                    "XML_STRUCTURE_INVALID",
                    "XML_DATA_INVALID",
                    "ENGINE_CONFIGURATION_ERROR",
                    "NO_SYSTEM_LOADED",
                    "EVENT_NOT_FOUND",
                    "EVENT_NOT_OPEN",
                    "INVALID_OPTION",
                    "INVALID_QUANTITY",
                    "FINANCIAL_CALCULATION_FAILED",
                    "INVALID_STATE_PATH",
                    "STATE_FILE_NOT_FOUND",
                    "STATE_FILE_ACCESS_FAILED",
                    "SAVED_STATE_INVALID"
                },
                java.util.Arrays.stream(EngineErrorCode.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void simpleExceptionPreservesRequiredFields() {
        EngineOperationException failure = new EngineOperationException(
                EngineErrorCode.NO_SYSTEM_LOADED,
                "No system is loaded",
                "Load XML or restore saved state first");

        assertAll(
                () -> assertTrue(Exception.class.isAssignableFrom(EngineOperationException.class)),
                () -> assertFalse(RuntimeException.class.isAssignableFrom(EngineOperationException.class)),
                () -> assertEquals(EngineErrorCode.NO_SYSTEM_LOADED, failure.getCode()),
                () -> assertEquals("No system is loaded", failure.getDetail()),
                () -> assertEquals("Load XML or restore saved state first", failure.getRecoveryHint()),
                () -> assertEquals("No system is loaded", failure.getMessage()),
                () -> assertNull(failure.getCause()),
                () -> assertEquals(Optional.empty(), failure.getPath()),
                () -> assertEquals(OptionalInt.empty(), failure.getXmlEventNumber()),
                () -> assertEquals(OptionalInt.empty(), failure.getEventId()),
                () -> assertEquals(Optional.empty(), failure.getFieldName()),
                () -> assertEquals(OptionalInt.empty(), failure.getOptionNumber()),
                () -> assertEquals(OptionalInt.empty(), failure.getQuantity()),
                () -> assertEquals(Optional.empty(), failure.getEventStatus()),
                () -> assertEquals(OptionalInt.empty(), failure.getLineNumber()),
                () -> assertEquals(OptionalInt.empty(), failure.getColumnNumber()));
    }

    @Test
    void completeExceptionPreservesOptionalContextAndCause() {
        Path path = Path.of("fixtures", "invalid.xml");
        Exception cause = new IllegalStateException("developer diagnosis only");
        EngineOperationException failure = new EngineOperationException(
                EngineErrorCode.XML_DATA_INVALID,
                "Commission is outside the accepted range",
                "Correct the event commission and load the file again",
                path,
                4,
                Integer.MIN_VALUE,
                "commission",
                2,
                7,
                EventStatus.CLOSED,
                10,
                20,
                cause);

        assertAll(
                () -> assertEquals(EngineErrorCode.XML_DATA_INVALID, failure.getCode()),
                () -> assertEquals(Optional.of(path), failure.getPath()),
                () -> assertEquals(OptionalInt.of(4), failure.getXmlEventNumber()),
                () -> assertEquals(OptionalInt.of(Integer.MIN_VALUE), failure.getEventId()),
                () -> assertEquals(Optional.of("commission"), failure.getFieldName()),
                () -> assertEquals(OptionalInt.of(2), failure.getOptionNumber()),
                () -> assertEquals(OptionalInt.of(7), failure.getQuantity()),
                () -> assertEquals(Optional.of(EventStatus.CLOSED), failure.getEventStatus()),
                () -> assertEquals(OptionalInt.of(10), failure.getLineNumber()),
                () -> assertEquals(OptionalInt.of(20), failure.getColumnNumber()),
                () -> assertSame(cause, failure.getCause()));
    }

    private static void assertMethod(Method method, Class<?> returnType) {
        assertEquals(returnType, method.getReturnType());
        assertArrayEquals(
                new Class<?>[] {EngineOperationException.class},
                method.getExceptionTypes(),
                method.getName() + " must declare the checked Engine failure contract");
    }
}
