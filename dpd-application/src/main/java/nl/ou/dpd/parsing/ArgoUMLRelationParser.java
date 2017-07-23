package nl.ou.dpd.parsing;

import nl.ou.dpd.domain.SystemUnderConsideration;
import nl.ou.dpd.domain.node.Node;
import nl.ou.dpd.domain.relation.Cardinality;
import nl.ou.dpd.domain.relation.Relation;
import nl.ou.dpd.domain.relation.RelationProperty;
import nl.ou.dpd.domain.relation.RelationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This class implements a parser for a xmi-file as generated by the ArgoUML-modeling tool (see
 * <a href="http://argouml.tigris.org/">http://argouml.tigris.org/</a>). If successful, the parsing results in a
 * {@link SystemUnderConsideration} with nodes as vertices and relations as edges. The Relations have one or more
 * relationtypes describing the kinds of relations the classes have.
 * <p>
 * IMPORTANT:
 * Pre-parsing to obtain a map of {@link Node}s with properties, attributes and operations set is necessary.
 * The key of this Map is the object id, the value the Node itself. Pre-parsing is taken care of by the
 * {@link ArgoUMLNodeParser}.
 *
 * @author Peter Vansweevelt
 * @author Martin de Boer
 */
public class ArgoUMLRelationParser extends ArgoUMLAbstractParser {
    private static final Logger LOGGER = LogManager.getLogger(ArgoUMLRelationParser.class);

    private static final List<String> eventTags = Arrays.asList(new String[]{
            MODEL, CLASS, INTERFACE, ASSOCIATION, ASSOCIATION_END,
            MULTIPLICITY_RANGE, ABSTRACTION, DEPENDENCY, GENERALIZATION
    });

    private Relation lastRelation; //holds the information of the last Relation.
    private SystemUnderConsideration system; // The system under consideration that will be returned.
    private Stack<Node> sourceAndTarget; // Keeps track of source and target vertices. If size is 2, an edge can be made.
    private Stack<Boolean> navigabilities; // Keeps track of the isNavigable attribute which determines directed or undirected associations.
    private Stack<Cardinality> cardinalities; // Keeps track of the cardinalities.

    /**
     * A constructor expecting an {@link XMLInputFactory}.
     * <p>
     * This constructor has package protected access so it can only be instantiated from within the same package (by the
     * ParserFactory or in a unit test in the same package).
     *
     * @param xmlInputFactory used for instantiating {@link XMLEventReader}s processing XML files.
     */
    ArgoUMLRelationParser(XMLInputFactory xmlInputFactory) {
        super(xmlInputFactory);
    }

    /**
     * Parses an xmi file with the specified {@code filename}.
     *
     * @param filename the name of the file to be parsed.
     * @param nodes    the nodes that were parsed previously by the {@link ArgoUMLNodeParser}.
     * @return a new {@link SystemUnderConsideration}.
     */
    public SystemUnderConsideration parse(String filename, Map<String, Node> nodes) {
        initParse(nodes);
        doParse(filename);
        LOGGER.info(String.format("Parsed %d relations from %s.", system.edgeSet().size(), filename));
        return system;
    }

    private void initParse(Map<String, Node> nodes) {
        this.nodes = nodes;
        navigabilities = new Stack<>();
        sourceAndTarget = new Stack<>();
        cardinalities = new Stack<>();
    }

    protected void handleStartElement(XMLEvent event) {
        switch (getStartElementNameLocalPart(event)) {
            case MODEL:
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
     * Remove an {@link XMLEvent} from the stack.
     *
     * @param event the {@link XMLEvent} to remove.
     */
    protected void handleEndElement(XMLEvent event) {
        if (eventTags.contains(event.asEndElement().getName().getLocalPart())) {
            events.pop();
        }
    }

    /**
     * Create a new {@link SystemUnderConsideration} with the id and name that are specified in the event.
     *
     * @param event the {@link XMLEvent} that specifies the name and the id of the {@link SystemUnderConsideration}
     * @return a new {@link SystemUnderConsideration} with the id and name specified in the event.
     */
    private SystemUnderConsideration createSystem(XMLEvent event) {
        final String id = readAttributes(event).get(ID);
        final String name = readAttributes(event).get(NAME);
        return new SystemUnderConsideration(id, name);
    }

    /**
     * Handles the attribute and Interface events, based on the event on level higher.
     *
     * @param event the {@link XMLEvent} to handle.
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
     * @param event the {@link XMLEvent} containing the name and id in its attributes.
     */
    private void handleAssociationEvent(XMLEvent event) {
        final String id = readAttributes(event).get(ID);
        final String name = readAttributes(event).get(NAME);
        lastRelation = findSystemRelationById(id);
        lastRelation = findSystemRelationById(id);
        if (lastRelation == null) {
            lastRelation = createIncompleteRelation(id, name);
        }
        final RelationType rt = findRelationTypeByString(getStartElementNameLocalPart(event));
        lastRelation.getRelationProperties().add(new RelationProperty(rt));
    }

    /**
     * Adds an inheritance, abstraction (realization) or generalization (inheritance)) relation to the system under
     * consideration.
     *
     * @param event the {@link XMLEvent} that contains the id and name in its attributes.
     */
    private void handleInheritance(XMLEvent event) {
        if (MODEL.equals(getParentElementNameLocalPart())) {
            final String id = readAttributes(event).get(ID);
            final String name = readAttributes(event).get(NAME);
            lastRelation = createIncompleteRelation(id, name);

            final RelationType rt = findRelationTypeByString(getStartElementNameLocalPart(event));
            lastRelation.getRelationProperties().add(new RelationProperty(rt));
        }
    }

    /**
     * Keep track of the navigabilities of the association in order to find out if the association is directed.
     *
     * @param event the {@link XMLEvent} containing the necessary XML attributes
     */
    private void handleAssociationEndEvent(XMLEvent event) {
        //association directed if first node !isNavigable and second node isNavigable
        //will be used when an association edge is created
        navigabilities.push(Boolean.valueOf(readAttributes(event).get(IS_NAVIGABLE)));
    }

    /**
     * Find or create an {@link Relation} and add the {@link RelationType}.
     *
     * @param event the {@link XMLEvent} containing the necessary XML attributes
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
     * @param event the {@link XMLEvent} containing the necessary XML attributes
     */
    private void handleMultiplicityRangeEvent(XMLEvent event) {
        if (!events.empty() && getParentElementNameLocalPart().equals(ASSOCIATION_END)) {
            int lower = Integer.parseInt(readAttributes(event).get(LOWER));
            int upper = Integer.parseInt(readAttributes(event).get(UPPER));
            RelationProperty rp = findRelationPropertyByType(lastRelation, RelationType.ASSOCIATES_WITH);
            if (rp != null) {
                cardinalities.push(new Cardinality(lower, upper));
                if (cardinalities.size() == 2) {
                    rp.setCardinalityRight(cardinalities.pop()).setCardinalityLeft(cardinalities.pop());
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
            addEdgeToSystem();
        }
    }

    private void addEdgeToSystem() {
        Node targetNode = sourceAndTarget.pop();
        Node sourceNode = sourceAndTarget.pop();
        boolean added = system.addEdge(sourceNode, targetNode, lastRelation);
        if (!added) {
            //add the new relationproperties to the existing relation
            addRelationProperties(lastRelation, system.getEdge(sourceNode, targetNode));
        }
        addReverseRelation(sourceNode, targetNode);
    }

    private Relation findSystemRelationById(String id) {
        return system.edgeSet().stream()
                .filter(relation -> relation.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds the {@link RelationProperty} (or properties) of the source to the target.
     *
     * @param source the {@link Relation} to get the properties from
     * @param target the {@link Relation} to add the properties to
     */
    private void addRelationProperties(Relation source, Relation target) {
        for (RelationProperty srcProperty : source.getRelationProperties()) {
            target.addRelationProperty(srcProperty);
        }
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
     * Adds a reverse relation between the specified nodes when the relation type is ASSOCIATES_WITH and navigabilities
     * are {@code true}. Only processed if the association is undirected (= bidirectional).
     *
     * @param sourceNode the source node of the relation
     * @param targetNode the target node of the relation
     */
    private void addReverseRelation(Node sourceNode, Node targetNode) {
        final Relation relation = system.getEdge(sourceNode, targetNode);
        boolean isAssociation = containsRelationType(relation, RelationType.ASSOCIATES_WITH);
        boolean reverseRelationExists = system.containsEdge(targetNode, sourceNode);
        if (reverseRelationExists) {
            system.getEdge(targetNode, sourceNode).getRelationProperties().add(new RelationProperty(RelationType.ASSOCIATES_WITH));
        } else {
            if (isAssociation && navigabilities.size() == 2) {
                if (navigabilities.pop() && navigabilities.pop()) {
                    lastRelation = createReverseAssociation(relation, sourceNode, targetNode);
                }
            }
        }
    }

    private Relation createReverseAssociation(Relation originalRelation, Node originalSourceNode, Node originalTargetNode) {
        final Relation relation = new Relation(originalRelation.getId() + "-reversed", null);
        if (originalRelation.getName() != null) {
        	relation.setName(originalRelation.getName() + "-reversed");
        }
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
     * Creates a new {@link Relation} with the name and id, specified in the event attributes.
     * Relationproperties are not set.
     *
     * @param id   the id of the relation
     * @param name the name of the relation
     * @return the newly created {@link Relation}
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

}
