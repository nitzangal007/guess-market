package guessmarket.engine.xml;

import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.MarketEvent;
import guessmarket.engine.xml.generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class XmlMarketLoader {
    private static final String TRUSTED_SCHEMA_RESOURCE =
            "guessmarket/engine/xml/GM-EX1-Schema.xsd";

    private final InputStreamOpener inputStreamOpener;
    private final CandidateMapper candidateMapper;
    private final TrustedSchemaOpener trustedSchemaOpener;

    public XmlMarketLoader() {
        this(Files::newInputStream, new JaxbMarketMapper()::map,
                XmlMarketLoader::openTrustedSchemaResource);
    }

    XmlMarketLoader(InputStreamOpener inputStreamOpener, CandidateMapper candidateMapper) {
        this(inputStreamOpener, candidateMapper, XmlMarketLoader::openTrustedSchemaResource);
    }

    XmlMarketLoader(
            InputStreamOpener inputStreamOpener,
            CandidateMapper candidateMapper,
            TrustedSchemaOpener trustedSchemaOpener) {
        this.inputStreamOpener = Objects.requireNonNull(inputStreamOpener, "inputStreamOpener");
        this.candidateMapper = Objects.requireNonNull(candidateMapper, "candidateMapper");
        this.trustedSchemaOpener = Objects.requireNonNull(trustedSchemaOpener,
                "trustedSchemaOpener");
    }

    public LinkedHashMap<Integer, MarketEvent> load(Path path)
            throws EngineOperationException {
        validatePath(path);

        Unmarshaller unmarshaller = trustedUnmarshaller(path);
        try (InputStream input = inputStreamOpener.open(path)) {
            Object result = unmarshaller.unmarshal(input);
            if (!(result instanceof GuessMarket root)) {
                throw structureInvalid(path, "The XML document does not contain a Guess-Market root.",
                        null);
            }
            return candidateMapper.map(root);
        } catch (EngineOperationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw accessFailed(path, exception);
        } catch (JAXBException exception) {
            IOException ioCause = findCause(exception, IOException.class);
            if (ioCause != null) {
                throw accessFailed(path, ioCause);
            }
            throw structureInvalid(path, "The XML file is malformed or violates the trusted schema.",
                    exception);
        }
    }

    private static void validatePath(Path path) throws EngineOperationException {
        if (path == null) {
            throw new EngineOperationException(
                    EngineErrorCode.INVALID_XML_PATH,
                    "An XML file path is required.",
                    "Choose an existing readable XML file and try again.");
        }
        if (!hasXmlSuffix(path)) {
            throw new EngineOperationException(
                    EngineErrorCode.INVALID_XML_PATH,
                    "The selected path must name a regular .xml file.",
                    "Choose an existing readable XML file and try again.",
                    path, null, null, null, null, null, null, null, null, null);
        }
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (NoSuchFileException exception) {
            throw new EngineOperationException(
                    EngineErrorCode.XML_FILE_NOT_FOUND,
                    "The selected XML file does not exist.",
                    "Choose an existing XML file and try again.",
                    path, null, null, null, null, null, null, null, null, null);
        } catch (IOException exception) {
            throw accessFailed(path, exception);
        }
        if (!attributes.isRegularFile()) {
            throw new EngineOperationException(
                    EngineErrorCode.INVALID_XML_PATH,
                    "The selected path must name a regular .xml file.",
                    "Choose an existing readable XML file and try again.",
                    path, null, null, null, null, null, null, null, null, null);
        }
        if (!Files.isReadable(path)) {
            throw new EngineOperationException(
                    EngineErrorCode.XML_FILE_ACCESS_FAILED,
                    "The selected XML file cannot be read.",
                    "Check the file permissions and try again.",
                    path, null, null, null, null, null, null, null, null, null);
        }
    }

    private static boolean hasXmlSuffix(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().toLowerCase(java.util.Locale.ROOT)
                .endsWith(".xml");
    }

    private Unmarshaller trustedUnmarshaller(Path path) throws EngineOperationException {
        Schema schema = trustedSchema(path);
        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(GuessMarket.class).createUnmarshaller();
            unmarshaller.setSchema(schema);
            return unmarshaller;
        } catch (JAXBException exception) {
            throw configurationFailed(path, "The JAXB XML loader configuration is unavailable.", exception);
        }
    }

    private Schema trustedSchema(Path path) throws EngineOperationException {
        try (InputStream schemaInput = trustedSchemaOpener.open(TRUSTED_SCHEMA_RESOURCE)) {
            if (schemaInput == null) {
                throw configurationFailed(path, "The trusted XML schema resource is missing.", null);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(new StreamSource(schemaInput));
        } catch (SAXException | IOException exception) {
            throw configurationFailed(path, "The trusted XML schema resource is invalid or unreadable.",
                    exception);
        }
    }

    private static InputStream openTrustedSchemaResource(String resourcePath) {
        return XmlMarketLoader.class.getClassLoader().getResourceAsStream(resourcePath);
    }

    private static EngineOperationException accessFailed(Path path, IOException cause) {
        return new EngineOperationException(
                EngineErrorCode.XML_FILE_ACCESS_FAILED,
                "The selected XML file could not be opened or read.",
                "Check the file permissions and try again.",
                path, null, null, null, null, null, null, null, null, cause);
    }

    private static EngineOperationException structureInvalid(
            Path path,
            String detail,
            JAXBException cause) {
        SAXParseException parseException = findCause(cause, SAXParseException.class);
        Integer lineNumber = parseException == null ? null : parseException.getLineNumber();
        Integer columnNumber = parseException == null ? null : parseException.getColumnNumber();
        return new EngineOperationException(
                EngineErrorCode.XML_STRUCTURE_INVALID,
                detail,
                "Correct the XML structure and try again.",
                path, null, null, null, null, null, null, lineNumber, columnNumber, cause);
    }

    private static EngineOperationException configurationFailed(
            Path path,
            String detail,
            Throwable cause) {
        return new EngineOperationException(
                EngineErrorCode.ENGINE_CONFIGURATION_ERROR,
                detail,
                "Repair the application XML configuration and try again.",
                path, null, null, null, null, null, null, null, null, cause);
    }

    private static <T extends Throwable> T findCause(Throwable exception, Class<T> causeType) {
        Throwable current = exception;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    @FunctionalInterface
    interface InputStreamOpener {
        InputStream open(Path path) throws IOException;
    }

    @FunctionalInterface
    interface CandidateMapper {
        LinkedHashMap<Integer, MarketEvent> map(GuessMarket root) throws EngineOperationException;
    }

    @FunctionalInterface
    interface TrustedSchemaOpener {
        InputStream open(String resourcePath) throws IOException;
    }
}
