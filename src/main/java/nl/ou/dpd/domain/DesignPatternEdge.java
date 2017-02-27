package nl.ou.dpd.domain;

/**
 * TODO...
 *
 * @author Martin de Boer
 */
public class DesignPatternEdge extends FourTuple {

    /**
     * TODO...
     *
     * @param cl1
     * @param cl2
     * @param type
     */
    public DesignPatternEdge(String cl1, String cl2, EdgeType type) {
        super(cl1, cl2, type);
    }

    /**
     * This constructor has protected access because it is only available within this package.
     *
     * @param edge a {@link DesignPatternEdge} to construct a copy of.
     */
    protected DesignPatternEdge(DesignPatternEdge edge) {
        super(edge);
    }
}
