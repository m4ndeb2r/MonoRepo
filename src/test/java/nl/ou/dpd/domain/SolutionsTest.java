package nl.ou.dpd.domain;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the class {@link Solution}.
 *
 * @author Martin de Boer
 */
public class SolutionsTest {

    private Solutions solutions1;
    private Solution solution1a;
    private Solution solution1b;

    private Solutions solutions2;
    private Solution solution2a;
    private Solution solution2b;

    /**
     * Initialise the test(s).
     */
    @Before
    public void initSolution() {
        solution1a = new Solution(
                "Pattern1a",
                createMatchedClasses_AB_CD_AC(),
                createSuperfluousEdges(),
                createMissingEdges());
        solution1b = new Solution(
                "Pattern1b",
                createMatchedClasses_AC_CD_AB(),
                createSuperfluousEdges(),
                createMissingEdges());

        solutions1 = new Solutions();
        solutions1.add(solution1a);

        solution2a = new Solution(
                "Pattern2a",
                createMatchedClasses_WX_WY_YZ(),
                createSuperfluousEdges(),
                createMissingEdges());
        solution2b = new Solution(
                "Pattern2b",
                createMatchedClasses_WX_YZ_WY(),
                createSuperfluousEdges(),
                createMissingEdges());

        solutions2 = new Solutions();
        solutions2.add(solution2a);
    }

    /**
     * Tests the {@link Solutions#isUniq(Solution)} method.
     */
    @Test
    public void testIsUnique() {
        assertFalse(solutions1.isUniq(solution1b));
        assertTrue(solutions1.isUniq(solution2a));
        assertTrue(solutions1.isUniq(solution2b));

        assertFalse(solutions2.isUniq(solution2b));
        assertTrue(solutions2.isUniq(solution1a));
        assertTrue(solutions2.isUniq(solution1b));

        solutions1.add(solution2a);
        assertFalse(solutions1.isUniq(solution2a));
        assertFalse(solutions1.isUniq(solution2b));

        solutions2.add(solution1a);
        assertFalse(solutions2.isUniq(solution1a));
        assertFalse(solutions2.isUniq(solution1b));
    }

    /**
     * Tests the {@link Solutions#getSolutionsAsMap()} method.
     */
    @Test
    public void testGetAsMap() {
        assertThat(solutions1.getSolutionsAsMap().keySet().size(), is(1));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1a").size(), is(1));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1a").get(0), is(solution1a));

        solutions1.add(solution1b);
        assertThat(solutions1.getSolutionsAsMap().keySet().size(), is(2));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1a").size(), is(1));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1a").get(0), is(solution1a));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1b").size(), is(1));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1b").get(0), is(solution1b));

        final Solution solution1b_2 = new Solution(
                "Pattern1b",
                createMatchedClasses_WX_YZ_WY(),
                createSuperfluousEdges(),
                createMissingEdges());
        solutions1.add(solution1b_2);
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1b").size(), is(2));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1b").get(0), is(solution1b));
        assertThat(solutions1.getSolutionsAsMap().get("Pattern1b").get(1), is(solution1b_2));
    }

    private Set<Edge> createSuperfluousEdges() {
        Set<Edge> result = new HashSet<>();
        result.add(new Edge(new Clazz("sysE"), new Clazz("sysB"), EdgeType.ASSOCIATION_DIRECTED));
        return result;
    }

    private Set<Edge> createMissingEdges() {
        Set<Edge> result = new HashSet<>();
        result.add(new Edge(new Clazz("dpP"), new Clazz("dpQ"), EdgeType.AGGREGATE));
        return result;
    }

    private MatchedClasses createMatchedClasses_WX_WY_YZ() {
        MatchedClasses result = new MatchedClasses();
        createMatch(result, "W", "X");
        createMatch(result, "W", "Y");
        createMatch(result, "Y", "Z");
        return result;
    }

    private MatchedClasses createMatchedClasses_WX_YZ_WY() {
        MatchedClasses result = new MatchedClasses();
        createMatch(result, "W", "X");
        createMatch(result, "Y", "Z");
        createMatch(result, "W", "Y");
        return result;
    }

    private MatchedClasses createMatchedClasses_AB_CD_AC() {
        MatchedClasses result = new MatchedClasses();
        createMatch(result, "A", "B");
        createMatch(result, "C", "D");
        createMatch(result, "A", "C");
        return result;
    }

    private MatchedClasses createMatchedClasses_AC_CD_AB() {
        MatchedClasses result = new MatchedClasses();
        createMatch(result, "A", "C");
        createMatch(result, "C", "D");
        createMatch(result, "A", "B");
        return result;
    }

    private void createMatch(MatchedClasses matchedClasses, String a, String b) {
        matchedClasses.makeMatch(
                new Edge(new Clazz("sys" + a), new Clazz("sys" + b), EdgeType.AGGREGATE),
                new Edge(new Clazz("dp" + a), new Clazz("dp" + b), EdgeType.DEPENDENCY)
        );
    }

}
