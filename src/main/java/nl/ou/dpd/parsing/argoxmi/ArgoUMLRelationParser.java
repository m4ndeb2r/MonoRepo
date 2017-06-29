package nl.ou.dpd.parsing.argoxmi;

import nl.ou.dpd.domain.SystemUnderConsideration;
import nl.ou.dpd.domain.node.Node;
import nl.ou.dpd.domain.relation.Cardinality;
import nl.ou.dpd.domain.relation.Relation;
import nl.ou.dpd.domain.relation.RelationProperty;
import nl.ou.dpd.domain.relation.RelationType;
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
 * This class implements a parser for a xmi-file as generated by the ArgoUML-modeling tool (see
 * <a href="http://argouml.tigris.org/">http://argouml.tigris.org/</a>)
 * If successful, the parsing results in a
 * {@link SystemUnderConsideration} with nodes as vertices and relations as edges.
 * The Relations have one or more relationtypes describing the kinds of relations the classes have.
 * IMPORTANT:
 * A pre-parsing to obtain a map of Nodes with properties, attributes and operations set is necessary.
 * The key of this Map is the object id, the value the Node itself.
 *
 * @author Peter Vansweevelt
 * @author Martin de Boer
 */
public class ArgoUMLRelationParser {
    private static final Logger LOGGER = LogManager.getLogger(ArgoUMLRelationParser.class);

    //attributes of the XMI used in the parser
    private static final String NAME = "name";
    private static final String ID = "xmi.id";
    private static final String IDREF = "xmi.idref";
    private static final String LOWER = "lower";
    private static final String UPPER = "upper";
    private static final String IS_NAVIGABLE = "isNavigable";

    //tags of the XMI used in the parser
    private static final String MODEL = "Model";
    private static final String CLASS = "Class";
    private static final String INTERFACE = "Interface";
    private static final String ATTRIBUTE = "Attribute";
    private static final String ASSOCIATION = "Association";
    private static final String ASSOCIATION_END = "AssociationEnd";
    private static final String MULTIPLICITY_RANGE = "MultiplicityRange";
    private static final String ABSTRACTION = "Abstraction";
    private static final String DEPENDENCY = "Dependency";
    private static final String GENERALIZATION = "Generalization";

    private static final List<String> eventTags = Arrays.asList(new String[]{
            MODEL, CLASS, INTERFACE, ASSOCIATION, ASSOCIATION_END,
            MULTIPLICITY_RANGE, ABSTRACTION, DEPENDENCY, GENERALIZATION
    });

    private Relation lastRelation; //holds the information of the last Relation.
    private SystemUnderConsideration system; //The system under consideration that will be returned.
    private Map<String, Node> nodes;
    private Stack<Node> sourceAndTarget; //used to keep track of source and target vertices. If size is 2, an edge can me made.
    private Stack<Boolean> navigabilities; //used to keep track of the isNavigable attribute which determines directed or undirected associations.
    private Stack<Cardinality> cardinalities; //used to keep track of the cardinalities.
    private Stack<XMLEvent> events = new Stack<>(); //holds the passed events. Used to keep track of the hierarchical structure of the XMI-tags.

    /**
     * Parses an xmi file with the specified {@code filename}.
     *
     * @param filename the name of the file to be parsed.
     * @return a new {@link SystemUnderConsideration}.
     */
    public SystemUnderConsideration parse(String filename, Map<String, Node> nodes) {
        this.nodes = nodes;
        navigabilities = new Stack<>();
        sourceAndTarget = new Stack<>();
        cardinalities = new Stack<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();

        try (InputStream input = new FileInputStream(new File(filename))) {
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
        return system;
    }

    /**
     * General method to handle the events of the XMI.
     *
     * @param eventReader
     * @throws XMLStreamException
     */
    private void handleEvents(XMLEventReader eventReader) throws XMLStreamException {
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            handleEvent(event);
        }
    }

    /**
     * General method to handle events. Start and end elements are only handled if there is an action bound to the
     * event.
     *
     * @param event the event to handle. This can be a start element as well as an end element.
     */
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
                // Create the SystemUnderConsideration
                system = createSystem(event);
                nodes.values().forEach(node -> system.addVertex(node));
                events.push(event);
                break;
            case CLASS:
            case INTERFACE:
                handleNodeEvent(event);
                events.push(event);
                break;
            case ASSOCIATION:
                handleAssociationEvent(event);
                events.push(event);
                break;
            case ABSTRACTION:
            case GENERALIZATION:
                handleInheritance(event);
                events.push(event);
                break;
            case ASSOCIATION_END:
                handleAssociationEndEvent(event);
                events.push(event);
                break;
            case DEPENDENCY:
                handleDependencyEvent(event);
                events.push(event);
                break;
            case MULTIPLICITY_RANGE:
                handleMultiplicityRangeEvent(event);
                events.push(event);
                break;
            default:
                break;
        }
    }

    /**
     * Remove the event from the stack.
     *
     * @param event
     */
    private void handleEndElement(XMLEvent event) {
        if (eventTags.contains(event.asEndElement().getName().getLocalPart())) {
            events.pop();
        }
    }

    /**
     * Create a new {@link SystemUnderConsideration} with the id and name that are specified in the event.
     *
     * @param event the XML event, specifies the name and the id of the {@link SystemUnderConsideration}
     * @return a new {@link SystemUnderConsideration} with the id and name specified in the event.
     */
    private SystemUnderConsideration createSystem(XMLEvent event) {
        String id = readAttributes(event).get(ID);
        String name = readAttributes(event).get(NAME);
        return new SystemUnderConsideration(id, name);
    }

    /**
     * Handles the attribute and Interface events, based on the event on level higher.
     *
     * @param event
     */
    private void handleNodeEvent(XMLEvent event) {
        //look for the event one level higher
        switch (getParentElementNameLocalPart()) {
            case ASSOCIATION_END:
            case ABSTRACTION:
            case GENERALIZATION:
            case DEPENDENCY:
                setSourceOrTargetNode(event);
                break;
            default:
                break;
        }
    }

    /**
     * Find or create an {@link Relation} and add the {@link RelationType}.
     *
     * @param event
     */
    private void handleAssociationEvent(XMLEvent event) {
        final String id = readAttributes(event).get(ID);
        final String name = readAttributes(event).get(NAME);
        lastRelation = findSystemRelationById(id);
        lastRelation = findSystemRelationById(id);
        if (lastRelation == null) {
            lastRelation = createIncompleteRelation(id, name);
        }
        RelationType rt = findRelationTypeByString(getStartElementNameLocalPart(event));
        lastRelation.getRelationProperties().add(new RelationProperty(rt));
    }

    /**
     * Adds an inheritance, abstraction (realization) or generalization (inheritance)) relation to the system under
     * consideration.
     *
     * @param event
     */
    private void handleInheritance(XMLEvent event) {
        if (MODEL.equals(getParentElementNameLocalPart())) {
            final String id = readAttributes(event).get(ID);
            final String name = readAttributes(event).get(NAME);
            lastRelation = createIncompleteRelation(id, name);
            RelationType rt = findRelationTypeByString(getStartElementNameLocalPart(event));
            lastRelation.getRelationProperties().add(new RelationProperty(rt));
        }
    }

    /**
     * Keep track of the navigabilities of the association in order to find
     * out if the association is directed.
     *
     * @param event
     */
    private void handleAssociationEndEvent(XMLEvent event) {
        //association directed if first node !isNavigable and second node isNavigable
        //will be used when an association edge is created
        navigabilities.push(Boolean.valueOf(readAttributes(event).get(IS_NAVIGABLE)));
    }

    /**
     * Find or create an {@link Relation} and add the {@link RelationType}.
     *
     * @param event
     */
    private void handleDependencyEvent(XMLEvent event) {
        if (CLASS.equals(getParentElementNameLocalPart())) {
            String dependencyId = readAttributes(event).get(ID);
            if (dependencyId != null && findSystemRelationById(dependencyId) == null) {
                //create an incomplete relation if the relation does not exist yet
                lastRelation = createIncompleteRelation(dependencyId, null);
                lastRelation.getRelationProperties().add(new RelationProperty(RelationType.DEPENDS_ON));
            }
        }
    }

    /**
     * Sets the multiplicity of an association end.
     *
     * @param event
     */
    private void handleMultiplicityRangeEvent(XMLEvent event) {
        if (!events.empty() && getParentElementNameLocalPart().equals(ASSOCIATION_END)) {
            int lower = Integer.parseInt(readAttributes(event).get(LOWER));
            int upper = Integer.parseInt(readAttributes(event).get(UPPER));
            RelationProperty rp = findRelationPropertyByType(lastRelation, RelationType.ASSOCIATES_WITH);
            if (rp != null) {
                cardinalities.push(new Cardinality(lower, upper));
                if (cardinalities.size() == 2) {
                    rp.setCardinalityRight(cardinalities.pop());
                    rp.setCardinalityLeft(cardinalities.pop());
                }
            }
        }
    }

    private void setSourceOrTargetNode(XMLEvent event) {
        final String idref = readAttributes(event).get(IDREF);
        Node node = nodes.get(idref);
        //add node to system (if new Node, e.g. datatype)
        system.addVertex(node);
        sourceAndTarget.push(node);
        if (sourceAndTarget.size() == 2) {
            addEdgeToSystem(event);
        }
    }

    private void addEdgeToSystem(XMLEvent event) {
        Node targetNode = sourceAndTarget.pop();
        Node sourceNode = sourceAndTarget.pop();
        system.getEdge(sourceNode, targetNode);
        system.addEdge(sourceNode, targetNode, lastRelation);
        system.getEdge(sourceNode, targetNode);
        addReverseRelation(event, sourceNode, targetNode);
    }

    private Relation findSystemRelationById(String id) {
        return system.edgeSet().stream()
                .filter(relation -> relation.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private RelationType findRelationTypeByString(String relationType) {
        switch (relationType) {
            case ASSOCIATION:
                return RelationType.ASSOCIATES_WITH;
            case ABSTRACTION:
                return RelationType.IMPLEMENTS;
            case GENERALIZATION:
                return RelationType.INHERITS_FROM;
            case ATTRIBUTE:
                return RelationType.HAS_ATTRIBUTE_OF;
            default:
                return null;
        }
    }

    /**
     * TODO
     * Only processed if the association is undirected (= bidirected).
     *
     * @param sourceNode
     * @param targetNode
     */
    private void addReverseRelation(XMLEvent event, Node sourceNode, Node targetNode) {
        //if the type is association and navigabilities are true
        //add a reverse edge with the same relationtype
        Relation relation = system.getEdge(sourceNode, targetNode);
        boolean isAssociation = containsRelationType(relation, RelationType.ASSOCIATES_WITH);
        boolean reverseRelationExists = system.containsEdge(targetNode, sourceNode);
        if (reverseRelationExists) {
            system.getEdge(targetNode, sourceNode).getRelationProperties().add(new RelationProperty(RelationType.ASSOCIATES_WITH));
        } else {
            if (isAssociation && navigabilities.size() == 2) {
                if (navigabilities.pop() && navigabilities.pop()) {
                    lastRelation = createReverseAssociation(event, relation, sourceNode, targetNode);
                }
            }
        }
    }

    private Relation createReverseAssociation(XMLEvent event, Relation originalRelation, Node originalSourceNode, Node originalTargetNode) {
        final Relation relation = new Relation(originalRelation.getId() + "-reversed", null);
        final RelationProperty originalRelationProperty = findRelationPropertyByType(originalRelation, RelationType.ASSOCIATES_WITH);
        final RelationProperty relationProperty = new RelationProperty(
                RelationType.ASSOCIATES_WITH,
                originalRelationProperty.getCardinalityRight(),
                originalRelationProperty.getCardinalityLeft());
        relation.addRelationProperty(relationProperty);
        system.addEdge(originalTargetNode, originalSourceNode, relation);
        return relation;
    }

    private RelationProperty findRelationPropertyByType(Relation relation, RelationType relationType) {
        return relation.getRelationProperties()
                .stream()
                .filter(rp -> relationType.equals(rp.getRelationType()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates a new {@link Edge} with the name and id, specified in the event attributes.
     * Relationproperties are not set.
     *
     * @param event the {@link XMLEvent} containing the name and id in its attributes.
     * @return the newly created {@link Edge}
     */
    private Relation createIncompleteRelation(String id, String name) {
        final Relation relation = new Relation(id, name);
        return relation;
    }

    private boolean containsRelationType(Relation relation, RelationType relationtype) {
        return relation.getRelationProperties()
                .stream()
                .anyMatch(rp -> relationtype.equals(rp.getRelationType()));
    }

    /**
     * Return an Attributes Map with the attribute name as key and the attribute value as value, retrieved from the
     * specified {@link XMLEvent}.
     *
     * @param event the {@link XMLEvent} containing the attributes.
     * @return a Map containing attributes, extracted from the {@code event}.
     */
    private Map<String, String> readAttributes(XMLEvent event) {
        final Map<String, String> attributes = new HashMap<>();
        ((Iterator<Attribute>) event.asStartElement().getAttributes())
                .forEachRemaining(attr -> attributes.put(attr.getName().getLocalPart(), attr.getValue()));
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
