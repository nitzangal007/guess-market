package guessmarket.engine.xml.ex2;

import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.dto.world.OrderBookConfiguration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

final class Ex2XmlWorldLoaderTest {
    @TempDir Path directory;
    private static Path fixture(String name) {
        return Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/ex2/fixtures", name);
    }
    private Path xml(String content) throws Exception {
        Path file = directory.resolve("world with spaces.xml");
        Files.writeString(file, content);
        return file;
    }

    @Test void suppliedErrorsIdentifyUserAndInvalidReference() throws Exception {
        Ex2XmlWorldLoader loader = new Ex2XmlWorldLoader();
        var cash = assertThrows(EngineOperationException.class, () -> loader.load(fixture("error-2.xml")));
        assertEquals(EngineErrorCode.XML_DATA_INVALID, cash.getCode());
        assertTrue(cash.getDetail().contains("Avrum"));
        assertTrue(cash.getDetail().contains("initial-cash"));
        var owner = assertThrows(EngineOperationException.class, () -> loader.load(fixture("error-3.xml")));
        assertTrue(owner.getDetail().contains("Avrum"));
        assertTrue(owner.getDetail().contains("12"));
    }

    static Stream<Arguments> invalidDefinitions() {
        return Stream.of(
            Arguments.of("duplicate username", "name=\"Menash\"", "name=\"Avrum\""),
            Arguments.of("duplicate id", "<id>2</id>", "<id>1</id>"),
            Arguments.of("two owners", "<event id=\"1\"/>", "<event id=\"2\"/>"),
            Arguments.of("one option", "<GM-option>No way !</GM-option>", ""),
            Arguments.of("zero b", "<b>100</b>", "<b>0</b>"),
            Arguments.of("negative b", "<b>100</b>", "<b>-1</b>"),
            Arguments.of("commission high", ">5</commission>", ">91</commission>"),
            Arguments.of("commission negative", ">5</commission>", ">-1</commission>"),
            Arguments.of("negative initial", "initial=\"100\"", "initial=\"-1\""),
            Arguments.of("zero d", " d=\"1\"", " d=\"0\""),
            Arguments.of("zero cash", "<initial-cash>1000</initial-cash>", "<initial-cash>0</initial-cash>")
        );
    }
    @ParameterizedTest(name="{0}") @MethodSource("invalidDefinitions")
    void rejectsSemanticErrors(String name, String before, String after) throws Exception {
        String original = Files.readString(fixture("small.xml"));
        assertTrue(original.contains(before), "Derivative must alter source: " + name);
        Path file = xml(original.replace(before, after));
        assertEquals(EngineErrorCode.XML_DATA_INVALID,
                assertThrows(EngineOperationException.class, () -> new Ex2XmlWorldLoader().load(file)).getCode());
    }

    @Test void acceptsZeroInvestmentNondivisibleConfigurationAndIdempotentSameOwnerReference() throws Exception {
        String original = Files.readString(fixture("small.xml"));
        String content = original.replace("initial=\"100\"", "initial=\"0\"")
                .replace("<event id=\"2\"/>", "<event id=\"2\"/><event id=\"2\"/>");
        var world = new Ex2XmlWorldLoader().load(xml(content)).snapshot();
        assertEquals(1, world.users().getFirst().ownedEventIds().size());
        assertEquals(new OrderBookConfiguration(0, 1, true), world.events().get(1).pricing());
        content = original.replace("initial=\"100\"", "initial=\"3\"").replace(" d=\"1\"", " d=\"2\"");
        var other = new Ex2XmlWorldLoader().load(xml(content)).snapshot();
        assertEquals(new OrderBookConfiguration(3, 2, true), other.events().get(1).pricing());
    }

    @Test void designatedStringsTrimOnlyEdgesAndPreserveCaseAndInternalSpaces() throws Exception {
        String content = Files.readString(fixture("small.xml"))
                .replace("name=\"Tikva\"", "name=\"avrum\"")
                .replace("name=\"Menash\"", "name=\" Men  ash \"")
                .replace("name=\"Mujtaba is Dead\"", "name=\"  Long  event name  \"")
                .replace("<description>", "<description>  ")
                .replace("</description>", "  </description>")
                .replace("Hell Yea !", "  Hell  Yea !  ");
        var world = new Ex2XmlWorldLoader().load(xml(content)).snapshot();
        assertEquals("avrum", world.users().get(1).name());
        assertEquals("Men  ash", world.users().get(2).name());
        assertEquals("Long  event name", world.events().getFirst().name());
        assertEquals("Hell  Yea !", world.events().getFirst().optionLabels().getFirst());
        assertEquals(world.events().getFirst().description().trim(), world.events().getFirst().description());
    }
    @Test void duplicateUsersAfterTrimmingAreRejected() throws Exception {
        String content=Files.readString(fixture("small.xml")).replace("name=\"Menash\"", "name=\" Avrum \"");
        assertThrows(EngineOperationException.class, () -> new Ex2XmlWorldLoader().load(xml(content)));
    }

    @Test void rejectsMissingOwnerWithoutAnotherReferenceError() throws Exception {
        String content = Files.readString(fixture("small.xml"))
                .replaceFirst("(?s)<GM-market-maker>.*?</GM-market-maker>", "");
        var failure = assertThrows(EngineOperationException.class,
                () -> new Ex2XmlWorldLoader().load(xml(content)));
        assertEquals(EngineErrorCode.XML_DATA_INVALID, failure.getCode());
        assertTrue(failure.getDetail().contains("Event 2 has no assigned market maker"));
    }

    @Test void rejectsE1MalformedXmlAndDtdWithoutExternalAccess() throws Exception {
        Ex2XmlWorldLoader loader = new Ex2XmlWorldLoader();
        Path e1 = Path.of("modules/guessmarket-engine/src/test/resources/guessmarket/engine/xml/fixtures/supplied/multiple.xml");
        assertEquals(EngineErrorCode.XML_STRUCTURE_INVALID,
                assertThrows(EngineOperationException.class, () -> loader.load(e1)).getCode());
        Path malformed = xml("<Guess-Market>");
        assertEquals(EngineErrorCode.XML_STRUCTURE_INVALID,
                assertThrows(EngineOperationException.class, () -> loader.load(malformed)).getCode());
        Path dtd = xml(Files.readString(fixture("small.xml")).replace("<Guess-Market",
                "<!DOCTYPE Guess-Market SYSTEM \"file:///must-not-be-read\">\n<Guess-Market"));
        assertEquals(EngineErrorCode.XML_STRUCTURE_INVALID,
                assertThrows(EngineOperationException.class, () -> loader.load(dtd)).getCode());
    }

    @Test void rejectsMissingAndWrongSuffixAndIgnoresRemoteSchemaHint() throws Exception {
        Ex2XmlWorldLoader loader = new Ex2XmlWorldLoader();
        assertEquals(EngineErrorCode.XML_FILE_NOT_FOUND,
                assertThrows(EngineOperationException.class, () -> loader.load(directory.resolve("missing.xml"))).getCode());
        assertEquals(EngineErrorCode.INVALID_XML_PATH,
                assertThrows(EngineOperationException.class, () -> loader.load(directory.resolve("world.txt"))).getCode());
        String content = Files.readString(fixture("small.xml")).replace("GM-EX2-Schema.xsd", "https://invalid.example/no-schema.xsd");
        assertEquals(2, loader.load(xml(content)).snapshot().events().size());
    }
}
