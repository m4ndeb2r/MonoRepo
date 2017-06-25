package nl.ou.dpd.domain.node;

import org.junit.Test;

import java.util.HashSet;

import java.util.Set;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;

/**
 * Tests the {@link Node} class.
 *
 * @author Martin de Boer
 */
public class NodeTest {
	
	/**
	 * Test the constructors.
	 */
	@Test
	public void testConstructors() {
		Node node1 = new Node("001");
		assertEquals("001", node1.getId());
		assertNull(node1.getName());
		assertEquals(0, node1.getTypes().size());
		assertEquals(0, node1.getAttributes().size());
		assertEquals(0, node1.getOperations().size());
		assertEquals(Visibility.PUBLIC, node1.getVisibility());
		
		Node node2 = new Node("002", "name2");
		assertEquals("002", node2.getId());
		assertEquals("name2", node2.getName());
		assertEquals(0, node2.getTypes().size());
		assertEquals(0, node1.getAttributes().size());
		assertEquals(0, node1.getOperations().size());
		assertEquals(Visibility.PUBLIC, node2.getVisibility());
		
		Node node3 = new Node("003", "name3", NodeType.CONCRETE_CLASS);
		assertEquals(1, node3.getTypes().size());
		assertTrue(node3.getTypes().contains(NodeType.CONCRETE_CLASS));
		assertFalse(node3.getTypes().contains(NodeType.INTERFACE));
		node3.addType(NodeType.INTERFACE);
		assertEquals(2, node3.getTypes().size());
		assertTrue(node3.getTypes().contains(NodeType.CONCRETE_CLASS));
		assertTrue(node3.getTypes().contains(NodeType.INTERFACE));
	}
	
	/**
	 * test adding attributes
	 */
	@Test
	public void testAttributes() {
		Node node1 = new Node("001");
		Node node4 = new Node("004", "name4", null);
		Attribute attr1 = new Attribute("attr1", node4);
		Attribute attr2 = new Attribute("attr1", node1);
		assertEquals(1, node4.getAttributes().size());
		assertTrue(node4.getAttributes().contains(attr1));
		Attribute attr3 = new Attribute("attr3", node4);
		assertEquals(2, node4.getAttributes().size());
		assertTrue(node4.getAttributes().contains(attr1));
		assertTrue(node4.getAttributes().contains(attr3));		
	}

	/**
	 * test equals signature
	 */
	@Test
	public void testEqualsSignature() {
		Node node1 = new Node("001", "name", null);
		Node node2 = new Node("002", "name", null);
		Node node3 = new Node("003", "name", null);
		assertTrue(node1.equalsSignature(node1));
		assertFalse(node1.equalsSignature(null));		
		assertTrue(node1.equalsSignature(node2));
		node3.setName(null);
		assertFalse(node1.equalsSignature(node3));
		assertFalse(node3.equalsSignature(node1));
		node3.setName("anothername");
		assertFalse(node1.equalsSignature(node3));
		node3.setName("name");
		assertTrue(node1.equalsSignature(node3));
		node3.addType(NodeType.CONCRETE_CLASS);
		assertFalse(node1.equalsSignature(node3));
		
		Attribute attr1 = new Attribute("attr1", node1);
		attr1.setName("attribute");
		Attribute attr2 = new Attribute("attr2", node2);
		attr2.setName("attribute");
		assertTrue(attr1.equalsSignature(attr2));
		assertTrue(node1.equalsSignature(node2));

		Node node4 = new Node("004", "name", null);
		assertFalse(node1.equalsSignature(node4));
		assertFalse(node4.equalsSignature(node1));
		Attribute attr3 = new Attribute("attr3", node4);
		attr3.setName("attribute");
		assertTrue(node4.equalsSignature(node1));

		Node node5 = new Node("005", "name", null);
		Attribute attr4 = new Attribute("attr4", node5);
		attr4.setName("anotherAttribute");
		assertFalse(node1.equalsSignature(node5));
		attr4.setName("attribute");
		attr4.setVisibility(Visibility.PUBLIC);
		assertTrue(node1.equalsSignature(node5));
		attr4.setType(node4);
		assertFalse(node1.equalsSignature(node5));
		
		node1.getAttributes().clear();
		node2.getAttributes().clear();
		Operation op1 = new Operation("op1", node1);
		op1.setName("operation");
		Operation op2 = new Operation("op2", node2);
		op2.setName("operation");
		assertTrue(node1.equalsSignature(node2));

		Node node50 = new Node("005", "name", null);
		Operation op3 = new Operation("op3", node50);
		op3.setName("anotherOperation");
		assertFalse(node1.equalsSignature(node50));
		op3.setName("operation");
		op3.setVisibility(Visibility.PUBLIC);
		assertTrue(node1.equalsSignature(node50));

		Node node40 = new Node("004", "name", null);
		assertFalse(node1.equalsSignature(node40));
		assertFalse(node40.equalsSignature(node1));
		
		op3.setReturnType(node40);
		assertFalse(node1.equalsSignature(node50));
		node1.addType(NodeType.INTERFACE);
		node2.addType(NodeType.INTERFACE);
		assertTrue(node1.equalsSignature(node2));
		Node node6 = new Node("006", "name", null);
		assertFalse(node1.equalsSignature(node6));
		assertFalse(node6.equalsSignature(node1));
		node6.addType(NodeType.CONCRETE_CLASS);
		assertFalse(node1.equalsSignature(node6));
		
		node1.getOperations().clear();
		node2.getOperations().clear();
		
		node1.setVisibility(Visibility.PUBLIC);;
		node2.setVisibility(Visibility.PUBLIC);;
		assertTrue(node1.equalsSignature(node2));
		node2.setVisibility(Visibility.PROTECTED);
		assertFalse(node1.equalsSignature(node2));

		node2.setVisibility(Visibility.PUBLIC);;
		assertTrue(node1.equalsSignature(node2));
		
	}
	/**
	 * test adding operations
	 */
	@Test
	public void testOperations() {
		Node node1 = new Node("001");
		Operation op1 = new Operation("op1", node1);
		assertEquals(1, node1.getOperations().size());
		assertTrue(node1.getOperations().contains(op1));

		Node node2 = new Node("002", "name2", null);
		Operation op2 = new Operation("op1", node2);
		assertEquals(1, node2.getOperations().size());
		assertTrue(node2.getOperations().contains(op2));
		Operation op3 = new Operation("op3", node2);
		assertEquals(2, node2.getOperations().size());
		assertTrue(node2.getOperations().contains(op2));
		assertTrue(node2.getOperations().contains(op3));
		
	}

}
