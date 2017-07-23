package nl.ou.dpd.parsing;

import nl.ou.dpd.domain.node.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * An abstract parent class of specified ArgoUML parsers. This abstract parent class contains shared functionality of
 * inheriting classes.
 *
 * @author Martin de Boer
 */
public abstract class ArgoUMLAbstractParser {

    //attributes of the XMI used in the parser
    protected static final String NAME = "name";
    protected static final String ID = "xmi.id";
    protected static final String IDREF = "xmi.idref";
    protected static final String VISIBILITY = "visibility";
    protected static final String IS_ABSTRACT = "isAbstract";
    protected static final String HREF = "href";
    protected static final String KIND = "kind";
    protected static final String INPUT = "in";
    protected static final String LOWER = "lower";
    protected static final String UPPER = "upper";
    protected static final String IS_NAVIGABLE = "isNavigable";

    //tags of the XMI used in the parser
    protected static final String MODEL = "Model";
    protected static final String CLASS = "Class";
    protected static final String INTERFACE = "Interface";
    protected static final String ATTRIBUTE = "Attribute";
    protected static final String DATATYPE = "DataType";
    protected static final String OPERATION = "Operation";
    protected static final String PARAMETER = "Parameter";
    protected static final String ASSOCIATION = "Association";
    protected static final String ASSOCIATION_END = "AssociationEnd";
    protected static final String MULTIPLICITY_RANGE = "MultiplicityRange";
    protected static final String ABSTRACTION = "Abstraction";
    protected static final String DEPENDENCY = "Dependency";
    protected static final String GENERALIZATION = "Generalization";

    private static final Logger LOGGER = LogManager.getLogger(ArgoUMLAbstractParser.class);

    private XMLInputFactory xmlInputFactory;

    /**
     * A constructor expecting an {@link XMLInputFactory}.
     * <p>
     * This constructor has protected access so it can only be instantiated from within the same package (by the
     * ParserFactory or in a unit test in the same package).
     *
     * @param xmlInputFactory used for instantiating {@link XMLEventReader}s processing XML files.
     */
    protected ArgoUMLAbstractParser(XMLInputFactory xmlInputFactory) {
        this.xmlInputFactory = xmlInputFactory;
    }

    /**
     * Contains the passed events. Keeps track of the hierarchical structure of the XMI-tags.
     */
    protected Stack<XMLEvent> events = new Stack<>();

    /**
     * Contains the collected nodes from the XMI-tags.
     */
    protected Map<String, Node> nodes;

    protected void doParse(String filename) {
        try (InputStream input = new FileInputStream(new File(filename))) {
            final XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(input);
            handleEvents(eventReader);
        } catch (ParseException pe) {
            // We don't need to repackage a ParseException in a ParseException.
            // Rethrow ParseExceptions directly
            throw pe;
        } catch (Exception e) {
            String msg = String.format("The XMI file '%s' could not be parsed.", filename);
            error(msg, e);
        }
    }

    protected void handleEvents(XMLEventReader eventReader) throws XMLStreamException {
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            handleEvent(event);
        }
    }

    protected void handleEvent(XMLEvent event) {
        if (event.isStartElement()) {
            handleStartElement(event);
        }
        if (event.isEndElement()) {
            handleEndElement(event);
        }
    }

    protected abstract void handleStartElement(XMLEvent event);

    protected abstract void handleEndElement(XMLEvent event);

    /**
     * Return an Attributes Map with the attribute name as key and the attribute value as value, retrieved from the
     * specified {@link XMLEvent}.
     *
     * @param event the {@link XMLEvent} containing the attributes.
     * @return a Map containing attributes, extracted from the {@code event}.
     */
    protected Map<String, String> readAttributes(XMLEvent event) {
        final Map<String, String> attributes = new HashMap<>();
        final Iterator<Attribute> attributeIterator = event.asStartElement().getAttributes();
        while (attributeIterator.hasNext()) {
            final Attribute attribute = attributeIterator.next();
            final String name = attribute.getName().getLocalPart();
            final String value = attribute.getValue();
            attributes.put(name, value);
        }
        return attributes;
    }

    protected String getParentElementNameLocalPart() {
        return getStartElementNameLocalPart(events.peek());
    }

    protected String getStartElementNameLocalPart(XMLEvent event) {
        return event.asStartElement().getName().getLocalPart();
    }

    protected void error(String msg, Exception cause) {
        LOGGER.error(msg, cause);
        throw new ParseException(msg, cause);
    }
}
