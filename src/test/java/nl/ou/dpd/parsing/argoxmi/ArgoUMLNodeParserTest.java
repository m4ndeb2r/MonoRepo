package nl.ou.dpd.parsing.argoxmi;

import nl.ou.dpd.domain.node.Attribute;
import nl.ou.dpd.domain.node.Node;
import nl.ou.dpd.domain.node.NodeType;
import nl.ou.dpd.domain.node.Operation;
import nl.ou.dpd.domain.node.Parameter;
import nl.ou.dpd.domain.node.Visibility;
import nl.ou.dpd.parsing.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link SystemRelationsExtractor} class.
 *
 * @author Martin de Boer
 * @author Peter Vansweevelt
 */
public class ArgoUMLNodeParserTest {

    // A test file containing valid XML.
    private static final String VALID_ADAPTER = "/adapters_structures_association.xmi";
    // A  more complicated test file containing valid XML.
    private static final String VALID_ADAPTERS = "/adapters.xmi";
    // A test file containing invalid XML.
    private static final String INVALID_XML = "/invalid.xml";

    /**
     * Exception rule.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Tests the exception handling in case of document which could not be parsed, resulting in a {@link XMLStreamException} during
     * parsing a template file by a {@link TemplatesParserWithConditions}.
     */
    @Test
    public void testXMLStreamException() {
        final String path = getPath(INVALID_XML);
        thrown.expect(ParseException.class);
        thrown.expectCause(is(XMLStreamException.class));
        thrown.expectMessage("The XMI file '" + path + "' could not be parsed.");
        new ArgoUMLNodeParser().parse(path);
    }

    /**
     * Tests the exception handling in case of a {@link IOException} during parsing a template file by a
     * {@link TemplatesParserWithConditions}.
     */
    @Test
    public void testFileNotFoundException() {
        thrown.expect(ParseException.class);
        thrown.expectCause(is(FileNotFoundException.class));
        thrown.expectMessage("The XMI file 'missing.xml' could not be parsed.");
        new ArgoUMLNodeParser().parse("missing.xml");
    }

    /**
     * Test the happy flow of parsing an XMI input file by the {@link ArgoUMLToSystemGraphParser}.
     */
    @Test
    public void testParse1() {
        final String path = getPath(VALID_ADAPTER);
        final Map<String, Node> nodes = new ArgoUMLNodeParser().parse(path);

        //number of nodes
        assertEquals(4, nodes.size());
        //get nodes
        Node client = nodes.get("-84-26-0-54--4e0797b8:15aafaeadb5:-8000:0000000000000A67");
        Node target = nodes.get("-84-26-0-54--4e0797b8:15aafaeadb5:-8000:0000000000000A6A");
        Node adapter = nodes.get("-84-26-0-54--4e0797b8:15aafaeadb5:-8000:0000000000000A68");
        Node adaptee = nodes.get("-84-26-0-54--4e0797b8:15aafaeadb5:-8000:0000000000000A69");
        //check node names
        assertEquals("MyClient", client.getName());
        assertEquals("MyTarget", target.getName());
        assertEquals("MyAdapter", adapter.getName());
        assertEquals("MyAdaptee", adaptee.getName());
        //node types
        assertTrue(containsType(target, NodeType.INTERFACE));
        assertTrue(containsType(client, NodeType.CONCRETE_CLASS));
        assertTrue(containsType(adapter, NodeType.CONCRETE_CLASS));
        assertTrue(containsType(adaptee, NodeType.CONCRETE_CLASS));
        //check attribute size
        assertEquals(1, client.getAttributes().size());
        assertEquals(1, adapter.getAttributes().size());
        //check attribute type        
        assertEquals(target, findAttributeByName(client, "adapter").getType());
        assertEquals(adaptee, findAttributeByName(adapter, "adaptee").getType());
        //check attribute visibility
        assertEquals(Visibility.PRIVATE, findAttributeByName(adapter, "adaptee").getVisibility());
        //check method return type
        assertNull(findOperationByName(client, "adapter.request").getReturnType());
        assertNull(findOperationByName(adapter, "request").getReturnType());
        assertNull(findOperationByName(adaptee, "specificRequest").getReturnType());
    }

    /**
     * Test the happy flow of parsing a more complicated XMI input file by the {@link ArgoUMLToSystemGraphParser}.
     */
    @Test
    public void testParse2() {
        final String path = getPath(VALID_ADAPTERS);
        final Map<String, Node> nodes = new ArgoUMLNodeParser().parse(path);

        assertEquals(40, nodes.size());

        //SquarePeg
        Node node = nodes.get("-84-26-0-54--1e9ba376:15aad4320f4:-8000:0000000000000866");
        assertEquals("SquarePeg", node.getName());
        assertTrue(containsType(node, NodeType.CONCRETE_CLASS));
        assertEquals(1, node.getAttributes().size());
        assertNotNull(findAttributeByName(node, "width"));
        assertEquals("Double", findAttributeByName(node, "width").getType().getName());
        assertEquals(2, node.getOperations().size());
        assertNotNull(findOperationByName(node, "getWidth"));
        assertNotNull(findOperationByName(node, "setWidth"));
        assertEquals("Double", findOperationByName(node, "getWidth").getReturnType().getName());
        assertEquals("Double", findParameterByName(node, "w").getType().getName());
        //SquarePegAdapter
        node = nodes.get("-84-26-0-54--1e9ba376:15aad4320f4:-8000:0000000000000878");
        assertEquals("SquarePegAdapter", node.getName());
        assertTrue(containsType(node, NodeType.CONCRETE_CLASS));
        assertEquals(1, node.getAttributes().size());
        assertNotNull(findAttributeByName(node, "sp"));
        assertEquals("SquarePeg", findAttributeByName(node, "sp").getType().getName());
        assertEquals(1, node.getOperations().size());
        assertNotNull(findOperationByName(node, "makeFit"));
        assertNull(findOperationByName(node, "makeFit").getReturnType());
        assertEquals("RoundHole", findParameterByName(node, "rh").getType().getName());
        //RoundHole
        node = nodes.get("-84-26-0-54--1e9ba376:15aad4320f4:-8000:0000000000000872");
        assertEquals("RoundHole", node.getName());
        assertTrue(containsType(node, NodeType.CONCRETE_CLASS));
        assertEquals(1, node.getAttributes().size());
        assertNotNull(findAttributeByName(node, "radius"));
        assertEquals("Integer", findAttributeByName(node, "radius").getType().getName());
        assertEquals(1, node.getOperations().size());
        assertNotNull(findOperationByName(node, "getRadius"));
        assertEquals("Integer", findOperationByName(node, "getRadius").getReturnType().getName());
        assertEquals(0, findOperationByName(node, "getRadius").getParameters().size());
    }

    private String getPath(String resourceName) {
        return this.getClass().getResource(resourceName).getPath();
    }

    private boolean containsType(Node node, NodeType nodetype) {
        return node.getTypes().stream()
                .anyMatch(nt -> nt.equals(nodetype));
    }

    private Attribute findAttributeByName(Node node, String name) {
        return node.getAttributes().stream()
                .filter(attribute -> attribute.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Operation findOperationByName(Node node, String name) {
        return node.getOperations().stream()
                .filter(operation -> operation.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Parameter findParameterByName(Node node, String name) {
        for (Operation op : node.getOperations()) {
            for (Parameter p : op.getParameters()) {
                if (p.getName().equals(name)) {
                    return p;
                }
            }
        }
        return null;
    }

}
