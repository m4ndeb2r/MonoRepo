package nl.ou.dpd.parsing;

import nl.ou.dpd.domain.node.Attribute;
import nl.ou.dpd.domain.node.Node;
import nl.ou.dpd.domain.node.NodeType;
import nl.ou.dpd.domain.node.Operation;
import nl.ou.dpd.domain.node.Parameter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.ATTRIBUTE;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.CLASS;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.DATATYPE;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.ID;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.IDREF;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.INTERFACE;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.IS_ABSTRACT;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.KIND;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.MODEL;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.OPERATION;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.PARAMETER;
import static nl.ou.dpd.parsing.ArgoUMLAbstractParser.XMI_FILE_COULD_NOT_BE_PARSED_MSG;
import static nl.ou.dpd.parsing.ArgoUMLNodeParser.INTEGER;
import static nl.ou.dpd.parsing.ParseTestHelper.createAttributeMock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ArgoUMLNodeParser} class.
 *
 * @author Martin de Boer
 */
@RunWith(MockitoJUnitRunner.class)
public class ArgoUMLNodeParserTest {

    private static final String INTEGER_HREF = String.format(".......%s", INTEGER);
    private static final String OPERATION_ID = "operationId";
    private static final String PARAMETER_ID = "parameterId";
    private static final String ATTRIBUTE_ID = "attributeId";
    private static final String CLASS_NODE_ID = "classNodeId";
    private static final String RETURN_TYPE_ID = "returnTypeId";
    private static final String INPUT_PARAM_TYPE = "in";
    private static final String RETURN_VALUE_TYPE = "return";
    private static final String INTERFACE_NODE_ID = "interfaceNodeId";
    private static final String ABSTRACT_CLASS_NODE_ID = "abstractClassNodeId";

    // This file is created just to satisfy the FileInputStream of the parser
    private String xmiFile = getPath("/argoUML/dummy.xmi");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private XMLInputFactory xmlInputFactory;

    @Mock
    private XMLEventReader xmlEventReader;

    @Mock
    XMLEvent modelEvent, classEvent, abstractClassEvent, interfaceEvent,
            datatypeEvent, attributeEvent, operationEvent, inputParamEvent,
            inputParamTypeEvent, returnParamEvent, returnParamTypeEvent;

    private ArgoUMLNodeParser nodeParser;

    /**
     * Initialises the test subject with a mocked xmlInputStream.
     *
     * @throws XMLStreamException not expected.
     */
    @Before
    public void initNodeParser() throws XMLStreamException {
        nodeParser = new ArgoUMLNodeParser(xmlInputFactory);
        when(xmlInputFactory.createXMLEventReader(any(InputStream.class))).thenReturn(xmlEventReader);
    }

    /**
     * Mocks the order in which the event reader reads the XML elements. Here, we put together a complete, mocked,
     * structure of the system design that is being parsed by the {@link ArgoUMLNodeParser}.
     *
     * @throws XMLStreamException not expected
     */
    @Before
    public void initEventReader() throws XMLStreamException {
        when(xmlEventReader.hasNext()).thenReturn(
                true, // model start-element
                true, // interface start-element
                true, // interface end-element
                true, // class start-element
                true, // attribute start-element
                true, // datatype start-element
                true, // datatype end-element
                true, // attribute end-element
                true, // operation start-element
                true, // input parameter start-element
                true, // input paramType start-element
                true, // input paramType end-element
                true, // input parameter end-element
                true, // return parameter start-element
                true, // return paramType start-element
                true, // return paramType end-element
                true, // return parameter end-element
                true, // operation end-element
                true, // class end-element
                true, // abstract class start-element
                true, // abstract class end-element
                true, // model end-element
                false);
        when(xmlEventReader.nextEvent()).thenReturn(
                modelEvent,
                interfaceEvent, interfaceEvent,
                classEvent,
                attributeEvent, datatypeEvent, datatypeEvent, attributeEvent,
                operationEvent,
                inputParamEvent, inputParamTypeEvent, inputParamTypeEvent, inputParamEvent,
                returnParamEvent, returnParamTypeEvent, returnParamTypeEvent, returnParamEvent,
                operationEvent,
                classEvent,
                abstractClassEvent, abstractClassEvent,
                modelEvent);
    }

    /**
     * Mocks the events that the event reader reads.
     */
    @Before
    public void initEvents() {
        modelEvent = ParseTestHelper.createXMLEventMock(MODEL);

        // Mock an interface, a class and an abstract class
        interfaceEvent = ParseTestHelper.createXMLEventMock(INTERFACE, mockId(INTERFACE_NODE_ID));
        classEvent = ParseTestHelper.createXMLEventMock(CLASS, mockId(CLASS_NODE_ID));
        abstractClassEvent = ParseTestHelper.createXMLEventMock(CLASS, mockId(ABSTRACT_CLASS_NODE_ID), mockIsAbstract(true));

        // Mock an operation that has one input parameter of type "interfaceNode" and one return value of type "interfaceNode"
        operationEvent = ParseTestHelper.createXMLEventMock(OPERATION, mockId(OPERATION_ID));
        inputParamEvent = ParseTestHelper.createXMLEventMock(PARAMETER, mockParamKind(INPUT_PARAM_TYPE), mockId(PARAMETER_ID));
        inputParamTypeEvent = ParseTestHelper.createXMLEventMock(INTERFACE, mockIdRef(INTERFACE_NODE_ID));
        returnParamEvent = ParseTestHelper.createXMLEventMock(PARAMETER, mockParamKind(RETURN_VALUE_TYPE), mockId(RETURN_TYPE_ID));
        returnParamTypeEvent = ParseTestHelper.createXMLEventMock(INTERFACE, mockIdRef(INTERFACE_NODE_ID));

        // Mock an attribute of type "Integer"
        attributeEvent = ParseTestHelper.createXMLEventMock(ATTRIBUTE, mockId(ATTRIBUTE_ID));
        datatypeEvent = ParseTestHelper.createXMLEventMock(DATATYPE, mockHref(INTEGER_HREF));
    }

    private javax.xml.stream.events.Attribute mockHref(String href) {
        return createAttributeMock(ArgoUMLAbstractParser.HREF, href);
    }

    private javax.xml.stream.events.Attribute mockIsAbstract(boolean isAbstract) {
        return createAttributeMock(IS_ABSTRACT, Boolean.toString(isAbstract));
    }

    private javax.xml.stream.events.Attribute mockParamKind(String inOrReturn) {
        return createAttributeMock(KIND, inOrReturn);
    }

    private javax.xml.stream.events.Attribute mockId(String id) {
        return createAttributeMock(ID, id);
    }

    private javax.xml.stream.events.Attribute mockIdRef(String idRef) {
        return createAttributeMock(IDREF, idRef);
    }

    @Test
    public void testParsedNodes() {
        assertThat(nodeParser.events.size(), is(0));

        final Map<String, Node> nodeMap = nodeParser.parse(xmiFile);
        assertThat(nodeMap.keySet().size(), is(4));

        assertClassNode(nodeMap);
        assertAbstractClassNode(nodeMap);
        assertInterface(nodeMap);
        assertDataType(nodeMap);

        assertThat(nodeParser.events.size(), is(0));
    }

    private void assertClassNode(Map<String, Node> nodeMap) {
        assertTrue(nodeMap.containsKey(CLASS_NODE_ID));
        final Node classNode = nodeMap.get(CLASS_NODE_ID);
        assertThat(classNode.getId(), is(CLASS_NODE_ID));
        assertTrue(classNode.getTypes().contains(NodeType.CONCRETE_CLASS));
        assertAttributes(classNode);
        assertOperations(classNode);
    }

    private void assertAttributes(Node node) {
        assertThat(node.getAttributes().size(), is(1));
        final Attribute attribute = node.getAttributes().iterator().next();
        assertThat(attribute.getId(), is(ATTRIBUTE_ID));
        assertThat(attribute.getType().getName(), is(Integer.class.getSimpleName()));
        assertThat(attribute.getParentNode().getId(), is(node.getId()));
    }

    private void assertOperations(Node node) {
        assertThat(node.getOperations().size(), is(1));
        final Operation operation = node.getOperations().iterator().next();
        assertThat(operation.getId(), is(OPERATION_ID));
        assertThat(operation.getParentNode().getId(), is(node.getId()));
        assertParameter(operation);
        assertReturnType(operation);
    }

    private void assertParameter(Operation operation) {
        assertThat(operation.getParameters().size(), is(1));
        final Parameter parameter = operation.getParameters().iterator().next();
        assertThat(parameter.getId(), is(PARAMETER_ID));
        assertThat(parameter.getType().getId(), is(INTERFACE_NODE_ID));
        assertThat(parameter.getParentOperation().getId(), is(operation.getId()));
    }

    private void assertReturnType(Operation operation) {
        assertThat(operation.getReturnType().getId(), is(INTERFACE_NODE_ID));
    }

    private void assertAbstractClassNode(Map<String, Node> nodeMap) {
        assertTrue(nodeMap.containsKey(ABSTRACT_CLASS_NODE_ID));
        final Node abstractClassNode = nodeMap.get(ABSTRACT_CLASS_NODE_ID);
        assertThat(abstractClassNode.getId(), is(ABSTRACT_CLASS_NODE_ID));
        assertNodeHasTypes(abstractClassNode, new NodeType[]{NodeType.ABSTRACT_CLASS, NodeType.ABSTRACT_CLASS_OR_INTERFACE});
        assertThat(abstractClassNode.getOperations().size(), is(0));
        assertThat(abstractClassNode.getAttributes().size(), is(0));
    }

    private void assertInterface(Map<String, Node> nodeMap) {
        assertTrue(nodeMap.containsKey(INTERFACE_NODE_ID));
        final Node interfaceNode = nodeMap.get(INTERFACE_NODE_ID);
        assertThat(interfaceNode.getId(), is(INTERFACE_NODE_ID));
        assertNodeHasTypes(interfaceNode, new NodeType[]{NodeType.INTERFACE, NodeType.ABSTRACT_CLASS_OR_INTERFACE});
        assertThat(interfaceNode.getAttributes().size(), is(0));
        assertThat(interfaceNode.getOperations().size(), is(0));
    }

    private void assertDataType(Map<String, Node> nodeMap) {
        assertTrue(nodeMap.containsKey(INTEGER_HREF));
        final Node datatypeNode = nodeMap.get(INTEGER_HREF);
        assertThat(datatypeNode.getId(), is(INTEGER_HREF));
        assertThat(datatypeNode.getName(), is(Integer.class.getSimpleName()));
        assertNodeHasTypes(datatypeNode, new NodeType[]{NodeType.DATATYPE});
        assertThat(datatypeNode.getAttributes().size(), is(0));
        assertThat(datatypeNode.getOperations().size(), is(0));
    }

    private void assertNodeHasTypes(Node node, NodeType... types) {
        assertThat(node.getTypes().size(), is(types.length));
        Arrays.stream(types).forEach(nodeType -> assertTrue(node.getTypes().contains(nodeType)));
    }

    @Test
    public void testAnyException() {
        when(interfaceEvent.asEndElement()).thenThrow(new IllegalArgumentException());
        thrown.expect(ParseException.class);
        thrown.expectCause(is(IllegalArgumentException.class));
        thrown.expectMessage(String.format(XMI_FILE_COULD_NOT_BE_PARSED_MSG, xmiFile));
        nodeParser.parse(xmiFile);
    }

    @Test
    public void testParseException() {
        final String errorMsg = "Darn!";
        when(interfaceEvent.asEndElement()).thenThrow(new ParseException(errorMsg, null));
        thrown.expect(ParseException.class);
        thrown.expectMessage(errorMsg);
        nodeParser.parse(xmiFile);
    }

    private String getPath(String resourceName) {
        return this.getClass().getResource(resourceName).getPath();
    }

}
