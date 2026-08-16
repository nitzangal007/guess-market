package guessmarket.engine.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.MarketEvent;
import guessmarket.engine.xml.generated.Comision;
import guessmarket.engine.xml.generated.GMEvent;
import guessmarket.engine.xml.generated.GMEvents;
import guessmarket.engine.xml.generated.GMLMSR;
import guessmarket.engine.xml.generated.GMMethod;
import guessmarket.engine.xml.generated.GMOptions;
import guessmarket.engine.xml.generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class JaxbMarketMapperTest {
    private final JaxbMarketMapper mapper = new JaxbMarketMapper();

    @Test
    void mapsSuppliedValidFixturesIntoOrderedFreshDomainEvents() throws Exception {
        LinkedHashMap<Integer, MarketEvent> single = mapFixture("supplied/single.xml");
        LinkedHashMap<Integer, MarketEvent> multiple = mapFixture("supplied/multiple.xml");

        assertEquals(List.of(3), List.copyOf(single.keySet()));
        assertEquals(List.of(1, 2, 3), List.copyOf(multiple.keySet()));
        assertNotNull(single.get(3));
        assertNotNull(multiple.get(1));
        assertNotNull(multiple.get(2));
        assertNotNull(multiple.get(3));
        assertTrue(single != multiple);
        assertTrue(single.get(3) != multiple.get(3));
    }

    @Test
    void mapsFullRangeIdsEmptyTextDuplicateLabelsAndBoundaryCommissionValues() throws Exception {
        LinkedHashMap<Integer, MarketEvent> events =
                mapFixture("custom/valid full-range IDs and empty text.xml");

        assertEquals(List.of(0, -17, Integer.MIN_VALUE, Integer.MAX_VALUE),
                List.copyOf(events.keySet()));
        for (int id : events.keySet()) {
            assertNotNull(events.get(id));
        }
    }

    @Test
    void normalizesOuterWhitespaceAndJoinsNameTokensWithoutChangingInternalText() throws Exception {
        GuessMarket root = rootOf(event(
                8,
                List.of("  Alpha", "Beta  "),
                "  first  second  ",
                0,
                "on-purchase",
                List.of("  one  two  ", "  three  four  "),
                1));

        LinkedHashMap<Integer, MarketEvent> events = mapper.map(root);

        assertEquals(List.of(8), List.copyOf(events.keySet()));
        assertNotNull(events.get(8));
    }

    @Test
    void rejectsSuppliedDuplicateIdWithSecondEventAndIdContext() throws Exception {
        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> mapFixture("supplied/error-2.xml"));

        assertXmlDataFailure(exception, 3, 1, "id");
    }

    @Test
    void rejectsSuppliedCommissionAboveMaximumWithEventAndFieldContext() throws Exception {
        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> mapFixture("supplied/error-3.xml"));

        assertXmlDataFailure(exception, 2, 2, "comision");
    }

    @Test
    void rejectsDuplicateZeroIdWithSecondEventAndIdContext() throws Exception {
        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> mapFixture("custom/invalid duplicate nonpositive IDs.xml"));

        assertXmlDataFailure(exception, 2, 0, "id");
    }

    @Test
    void rejectsOneOptionWithEventIdAndOptionsContext() throws Exception {
        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> mapFixture("custom/invalid one option.xml"));

        assertXmlDataFailure(exception, 1, 7, "GM-options");
        assertFalse(exception.getOptionNumber().isPresent());
    }

    @Test
    void rejectsCommissionOutsideBothBusinessBoundsWithEventAndFieldContext() {
        for (int commission : List.of(-1, 91)) {
            EngineOperationException exception = assertThrows(EngineOperationException.class,
                    () -> mapper.map(rootOf(event(9, List.of("commission"), "description",
                            commission, "on-close", List.of("one", "two"), 1))));

            assertXmlDataFailure(exception, 1, 9, "comision");
        }
    }

    @Test
    void rejectsNonpositiveLiquidityWithEventAndFieldContext() {
        for (int b : List.of(0, -1)) {
            EngineOperationException exception = assertThrows(EngineOperationException.class,
                    () -> mapper.map(rootOf(event(10, List.of("liquidity"), "description",
                            0, "on-purchase", List.of("one", "two"), b))));

            assertXmlDataFailure(exception, 1, 10, "b");
        }
    }

    private LinkedHashMap<Integer, MarketEvent> mapFixture(String relativeFixturePath)
            throws JAXBException, EngineOperationException {
        return mapper.map(unmarshalFixture(relativeFixturePath));
    }

    private static GuessMarket unmarshalFixture(String relativeFixturePath) throws JAXBException {
        String resourcePath = "guessmarket/engine/xml/fixtures/" + relativeFixturePath;
        try (InputStream input = JaxbMarketMapperTest.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing test fixture: " + resourcePath);
            }
            Unmarshaller unmarshaller = JAXBContext.newInstance(GuessMarket.class).createUnmarshaller();
            return (GuessMarket) unmarshaller.unmarshal(input);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not close test fixture: " + resourcePath, exception);
        }
    }

    private static GuessMarket rootOf(GMEvent event) {
        GuessMarket root = new GuessMarket();
        GMEvents events = new GMEvents();
        events.getGMEvent().add(event);
        root.setGMEvents(events);
        return root;
    }

    private static GMEvent event(
            int id,
            List<String> nameTokens,
            String description,
            int commissionValue,
            String commissionType,
            List<String> optionLabels,
            int b) {
        GMEvent event = new GMEvent();
        event.setId(id);
        event.getName().addAll(nameTokens);
        event.setDescription(description);

        Comision commission = new Comision();
        commission.setValue(commissionValue);
        commission.setType(commissionType);
        event.setComision(commission);

        GMOptions options = new GMOptions();
        options.getGMOption().addAll(optionLabels);
        event.setGMOptions(options);

        GMLMSR lmsr = new GMLMSR();
        lmsr.setB(b);
        GMMethod method = new GMMethod();
        method.setGMLMSR(lmsr);
        event.setGMMethod(method);
        return event;
    }

    private static void assertXmlDataFailure(
            EngineOperationException exception,
            int xmlEventNumber,
            int eventId,
            String fieldName) {
        assertEquals(EngineErrorCode.XML_DATA_INVALID, exception.getCode());
        assertEquals(xmlEventNumber, exception.getXmlEventNumber().orElseThrow());
        assertEquals(eventId, exception.getEventId().orElseThrow());
        assertEquals(fieldName, exception.getFieldName().orElseThrow());
    }
}
