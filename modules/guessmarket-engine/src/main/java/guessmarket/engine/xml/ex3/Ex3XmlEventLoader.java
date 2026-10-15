package guessmarket.engine.xml.ex3;

import guessmarket.engine.EngineErrorCode;
import guessmarket.engine.EngineOperationException;
import guessmarket.engine.MarketWorld;
import guessmarket.engine.xml.ex3.generated.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public final class Ex3XmlEventLoader {
    private static final String SCHEMA = "/guessmarket/engine/xml/ex3/GM-EX3-Schema.xsd";

    public ParsedEventBatch parse(InputStream input, String filename) throws EngineOperationException {
        Path path = null;
        Unmarshaller unmarshaller = unmarshaller(path);
        XMLReader reader = secureReader(path);
        try {
            Object value = unmarshaller.unmarshal(new SAXSource(reader, new InputSource(input)));
            if (!(value instanceof GuessMarket root))
                throw failure(EngineErrorCode.XML_STRUCTURE_INVALID, path,
                        "An EX3 Guess-Market root is required.", null);
            return new Ex3JaxbEventMapper().map(root);
        } catch (JAXBException failure) {
            IOException io = cause(failure, IOException.class);
            if (io != null && !(io instanceof CharConversionException))
                throw failure(EngineErrorCode.XML_FILE_ACCESS_FAILED, path, "The XML file could not be read.", failure);
            throw failure(EngineErrorCode.XML_STRUCTURE_INVALID, path,
                    "The file is malformed or does not match the EX3 XML schema.", failure);
        }
    }

    private static Unmarshaller unmarshaller(Path path) throws EngineOperationException {
        try (InputStream schema = Ex3XmlEventLoader.class.getResourceAsStream(SCHEMA)) {
            if (schema == null) throw failure(EngineErrorCode.ENGINE_CONFIGURATION_ERROR, path,
                    "The trusted EX3 schema is missing from the application.", null);
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Unmarshaller unmarshaller = JAXBContext.newInstance(GuessMarket.class).createUnmarshaller();
            unmarshaller.setSchema(factory.newSchema(new StreamSource(schema)));
            return unmarshaller;
        } catch (IOException | SAXException | JAXBException failure) {
            throw failure(EngineErrorCode.ENGINE_CONFIGURATION_ERROR, path,
                    "The EX3 XML parser could not be configured.", failure);
        }
    }

    private static XMLReader secureReader(Path path) throws EngineOperationException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return reader;
        } catch (ParserConfigurationException | SAXException failure) {
            throw failure(EngineErrorCode.ENGINE_CONFIGURATION_ERROR, path,
                    "The secure XML reader could not be configured.", failure);
        }
    }

    private static EngineOperationException failure(EngineErrorCode code, Path path, String detail, Throwable cause) {
        String recovery = switch (code) {
            case ENGINE_CONFIGURATION_ERROR -> "Repair the application XML configuration and restart.";
            case XML_FILE_ACCESS_FAILED -> "Check the file permissions and try again.";
            default -> "Choose a valid EX3 XML file or correct this file and try again.";
        };
        SAXParseException parse = cause(cause, SAXParseException.class);
        return new EngineOperationException(code, detail, recovery, path, null, null, null,
                null, null, null, parse == null ? null : parse.getLineNumber(),
                parse == null ? null : parse.getColumnNumber(), cause);
    }

    private static <T extends Throwable> T cause(Throwable failure, Class<T> type) {
        while (failure != null) {
            if (type.isInstance(failure)) return type.cast(failure);
            failure = failure.getCause();
        }
        return null;
    }
}
