package nl.ou.dpd.domain.rule;

import nl.ou.dpd.domain.node.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link NodeRule} is a {@link Rule} that applies to {@link Node}s specificly.
 *
 * @author Peter Vansweevelt
 * @author Martin de Boer
 * @see EdgeRule
 * @see Rule
 */
public class NodeRule extends Rule<Node> {

    private static final Logger LOGGER = LogManager.getLogger(Rule.class);

    /**
     * Creates a rule with the {@link Node} mould that implements the features needed to apply to this rule.
     *
     * @param mould    the {@link Node} containing the rule's features
     * @param scope    the scope of the evaluation (object, attributes, ...)
     * @param topic    holds the feature to be evaluated (type, visibility, ...)
     * @param operator the evaluation operator (equals, exists, ...)
     */
    public NodeRule(Node mould, Scope scope, Topic topic, Operator operator) {
        super(mould, scope, topic, operator);
    }

    /**
     * Applies the rule on a given {@ink Node}.
     * <p>
     * TODO: Consider returning feedback instead of a boolean
     * <p>
     *
     * @param systemNode the node under consideration
     * @return {@code true} if the {@link Node} meets the conditions set in this {@link NodeRule}, or {@code false}
     * otherwise.
     */
    public boolean process(Node systemNode) {
        switch (getScope()) {
            case OBJECT:
                return processObject(systemNode);
            default:
                return error("Unexpected scope: " + getScope() + ".");
        }
    }

    private boolean processObject(Node systemNode) {
        switch (getTopic()) {
            case TYPE:
                return processObjectType(systemNode);
            case VISIBILITY:
                return processObjectVisibility(systemNode);
            case MODIFIER_ROOT:
                return processObjectModifierRoot(systemNode);
            case MODIFIER_LEAF:
                return processObjectModifierLeaf(systemNode);
            case MODIFIER_ABSTRACT:
                return processObjectModifierAbstract(systemNode);
            case MODIFIER_ACTIVE:
                return processObjectModifierActive(systemNode);
            default:
                return error("Unexpected topic while processing OBJECT: " + getTopic() + ".");
        }
    }

    private boolean processObjectType(Node systemNode) {
        switch (getOperator()) {
            case EXISTS:
                return processObjectTypeExists(systemNode);
            case NOT_EXISTS:
                return !processObjectTypeExists(systemNode);
            case EQUALS:
                return processObjectTypeEquals(systemNode);
            case NOT_EQUALS:
                return !processObjectTypeEquals(systemNode);
            default:
                return error("Unexpected operator while processing OBJECT: " + getOperator() + ".");
        }
    }

    private boolean processObjectVisibility(Node systemNode) {
        switch (getOperator()) {
            case EXISTS:
                return processObjectVisibilityExists(systemNode);
            case NOT_EXISTS:
                return !processObjectVisibilityExists(systemNode);
            case EQUALS:
                return processObjectVisibilityEquals(systemNode);
            case NOT_EQUALS:
                return !processObjectVisibilityEquals(systemNode);
            default:
                return error("Unexpected operator while processing OBJECT: " + getOperator() + ".");
        }
    }

    private boolean processObjectModifierRoot(Node systemNode) {
        switch (getOperator()) {
            case EXISTS:
                return processObjectModifierRootExists(systemNode);
            case NOT_EXISTS:
                return !processObjectModifierRootExists(systemNode);
            case EQUALS:
                return processObjectModifierRootEquals(systemNode);
            case NOT_EQUALS:
                return !processObjectModifierRootEquals(systemNode);
            default:
                return error("Unexpected operator while processing OBJECT: " + getOperator() + ".");
        }
    }

    private boolean processObjectModifierLeaf(Node systemNode) {
        switch (getOperator()) {
            case EXISTS:
                return processObjectModifierLeafExists(systemNode);
            case NOT_EXISTS:
                return !processObjectModifierLeafExists(systemNode);
            case EQUALS:
                return processObjectModifierLeafEquals(systemNode);
            case NOT_EQUALS:
                return !processObjectModifierLeafEquals(systemNode);
            default:
                return error("Unexpected operator while processing OBJECT: " + getOperator() + ".");
        }
    }

    private boolean processObjectModifierAbstract(Node systemNode) {
        switch (getOperator()) {
            case EXISTS:
                return processObjectModifierAbstractExists(systemNode);
            case NOT_EXISTS:
                return !processObjectModifierAbstractExists(systemNode);
            case EQUALS:
                return processObjectModifierAbstractEquals(systemNode);
            case NOT_EQUALS:
                return !processObjectModifierAbstractEquals(systemNode);
            default:
                return error("Unexpected operator while processing OBJECT: " + getOperator() + ".");
        }
    }

    private boolean processObjectModifierActive(Node systemNode) {
        switch (getOperator()) {
            case EXISTS:
                return processObjectModifierActiveExists(systemNode);
            case NOT_EXISTS:
                return !processObjectModifierActiveExists(systemNode);
            case EQUALS:
                return processObjectModifierActiveEquals(systemNode);
            case NOT_EQUALS:
                return !processObjectModifierActiveEquals(systemNode);
            default:
                return error("Unexpected operator while processing OBJECT: " + getOperator() + ".");
        }
    }

    private boolean processObjectTypeExists(Node systemNode) {
        return systemNode.getType() != null;
    }

    private boolean processObjectTypeEquals(Node systemNode) {
        if (getMould().getType() == null) {
            return error("Cannot perform rule on topic TYPE. Unable to detect what to check for.");
        }
        return systemNode.getType() == getMould().getType();
    }

    private boolean processObjectVisibilityExists(Node systemNode) {
        return systemNode.getVisibility() != null;
    }

    private boolean processObjectVisibilityEquals(Node systemNode) {
        if (getMould().getVisibility() == null) {
            return error("Cannot perform rule on topic VISIBILITY. Unable to detect what to check for.");
        }
        return systemNode.getVisibility() == getMould().getVisibility();
    }

    private boolean processObjectModifierRootExists(Node systemNode) {
        return systemNode.isRoot() != null;
    }

    private boolean processObjectModifierRootEquals(Node systemNode) {
        if (getMould().isRoot() == null) {
            return error("Cannot perform rule on topic MODIFIER_ROOT. Unable to detect what to check for.");
        }
        return systemNode.isRoot() == getMould().isRoot();
    }

    private boolean processObjectModifierLeafExists(Node systemNode) {
        return systemNode.isLeaf() != null;
    }

    private boolean processObjectModifierLeafEquals(Node systemNode) {
        if (getMould().isLeaf() == null) {
            return error("Cannot perform rule on topic MODIFIER_LEAF. Unable to detect what to check for.");
        }
        return systemNode.isLeaf() == getMould().isLeaf();
    }

    private boolean processObjectModifierAbstractExists(Node systemNode) {
        return systemNode.isAbstract() != null;
    }

    private boolean processObjectModifierAbstractEquals(Node systemNode) {
        if (getMould().isAbstract() == null) {
            return error("Cannot perform rule on topic MODIFIER_ABSTRACT. Unable to detect what to check for.");
        }
        return systemNode.isAbstract() == getMould().isAbstract();
    }

    private boolean processObjectModifierActiveExists(Node systemNode) {
        return systemNode.isActive() != null;
    }

    private boolean processObjectModifierActiveEquals(Node systemNode) {
        if (getMould().isActive() == null) {
            return error("Cannot perform rule on topic MODIFIER_ACTIVE. Unable to detect what to check for.");
        }
        return systemNode.isActive() == getMould().isActive();
    }

    private boolean error(String message) {
        LOGGER.error(message);
        throw new RuleException(message);
    }

}
