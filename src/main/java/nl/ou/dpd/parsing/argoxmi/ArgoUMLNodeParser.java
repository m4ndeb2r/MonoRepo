package nl.ou.dpd.parsing.argoxmi;

import nl.ou.dpd.domain.node.Node;
import nl.ou.dpd.domain.node.NodeType;
import nl.ou.dpd.domain.node.Operation;
import nl.ou.dpd.domain.node.Parameter;
import nl.ou.dpd.domain.node.Visibility;
import nl.ou.dpd.parsing.ParseException;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class implements a parser for a xmi-file as generated by the ArgoUML-modelling tool (see
 * <a href="http://argouml.tigris.org/">http://argouml.tigris.org/</a>), including parsing nodes, attributes and
 * operations. If successful, the parsing results in a map with the id as key and values of {@link Node}s with
 * attributes, operations and all the necessary properties that could be read from the XMI-file. Relations are not
 * parsed.
 * <p>
 * This is the first step of the parsing process. The successive steps are inplemented by {@link ArgoUMLRelationParser}
 * and {@link SystemRelationsExtractor}.
 *
 * @author Peter Vansweevelt
 * @author Martin de Boer
 * @see ArgoUMLRelationParser
 * @see SystemRelationsExtractor
 */
public class ArgoUMLNodeParser {
    private static final Logger LOGGER = LogManager.getLogger(ArgoUMLNodeParser.class);

    //Datatype xmi.id endings
    private static final String STRING = "87E";
    private static final String INTEGER = "87C";
    private static final String UNLIMITED_INTEGER = "87D";
    private static final String BOOLEAN = "880";

    //attributes of the XMI
    private static final String NAME = "name";
    private static final String ID = "xmi.id";
    private static final String VISIBILITY = "visibility";
    private static final String IS_ABSTRACT = "isAbstract";
    private static final String IDREF = "xmi.idref";
    private static final String HREF = "href";
    private static final String KIND = "kind";

    //tags of the XMI
    private static final String MODEL = "Model";
    private static final String CLASS = "Class";
    private static final String INTERFACE = "Interface";
    private static final String ATTRIBUTE = "Attribute";
    private static final String DATATYPE = "DataType";
    private static final String OPERATION = "Operation";
    private static final String PARAMETER = "Parameter";

    private static final List<String> eventTags = Arrays.asList(new String[]{
            MODEL, CLASS, INTERFACE, ATTRIBUTE, DATATYPE, OPERATION, PARAMETER
    });

    private Map<String, Node> nodes;
    private Stack<XMLEvent> events = new Stack<>();
    private Operation parentOperation;

    /**
     * Parses an xmi file with the specified {@code filename}.
     *
     * @param filename the name of the file to be parsed.
     * @return a map of {@link Node}s including attributes and operations. The key is represented by the node id, the
     * value is represented by the node itself.
     */
    public Map<String, Node> parse(String filename) {
        nodes = new HashMap<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            InputStream input = new FileInputStream(new File(filename));
            XMLEventReader eventReader = factory.createXMLEventReader(input);
            handleEvents(eventReader);
        } catch (ParseException pe) {
            // We don't need to repackage a ParseException in a ParseException.
            // Rethrow ParseExceptions directly
            throw pe;
        } catch (Exception e) {
            final String msg = "The XMI file " + filename + " could not be parsed.";
            error(msg, e);
        }
        return nodes;
    }

    private void handleEvents(XMLEventReader eventReader) throws XMLStreamException {
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            handleEvent(event);
        }
    }

    private void handleEvent(XMLEvent event) {
        if (event.isStartElement()) {
            handleStartElement(event);
        }
        if (event.isEndElement()) {
            handleEndElement(event);
        }
    }

    private void handleStartElement(XMLEvent event) {
        switch (getStartElementNameLocalPart(event)) {
            case MODEL:
                events.push(event);
                break;
            case CLASS:
            case INTERFACE:
                handleNodeEvent(event);
                events.push(event);
                break;
            case ATTRIBUTE:
                handleAttributeEvent(event);
                events.push(event);
                break;
            case OPERATION:
                handleOperationEvent(event);
                events.push(event);
                break;
            case DATATYPE:
                //internal ArgoUML datatype of an attribute
                handleDatatypeEvent(event);
                events.push(event);
                break;
            case PARAMETER:
                handleParameterEvent(event);
                events.push(event);
                break;
            default:
                break;
        }
    }

    private void handleEndElement(XMLEvent event) {
        if (eventTags.contains(event.asEndElement().getName().getLocalPart())) {
            events.pop();
        }
    }

    private void handleNodeEvent(XMLEvent event) {
        //look for the event one level higher
        switch (getParentElementNameLocalPart()) {
            case MODEL:
                handleModelEvent(event);
                break;
            case ATTRIBUTE:
                setAttributeType(event, readAttributes(event).get(IDREF));
                break;
            case PARAMETER:
                setParameterType(event, readAttributes(event).get(IDREF));
                break;
            default:
                break;
        }
    }

    /**
     * Create a node (a class or interface) and add it to the {@link Node}s map.
     *
     * @param event
     */
    private void handleModelEvent(XMLEvent event) {
        Node node = findOrcreateNode(event);
        if (node != null) {
            addNode(node);
        }
    }

    /**
     * Create an attribute WITHOUT its type (null) and add it to the attributes of Node.  A type will be added as a
     * special handling of the Class/Interface event.
     *
     * @param event the attribute event
     */
    private void handleAttributeEvent(XMLEvent event) {
        Node node = nodes.get(readAttributes(events.peek()).get(ID));
        createIncompleteAttribute(event, node);
    }

    /**
     * Create an operation WITHOUT the types (null) of return values and parameters and add it to the operations of the
     * node. A type will be added as a special handling of the Class/Interface event.
     *
     * @param event the operation event
     */
    private void handleOperationEvent(XMLEvent event) {
        switch (getParentElementNameLocalPart()) {
            case CLASS:
            case INTERFACE:
                Node node = nodes.get(readAttributes(events.peek()).get(ID));
                createIncompleteOperation(event, node);
                break;
        }
    }

    /**
     * Create or find the node of this particular Datatype and set this type to the corresponding {@link Attribute} or
     * {@link Parameter}.
     *
     * @param event the datatype event
     * @see http://argouml.tigris.org//profiles/uml14/default-uml14.xmi
     */
    private void handleDatatypeEvent(XMLEvent event) {
        //make a node if necessary
        Node node = findOrCreateDatatypeNode(event);
        if (node != null) {
            addNode(node);
        }
        //look for the event one level higher
        switch (getParentElementNameLocalPart()) {
            case ATTRIBUTE:
                setAttributeType(event, readAttributes(event).get(HREF));
                break;
            case PARAMETER:
                setParameterType(event, readAttributes(event).get(HREF));
                break;
        }
    }

    /**
     * If the kind of the {@link Parameter} is 'in' create a Parameter without type, else (the kind is 'return')
     * memorize the {@link Operation} under consideration. Types will be set as a special handling of the
     * CLASS/INTERFACE event.
     *
     * @param event the parameter event
     */
    private void handleParameterEvent(XMLEvent event) {
        final Map<String, String> attributes = readAttributes(event);
        final String opId = readAttributes(events.peek()).get(ID);
        final Operation operation = findOperationById(opId);
        if ("in".equals(attributes.get(KIND))) {
            createIncompleteParameter(event, operation);
        } else {
            parentOperation = operation;
        }
    }

    /**
     * Set the type of the {@link Attribute} as a {@link Node}.
     *
     * @param event
     * @param idref
     */
    private void setAttributeType(XMLEvent event, String idref) {
        String attrId = readAttributes(events.peek()).get(ID);
        final nl.ou.dpd.domain.node.Attribute attr = findAttributeById(attrId);
        Node node = nodes.get(idref);
        if (node == null) {
            node = new Node(idref);
            addNode(node);
        }
        attr.setType(node);
    }

    /**
     * Set the type of the {@link Parameter} as a {@link Node}.
     *
     * @param event
     * @param idref
     */
    private void setParameterType(XMLEvent event, String idref) {
        String kind = readAttributes(events.peek()).get(KIND);
        if ("in".equals(kind)) {
            setParameterInType(event, idref);
        } else {
            setParameterReturnType(event, idref);
        }
    }


    /**
     * Set the type of the {@link Parameter} of kind 'in' as a {@link Node}.
     *
     * @param event
     * @param idref
     */
    private void setParameterInType(XMLEvent event, String idref) {
        String paramId = readAttributes(events.peek()).get(ID);
        Parameter parameter = findParameterById(paramId);
        Node node = nodes.get(idref);
        if (node == null) {
            node = new Node(idref);
            addNode(node);
        }
        parameter.setType(node);
    }

    /**
     * Set the type of the {@link Parameter} of kind 'return' as a {@link Node}.
     *
     * @param event
     * @param idref
     */
    private void setParameterReturnType(XMLEvent event, String idref) {
        Node node = nodes.get(idref);
        if (node == null) {
            node = new Node(idref);
            addNode(node);
        }
        parentOperation.setReturnType(node);
    }

    /**
     * Add a node to the {@link Node}s map with key the id and value the {@link Node}.
     *
     * @param node
     */
    private void addNode(Node node) {
        nodes.put(node.getId(), node);
    }

    /**
     * Find a {@link Node} with the ID specified in the attributes of the given event.
     * If a node with this ID does not exist, create it.
     * Finally, set the appropriate properties of the (found or newly created) node.
     *
     * @param event the {@link XMLEvent} currently handled. Contains the attributes necessary for the creation of a node.
     */
    private Node findOrcreateNode(XMLEvent event) {
        final Map<String, String> attributes = readAttributes(event);
        final String id = attributes.get(ID);
        Node node = null;
        if (id != null) {
            node = nodes.get(id);
            if (node == null) {
                node = new Node(id);
                node = addNodeProperties(node, event);
            } else {
                addNodeProperties(node, event);
            }
        }
        return node;
    }

    /**
     * Set the properties of a {@link Node}, including the name, visibility, type and abstractness.
     *
     * @param node  the {@link Node} to be handled
     * @param event currently handled, containing the necessary attributes.
     * @return the newly updated {@link Node}
     */
    private Node addNodeProperties(Node node, XMLEvent event) {
        final Map<String, String> attributes = readAttributes(event);
        final String name = attributes.get(NAME);
        final NodeType type = findNodeType(event);
        final Visibility visibility = Visibility.valueOfIgnoreCase(attributes.get(VISIBILITY));
        node.setName(name);
        node.setVisibility(visibility);
        node.addType(type);
        return node;
    }

    private NodeType findNodeType(XMLEvent event) {
        final String typeString = getStartElementNameLocalPart(event).toUpperCase();
        final boolean isAbstract = Boolean.parseBoolean(readAttributes(event).get(IS_ABSTRACT));
        if (typeString.equals("CLASS")) {
            if (isAbstract) {
                return NodeType.ABSTRACT_CLASS;
            } else {
                return NodeType.CONCRETE_CLASS;
            }
        }
        if (typeString.equals("INTERFACE")) {
            return NodeType.INTERFACE;
        }
        return null;
    }

    /**
     * Find a {@link Node} of the type DATATYPE with the ID specified in the attributes of the given event.
     * If a node with this ID does not exist, create it.
     * Finally, set the appropriate properties of the (found or newly created) node.
     *
     * @param event the {@link XMLEvent} currently handled. Contains the attributes necessary for the creation of a node.
     */
    private Node findOrCreateDatatypeNode(XMLEvent event) {
        final Map<String, String> attributes = readAttributes(event);
        final String id = attributes.get(HREF);
        Node node = null;
        if (id != null) {
            node = nodes.get(id);
            if (node == null) {
                node = new Node(id);
                node = addDataTypeProperties(node, event);
            }
        }
        return node;
    }

    /**
     * Set the properties of a {@link Node} of the type DATATYPE, including the name, visibility, type and abstractness.
     *
     * @param node  the {@link Node} to be handled
     * @param event currently handled, containing the necessary attributes.
     * @return the newly updated {@link Node}
     */
    private Node addDataTypeProperties(Node node, XMLEvent event) {
        String id = readAttributes(event).get(HREF).substring(105);
        switch (id) {
            case STRING:
                node.setName("String");
                break;
            case UNLIMITED_INTEGER:
                node.setName("UnlimitedInteger");
            case INTEGER:
                node.setName("Integer");
                break;
            case BOOLEAN:
                node.setName("Boolean");
                break;
        }
        node.setVisibility(Visibility.PUBLIC);
        node.addType(NodeType.DATATYPE);
        return node;
    }

    /**
     * Create a new node attribute with the name and visibility specified in the event's attributes.
     * The type is not set.
     *
     * @param event the {@link XMLEvent} containing the name and type in its attributes.
     * @return the newly created attribute (found in the list of Nodes).
     */
    private nl.ou.dpd.domain.node.Attribute createIncompleteAttribute(XMLEvent event, Node parentNode) {
        final Map<String, String> attributes = readAttributes(event);
        final String id = attributes.get(ID);
        final String name = attributes.get(NAME);
        final Visibility visibility = Visibility.valueOfIgnoreCase(attributes.get(VISIBILITY));
        final nl.ou.dpd.domain.node.Attribute attr = new nl.ou.dpd.domain.node.Attribute(id, parentNode);
        attr.setName(name);
        attr.setVisibility(visibility);
        return attr;
    }

    /**
     * Create a new method 'in'-parameter with the name asspecified in the event's attributes.
     * The type is not set.
     *
     * @param event the {@link XMLEvent} containing the name and type in its attributes.
     * @return the newly created attribute (found in the list of Nodes).
     */
    private Parameter createIncompleteParameter(XMLEvent event, Operation operation) {
        final Map<String, String> attributes = readAttributes(event);
        final String id = attributes.get(ID);
        final String name = attributes.get(NAME);
        final Parameter parameter = new Parameter(id, operation);
        parameter.setName(name);
        return parameter;
    }

    /**
     * Create a new node operation with the name, type and visibility specified in the event's attributes.
     * ReturnType and Parameters are not set.
     *
     * @param event the {@link XMLEvent} containing the name and type in its attributes.
     * @return the newly created attribute (found in the list of Nodes).
     */
    private Operation createIncompleteOperation(XMLEvent event, Node parentNode) {
        final Map<String, String> attributes = readAttributes(event);
        final String id = attributes.get(ID);
        final String name = attributes.get(NAME);
        final Visibility visibility = Visibility.valueOfIgnoreCase(attributes.get(VISIBILITY));
        final Operation operation = new Operation(id, parentNode);
        operation.setName(name);
        operation.setVisibility(visibility);
        return operation;
    }

    /**
     * Find an {@link Attribute} by ID.
     *
     * @param id
     * @return an {@link Attribute}
     */
    private nl.ou.dpd.domain.node.Attribute findAttributeById(String id) {
        for (Node n : nodes.values()) {
            for (nl.ou.dpd.domain.node.Attribute a : n.getAttributes()) {
                if (id.equals(a.getId())) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Find an {@link Operation} by ID.
     *
     * @param id
     * @return an {@link Operation}
     */
    private Operation findOperationById(String id) {
        for (Node n : nodes.values()) {
            for (Operation op : n.getOperations()) {
                if (id.equals(op.getId())) {
                    return op;
                }
            }
        }
        return null;
    }

    /**
     * Find an {@link Parameter} by ID.
     *
     * @param id
     * @return an {@link Parameter}
     */
    private Parameter findParameterById(String id) {
        for (Node n : nodes.values()) {
            for (Operation op : n.getOperations()) {
                for (Parameter param : op.getParameters())
                    if (id.equals(param.getId())) {
                        return param;
                    }
            }
        }
        return null;
    }

    /**
     * Return an Attributes Map with the attribute name as key and the attribute value as value, retrieved from the
     * specified {@link XMLEvent}.
     *
     * @param event the {@link XMLEvent} containing the attributes.
     * @return a Map containing attributes, extracted from the {@code event}.
     */
    private Map<String, String> readAttributes(XMLEvent event) {
        Map<String, String> attributes = new HashMap<>();
        Iterator<Attribute> attrIterator = event.asStartElement().getAttributes();
        while (attrIterator.hasNext()) {
            Attribute attr = attrIterator.next();
            attributes.put(attr.getName().getLocalPart(), attr.getValue());
        }
        return attributes;
    }

    private String getParentElementNameLocalPart() {
        return getStartElementNameLocalPart(events.peek());
    }

    private String getStartElementNameLocalPart(XMLEvent event) {
        return event.asStartElement().getName().getLocalPart();
    }

    private void error(String msg, Exception cause) {
        LOGGER.error(msg, cause);
        throw new ParseException(msg, cause);
    }

}
