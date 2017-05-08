package nl.ou.dpd.domain.rule;

import nl.ou.dpd.domain.node.Attribute;
import nl.ou.dpd.domain.node.Clazz;
import nl.ou.dpd.domain.node.Interface;
import nl.ou.dpd.domain.node.Node;
import nl.ou.dpd.domain.node.Visibility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link NodeRule} class.
 *
 * @author Martin de Boer
 */
public class NodeRuleTest {

    /**
     * Exception rule.
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructorMissingMould() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("All arguments are mandatory.");
        new NodeRule(null, Scope.OBJECT, Topic.TYPE, Operation.EQUALS);
    }

    @Test
    public void testConstructorMissingScope() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("All arguments are mandatory.");
        new NodeRule(new Interface("", ""), null, Topic.TYPE, Operation.EQUALS);
    }

    @Test
    public void testConstructorMissingTopic() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("All arguments are mandatory.");
        new NodeRule(new Interface("", ""), Scope.ATTRIBUTE, null, Operation.EXISTS);
    }

    @Test
    public void testConstructorMissingOperation() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("All arguments are mandatory.");
        new NodeRule(new Interface("", ""), Scope.OBJECT, Topic.TYPE, null);
    }

    @Test
    public void testNodeRuleWithUnknownScope() {
        final Node mould = new Clazz("id1", "ClassName1");
        final NodeRule failingScopeRule = new NodeRule(mould, Scope.RELATION, Topic.TYPE, Operation.EQUALS);

        thrown.expect(RuleException.class);
        thrown.expectMessage("Unexpected scope: 'RELATION'");
        failingScopeRule.process(new Clazz("id2", "ClassName2"));
    }

    @Test
    public void testNodeRuleWithUnknownTopic() {
        final Node mould = new Clazz("id1", "ClassName");
        final NodeRule failingScopeRule = new NodeRule(mould, Scope.OBJECT, Topic.CARDINALITY, Operation.EQUALS);

        thrown.expect(RuleException.class);
        thrown.expectMessage("Unexpected topic 'CARDINALITY' while processing scope 'OBJECT'.");
        failingScopeRule.process(new Clazz("id2", "ClassName"));
    }

    /**
     * Test the existence of the scopes in the systemNode (whatever the mould demands).
     */
    @Test
    public void testNodeForTopicExistence() {
        final List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("Class1", new Clazz("id0a", "Class")));
        attributes.add(new Attribute("Interface1", new Clazz("id0b", "Interface")));

        final Node nothingSetNode = new Clazz("id1a", "ClassName");
        final Node everythingSetNode = new Clazz("id1b", "ClassName", Visibility.PRIVATE, attributes, true, false, false, true);

        // Type is always set by Constructor
        final NodeRule typeNotSetRule = new NodeRule(nothingSetNode, Scope.OBJECT, Topic.TYPE, Operation.EXISTS);
        final NodeRule typeSetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.TYPE, Operation.EXISTS);
        assertTrue(typeNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertTrue(typeNotSetRule.process(new Clazz("id3", "ClassName", null, null, false, false, true, false)));
        assertTrue(typeSetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, false, false, true, false)));
        assertTrue(typeSetRule.process(new Clazz("id5", "ClassName", null, null, false, false, true, false)));
        assertTrue(typeNotSetRule.process(new Interface("id6", "ClassName")));

        final NodeRule notTypeNotSetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.TYPE, Operation.NOT_EXISTS);
        assertFalse(notTypeNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertFalse(notTypeNotSetRule.process(new Clazz("id3", "ClassName", null, null, false, false, true, false)));
        assertFalse(notTypeNotSetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, false, false, true, false)));
        assertFalse(notTypeNotSetRule.process(new Clazz("id5", "ClassName", null, null, false, false, true, false)));
        assertFalse(notTypeNotSetRule.process(new Interface("id6", "ClassName")));

        //visibility
        final NodeRule visibilityNotSetRule = new NodeRule(nothingSetNode, Scope.OBJECT, Topic.VISIBILITY, Operation.EXISTS);
        final NodeRule visibilitySetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.VISIBILITY, Operation.EXISTS);
        assertTrue(visibilityNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertFalse(visibilityNotSetRule.process(new Clazz("id3", "ClassName", null, null, null, null, null, null)));
        assertTrue(visibilitySetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, true, true, false, true)));
        assertFalse(visibilitySetRule.process(new Clazz("id5", "ClassName", null, null, null, null, null, null)));
        //visibility is always set (public) in an interface 
        assertTrue(visibilityNotSetRule.process(new Interface("id6", "ClassName", true, false, false)));
        assertTrue(visibilityNotSetRule.process(new Interface("id6", "ClassName")));

        //rootModifier
        final NodeRule rootModifierNotSetRule = new NodeRule(nothingSetNode, Scope.OBJECT, Topic.MODIFIER_ROOT, Operation.EXISTS);
        final NodeRule rootModifierSetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.MODIFIER_ROOT, Operation.EXISTS);
        assertTrue(rootModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertFalse(rootModifierNotSetRule.process(new Clazz("id3", "ClassName")));
        assertTrue(rootModifierSetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, true, true, false, true)));
        assertFalse(rootModifierSetRule.process(new Clazz("id5", "ClassName")));

        assertTrue(rootModifierNotSetRule.process(new Interface("id6", "ClassName", true, false, false)));
        assertFalse(rootModifierNotSetRule.process(new Interface("id6", "ClassName")));

        //leafModifier
        final NodeRule leafModifierNotSetRule = new NodeRule(nothingSetNode, Scope.OBJECT, Topic.MODIFIER_LEAF, Operation.EXISTS);
        final NodeRule leafModifierSetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.MODIFIER_LEAF, Operation.EXISTS);
        assertTrue(leafModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertFalse(leafModifierNotSetRule.process(new Clazz("id3", "ClassName")));
        assertTrue(leafModifierSetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, true, true, false, true)));
        assertFalse(leafModifierSetRule.process(new Clazz("id5", "ClassName")));

        assertTrue(leafModifierNotSetRule.process(new Interface("id6", "ClassName", true, false, false)));
        assertFalse(leafModifierNotSetRule.process(new Interface("id6", "ClassName")));

        //abstractModifier
        final NodeRule abstractModifierNotSetRule = new NodeRule(nothingSetNode, Scope.OBJECT, Topic.MODIFIER_ABSTRACT, Operation.EXISTS);
        final NodeRule abstractModifierSetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.MODIFIER_ABSTRACT, Operation.EXISTS);
        assertTrue(abstractModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertFalse(abstractModifierNotSetRule.process(new Clazz("id3", "ClassName")));
        assertTrue(abstractModifierSetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, true, true, false, true)));
        assertFalse(abstractModifierSetRule.process(new Clazz("id5", "ClassName")));
        //abstractModifier is always set true in an interface 
        assertTrue(abstractModifierNotSetRule.process(new Interface("id6", "ClassName", true, false, false)));
        assertTrue(abstractModifierNotSetRule.process(new Interface("id6", "ClassName")));

        //activeModifier
        final NodeRule activeModifierNotSetRule = new NodeRule(nothingSetNode, Scope.OBJECT, Topic.MODIFIER_ACTIVE, Operation.EXISTS);
        final NodeRule activeModifierSetRule = new NodeRule(everythingSetNode, Scope.OBJECT, Topic.MODIFIER_ACTIVE, Operation.EXISTS);
        assertTrue(activeModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertFalse(activeModifierNotSetRule.process(new Clazz("id3", "ClassName")));
        assertTrue(activeModifierSetRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, true, true, false, true)));
        assertFalse(activeModifierSetRule.process(new Clazz("id5", "ClassName")));

        assertTrue(activeModifierNotSetRule.process(new Interface("id6", "ClassName", true, false, false)));
        assertFalse(activeModifierNotSetRule.process(new Interface("id6", "ClassName")));
    }

    /**
     * Test the {@link NodeRule} class for {@link Topic#TYPE} checking.
     */
    @Test
    public void testNodeForType() {
        // Test with a Clazz
        final Node clazz = new Clazz("id1", "ClassName");
        final NodeRule clazzRule = new NodeRule(clazz, Scope.OBJECT, Topic.TYPE, Operation.EQUALS);
        assertTrue(clazzRule.process(new Clazz("id2", "ClassName")));
        assertFalse(clazzRule.process(new Interface("id3", "ClassName")));

        // Test with an Interface
        final Node inferfaze = new Interface("id1", "InterfaceName");
        final NodeRule interfazeRule = new NodeRule(inferfaze, Scope.OBJECT, Topic.TYPE, Operation.EQUALS);
        assertFalse(interfazeRule.process(new Clazz("id2", "ClassName")));
        assertTrue(interfazeRule.process(new Interface("id3", "InterfaceName")));
    }

    /**
     * Test the {@link NodeRule} class for {@link Topic#VISIBILITY} checking.
     */
    @Test
    public void testNodeForVisibility() {
        // Test the visibility modifier
        final Node publicNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, null, null, null, null);
        final NodeRule visibilityRule = new NodeRule(publicNode, Scope.OBJECT, Topic.VISIBILITY, Operation.EQUALS);
        assertTrue(visibilityRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false)));
        assertTrue(visibilityRule.process(new Interface("id3", "ClassName")));
        assertFalse(visibilityRule.process(new Clazz("id4", "ClassName", Visibility.PROTECTED, null, false, false, true, false)));
        assertFalse(visibilityRule.process(new Clazz("id5", "ClassName", Visibility.PRIVATE, null, false, false, true, false)));
        assertFalse(visibilityRule.process(new Clazz("id6", "ClassName", Visibility.PACKAGE, null, false, false, true, false)));

        // This situation is a bit far-fetched. It can only happen in case of a programming error.
        final Node visibilityNotSetNode = new Clazz("id1", "ClassName");
        final NodeRule visibilityNotSetRule = new NodeRule(visibilityNotSetNode, Scope.OBJECT, Topic.VISIBILITY, Operation.EQUALS);
        thrown.expect(RuleException.class);
        thrown.expect(RuleException.class);
        thrown.expectMessage("Cannot perform rule on topic 'VISIBILITY'. Unable to detect what to check for.");
        visibilityNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PUBLIC, null, false, false, true, false));
    }

    /**
     * Test the {@link NodeRule} class for {@link Topic#MODIFIER_ROOT} checking.
     */
    @Test
    public void testNodeForRootModifier() {
        // Test the root modifier
        final Node rootNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, true, false, false, false);
        final NodeRule rootRule = new NodeRule(rootNode, Scope.OBJECT, Topic.MODIFIER_ROOT, Operation.EQUALS);
        assertTrue(rootRule.process(new Clazz("id2", "ClassName", Visibility.PRIVATE, null, true, false, false, false)));
        assertTrue(rootRule.process(new Clazz("id3", "ClassName", Visibility.PRIVATE, null, true, true, false, false)));
        assertTrue(rootRule.process(new Clazz("id4", "ClassName", Visibility.PRIVATE, null, true, false, true, false)));
        assertTrue(rootRule.process(new Clazz("id5", "ClassName", Visibility.PRIVATE, null, true, false, false, true)));
        assertFalse(rootRule.process(new Clazz("id6", "ClassName", Visibility.PRIVATE, null, false, false, false, false)));

        // This situation is a bit far-fetched. It can only happen in case of a programming error.
        final Node rootModifierNotSetNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, null, false, false, false);
        final NodeRule rootModifierNotSetRule = new NodeRule(rootModifierNotSetNode, Scope.OBJECT, Topic.MODIFIER_ROOT, Operation.EQUALS);
        thrown.expect(RuleException.class);
        thrown.expect(RuleException.class);
        thrown.expectMessage("Cannot perform rule on topic 'MODIFIER_ROOT'. Unable to detect what to check for.");
        rootModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PRIVATE, null, true, false, false, false));
    }

    /**
     * Test the {@link NodeRule} class for {@link Topic#MODIFIER_LEAF} checking.
     */
    @Test
    public void testNodeForLeafModifier() {
        // Test the leaf modifier
        final Node leafNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, false, true, false, false);
        final NodeRule leafRule = new NodeRule(leafNode, Scope.OBJECT, Topic.MODIFIER_LEAF, Operation.EQUALS);
        assertTrue(leafRule.process(new Clazz("id2", "ClassName", Visibility.PRIVATE, null, false, true, false, false)));
        assertTrue(leafRule.process(new Clazz("id3", "ClassName", Visibility.PRIVATE, null, true, true, false, false)));
        assertTrue(leafRule.process(new Clazz("id4", "ClassName", Visibility.PRIVATE, null, false, true, true, false)));
        assertTrue(leafRule.process(new Clazz("id5", "ClassName", Visibility.PRIVATE, null, false, true, false, true)));
        assertFalse(leafRule.process(new Clazz("id6", "ClassName", Visibility.PRIVATE, null, false, false, false, false)));

        // This situation is a bit far-fetched. It can only happen in case of a programming error.
        final Node leafModifierNotSetNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, true, null, false, false);
        final NodeRule leafModifierNotSetRule = new NodeRule(leafModifierNotSetNode, Scope.OBJECT, Topic.MODIFIER_LEAF, Operation.EQUALS);
        thrown.expect(RuleException.class);
        thrown.expect(RuleException.class);
        thrown.expectMessage("Cannot perform rule on topic 'MODIFIER_LEAF'. Unable to detect what to check for.");
        leafModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PRIVATE, null, false, true, false, false));
    }

    /**
     * Test the {@link NodeRule} class for {@link Topic#MODIFIER_ABSTRACT} checking.
     */
    @Test
    public void testNodeForAbstractModifier() {
        // Test the abstract modifier
        final Node abstractNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, false, false, true, false);
        final NodeRule abstractRule = new NodeRule(abstractNode, Scope.OBJECT, Topic.MODIFIER_ABSTRACT, Operation.EQUALS);
        assertTrue(abstractRule.process(new Interface("id2", "ClassName", false, false, false)));
        assertTrue(abstractRule.process(new Clazz("id3", "ClassName", Visibility.PRIVATE, null, false, false, true, false)));
        assertTrue(abstractRule.process(new Clazz("id4", "ClassName", Visibility.PRIVATE, null, true, false, true, false)));
        assertTrue(abstractRule.process(new Clazz("id5", "ClassName", Visibility.PRIVATE, null, false, true, true, false)));
        assertTrue(abstractRule.process(new Clazz("id6", "ClassName", Visibility.PRIVATE, null, false, false, true, true)));
        assertFalse(abstractRule.process(new Clazz("id7", "ClassName", Visibility.PRIVATE, null, false, false, false, false)));

        // This situation is a bit far-fetched. It can only happen in case of a programming error.
        final Node abstractModifierNotSetNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, true, false, null, false);
        final NodeRule abstractModifierNotSetRule = new NodeRule(abstractModifierNotSetNode, Scope.OBJECT, Topic.MODIFIER_ABSTRACT, Operation.EQUALS);
        thrown.expect(RuleException.class);
        thrown.expect(RuleException.class);
        thrown.expectMessage("Cannot perform rule on topic 'MODIFIER_ABSTRACT'. Unable to detect what to check for.");
        abstractModifierNotSetRule.process(new Interface("id2", "ClassName", false, false, false));
    }

    /**
     * Test the {@link NodeRule} class for {@link Topic#MODIFIER_ACTIVE} checking.
     */
    @Test
    public void testNodeForActiveModifier() {
        // Test active modifier
        final Node activeNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, false, false, false, true);
        final NodeRule activeRule = new NodeRule(activeNode, Scope.OBJECT, Topic.MODIFIER_ACTIVE, Operation.EQUALS);
        assertTrue(activeRule.process(new Clazz("id2", "ClassName", Visibility.PRIVATE, null, false, false, false, true)));
        assertTrue(activeRule.process(new Clazz("id3", "ClassName", Visibility.PRIVATE, null, true, false, false, true)));
        assertTrue(activeRule.process(new Clazz("id4", "ClassName", Visibility.PRIVATE, null, false, true, false, true)));
        assertTrue(activeRule.process(new Clazz("id5", "ClassName", Visibility.PRIVATE, null, false, false, true, true)));
        assertFalse(activeRule.process(new Clazz("id6", "ClassName", Visibility.PRIVATE, null, false, false, false, false)));

        // This first situation is a bit far-fetched. It can only happen in case of a programming error.
        final Node activeModifierNotSetNode = new Clazz("id1", "ClassName", Visibility.PUBLIC, null, null, false, false, null);
        final NodeRule activeModifierNotSetRule = new NodeRule(activeModifierNotSetNode, Scope.OBJECT, Topic.MODIFIER_ACTIVE, Operation.EQUALS);
        thrown.expect(RuleException.class);
        thrown.expect(RuleException.class);
        thrown.expectMessage("Cannot perform rule on topic 'MODIFIER_ACTIVE'. Unable to detect what to check for.");
        activeModifierNotSetRule.process(new Clazz("id2", "ClassName", Visibility.PRIVATE, null, false, false, false, true));
    }

    /**
     * Tests the {@link NodeRule#process(Node)} for attributes.
     */
    @Test
    public void testNodeForAttributes() {
        final NodeRule rule = new NodeRule(Clazz.EMPTY_NODE, Scope.ATTRIBUTE, Topic.TYPE, Operation.EQUALS);
        // TODO: attributes checking is not yet implemented and always throws an exception.
        thrown.expect(RuleException.class);
        thrown.expect(RuleException.class);
        thrown.expectMessage("Unexpected scope: 'ATTRIBUTE'.");
        rule.process(Clazz.EMPTY_NODE);
    }
}
