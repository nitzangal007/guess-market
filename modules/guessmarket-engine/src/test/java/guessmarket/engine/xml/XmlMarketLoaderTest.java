package guessmarket.engine.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.MarketEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlMarketLoaderTest {
    private final XmlMarketLoader loader = new XmlMarketLoader();

    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectsNullPathAsInvalidXmlPath() {
        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(null));

        assertEquals(EngineErrorCode.INVALID_XML_PATH, exception.getCode());
        assertFalse(exception.getPath().isPresent());
    }

    @Test
    void rejectsXmlNamedDirectoryAsInvalidXmlPath() throws IOException {
        Path directory = Files.createDirectory(temporaryDirectory.resolve("folder.xml"));

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(directory));

        assertEquals(EngineErrorCode.INVALID_XML_PATH, exception.getCode());
        assertEquals(directory, exception.getPath().orElseThrow());
    }

    @Test
    void rejectsWrongSuffixAsInvalidXmlPath() throws IOException {
        Path path = temporaryDirectory.resolve("market.txt");
        Files.writeString(path, "not XML");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(path));

        assertEquals(EngineErrorCode.INVALID_XML_PATH, exception.getCode());
        assertEquals(path, exception.getPath().orElseThrow());
    }

    @Test
    void reportsMissingXmlFileAfterSuffixValidation() {
        Path path = temporaryDirectory.resolve("missing.XML");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(path));

        assertEquals(EngineErrorCode.XML_FILE_NOT_FOUND, exception.getCode());
        assertEquals(path, exception.getPath().orElseThrow());
    }

    @Test
    void loadsUppercaseSuffixFromPathContainingSpaces() throws Exception {
        Path path = copyFixture("supplied/single.xml", "market with spaces.XML");

        LinkedHashMap<Integer, MarketEvent> events = loader.load(path);

        assertEquals(1, events.size());
        assertTrue(events.containsKey(3));
    }

    @Test
    void usesTrustedClasspathSchemaInsteadOfInputSchemaLocationHint() throws Exception {
        Path path = copyFixture("supplied/single.xml", "misleading-hint.xml");
        String xml = Files.readString(path).replace("GM-EX1-schema.xsd",
                "file:///definitely-not-the-application-schema.xsd");
        Files.writeString(path, xml);

        LinkedHashMap<Integer, MarketEvent> events = loader.load(path);

        assertEquals(1, events.size());
        assertTrue(events.containsKey(3));
    }

    @Test
    void translatesMalformedXmlWithSafeCauseAndLocation() throws IOException {
        Path path = temporaryDirectory.resolve("malformed.xml");
        Files.writeString(path, "<Guess-Market><GM-events>");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(path));

        assertStructureFailure(exception, path);
        assertNotNull(exception.getCause());
        assertTrue(exception.getLineNumber().orElseThrow() > 0);
        assertTrue(exception.getColumnNumber().orElseThrow() > 0);
    }

    @Test
    void translatesInvalidUtf8AsStructuralFailure() throws IOException {
        Path path = temporaryDirectory.resolve("invalid encoding.xml");
        String xml = Files.readString(fixturePath("supplied/single.xml"))
                .replace("Will there be", "\u00c3(ill there be");
        Files.write(path, xml.getBytes(StandardCharsets.ISO_8859_1));

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(path));

        assertStructureFailure(exception, path);
        assertNotNull(exception.getCause());
    }

    @Test
    void translatesSchemaInvalidMissingStructureWithSafeCauseAndLocation() {
        Path path = fixturePath("custom/invalid missing description.xml");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(path));

        assertStructureFailure(exception, path);
        assertNotNull(exception.getCause());
        assertTrue(exception.getLineNumber().orElseThrow() > 0);
        assertTrue(exception.getColumnNumber().orElseThrow() > 0);
    }

    @Test
    void preservesMapperOwnedBusinessInvalidContextWithoutLowLevelCause() {
        Path path = fixturePath("supplied/error-2.xml");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> loader.load(path));

        assertEquals(EngineErrorCode.XML_DATA_INVALID, exception.getCode());
        assertEquals(3, exception.getXmlEventNumber().orElseThrow());
        assertEquals(1, exception.getEventId().orElseThrow());
        assertEquals("id", exception.getFieldName().orElseThrow());
        assertNull(exception.getCause());
    }

    @Test
    void translatesExpectedStreamOpenFailureToAccessFailure() {
        IOException expectedCause = new IOException("simulated open failure");
        XmlMarketLoader failingLoader = new XmlMarketLoader(
                path -> {
                    throw expectedCause;
                },
                new JaxbMarketMapper()::map);
        Path path = fixturePath("supplied/single.xml");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> failingLoader.load(path));

        assertEquals(EngineErrorCode.XML_FILE_ACCESS_FAILED, exception.getCode());
        assertEquals(path, exception.getPath().orElseThrow());
        assertEquals(expectedCause, exception.getCause());
    }

    @Test
    void reportsMissingTrustedSchemaAsConfigurationError() {
        XmlMarketLoader configurationFailingLoader = new XmlMarketLoader(
                Files::newInputStream,
                new JaxbMarketMapper()::map,
                resourcePath -> null);
        Path path = fixturePath("supplied/single.xml");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> configurationFailingLoader.load(path));

        assertConfigurationFailure(exception, path);
        assertNull(exception.getCause());
    }

    @Test
    void reportsUnreadableTrustedSchemaAsConfigurationError() {
        IOException expectedCause = new IOException("simulated schema read failure");
        XmlMarketLoader configurationFailingLoader = new XmlMarketLoader(
                Files::newInputStream,
                new JaxbMarketMapper()::map,
                resourcePath -> {
                    throw expectedCause;
                });
        Path path = fixturePath("supplied/single.xml");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> configurationFailingLoader.load(path));

        assertConfigurationFailure(exception, path);
        assertEquals(expectedCause, exception.getCause());
    }

    @Test
    void reportsBrokenTrustedSchemaAsConfigurationError() {
        XmlMarketLoader configurationFailingLoader = new XmlMarketLoader(
                Files::newInputStream,
                new JaxbMarketMapper()::map,
                resourcePath -> new ByteArrayInputStream("not an XML schema".getBytes(
                        StandardCharsets.UTF_8)));
        Path path = fixturePath("supplied/single.xml");

        EngineOperationException exception = assertThrows(EngineOperationException.class,
                () -> configurationFailingLoader.load(path));

        assertConfigurationFailure(exception, path);
        assertNotNull(exception.getCause());
    }

    @Test
    void closesSuccessfulInputBeforeWindowsMoveAndDelete() throws Exception {
        Path path = copyFixture("supplied/single.xml", "close after success.xml");
        loader.load(path);

        Path moved = temporaryDirectory.resolve("moved after success.xml");
        Files.move(path, moved, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(moved);

        assertFalse(Files.exists(moved));
    }

    @Test
    void closesFailedInputBeforeWindowsMoveAndDelete() throws Exception {
        Path path = copyFixture("custom/invalid missing description.xml",
                "close after failure.xml");
        assertThrows(EngineOperationException.class, () -> loader.load(path));

        Path moved = temporaryDirectory.resolve("moved after failure.xml");
        Files.move(path, moved, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(moved);

        assertFalse(Files.exists(moved));
    }

    @Test
    void leavesProgrammingDefectsFromMapperVisible() throws IOException {
        XmlMarketLoader defectiveLoader = new XmlMarketLoader(
                Files::newInputStream,
                root -> {
                    throw new IllegalStateException("programming defect");
                });
        Path path = copyFixture("supplied/single.xml", "mapper defect.xml");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> defectiveLoader.load(path));

        assertEquals("programming defect", exception.getMessage());
    }

    private void assertStructureFailure(EngineOperationException exception, Path path) {
        assertEquals(EngineErrorCode.XML_STRUCTURE_INVALID, exception.getCode());
        assertEquals(path, exception.getPath().orElseThrow());
    }

    private void assertConfigurationFailure(EngineOperationException exception, Path path) {
        assertEquals(EngineErrorCode.ENGINE_CONFIGURATION_ERROR, exception.getCode());
        assertEquals(path, exception.getPath().orElseThrow());
    }

    private Path copyFixture(String relativeFixturePath, String targetFileName) throws IOException {
        Path target = temporaryDirectory.resolve(targetFileName);
        Files.copy(fixturePath(relativeFixturePath), target);
        return target;
    }

    private static Path fixturePath(String relativeFixturePath) {
        try {
            return Path.of(XmlMarketLoaderTest.class.getClassLoader().getResource(
                    "guessmarket/engine/xml/fixtures/" + relativeFixturePath).toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid test fixture URI: " + relativeFixturePath,
                    exception);
        }
    }
}
