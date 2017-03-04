package nl.ou.dpd.domain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

/**
 * A {@link Matcher} matches a {@link DesignPattern} and a {@link SystemUnderConsideration}, and tries to detect
 * a match. If found, we have detected the occurrence of a design pattern in the "system under consideration".
 *
 * @author Martin de Boer
 */
public class Matcher {

    private static final Logger LOGGER = LogManager.getLogger(Matcher.class);

    private Solutions solutions;

    /**
     * Matches the specified {@link DesignPattern} and {@link SystemUnderConsideration}. This method is called once for
     * the whole detection process.
     *
     * @param pattern         the pattern to detect
     * @param system          the system to search for the {@code pattern}
     * @param maxNotMatchable the maximum number of not matchable edges allowed.
     * @return a {@link Solutions} instance containing feedback information.
     */
    public Solutions match(DesignPattern pattern, SystemUnderConsideration system, int maxNotMatchable) {
        // Order the design pattern's edges
        pattern.order();

        // Put every classname occuring in a 4-tuple in system in MatchedClasses with value = EMPTY.
        MatchedClasses matchedClasses = prepareMatchedClasses(system);

        // Initialise the solutions. These will be populated during the recursive match.
        solutions = new Solutions();

        // Do the matching recursively.
        recursiveMatch(pattern, system, maxNotMatchable, 0, matchedClasses);

        // Return feedback
        return solutions;
    }

    /**
     * TODO: simplify this method. It is to long and incomprehensive.
     *
     * @param pattern
     * @param system
     * @param maxNotMatchable
     * @param startIndex
     * @param matchedClasses
     * @return
     */
    private boolean recursiveMatch(
            DesignPattern pattern,
            SystemUnderConsideration system,
            int maxNotMatchable,
            int startIndex,
            MatchedClasses matchedClasses) {

        // --this-- should contain the edges of the design pattern !!
        // For a particular edge (fourtuple.get(startIndex) in the design pattern
        // try to find the corresponding edge(s) in system under consideration.

        if (startIndex >= pattern.getEdges().size()) {
            // The detecting process is completed

            if (maxNotMatchable >= 0) {
                // This is an acceptable solution. Let's gather the information and keep it.

                final Solution solution = createSolution(pattern, system, matchedClasses);
                if (!solution.isEmpty() && solutions.isUniq(solution)) {
                    solutions.add(solution);
                }
                return true;
            }

            // Should not occur. The search should be stopped before.
            LOGGER.warn("Unexpected situation in DesignPattern#recursiveMatch(). " +
                    "Value of maxNotMatchable = " + maxNotMatchable);
            return false;
        }

        final Edge patternEdge = pattern.getEdges().get(startIndex);
        boolean found = false;

        // For this (startIndex) edge in DP, find matching edges in SE
        for (int j = 0; j < system.getEdges().size(); j++) {

            final Edge systemEdge = system.getEdges().get(j);
            final MatchedClasses copyMatchedClasses = new MatchedClasses(matchedClasses);
            final List<Integer> extraMatched = new ArrayList<Integer>();

            if (!systemEdge.isLocked() && matchedClasses.canMatch(systemEdge, patternEdge)) {

                // Make match in copyMatchedClasses, and lock the matched edges to prevent them from being matched twice
                copyMatchedClasses.makeMatch(systemEdge, patternEdge);
                lockEdges(patternEdge, systemEdge);

                if (patternEdge.getTypeRelation() == EdgeType.INHERITANCE_MULTI) {
                    // There may be more edges of se that contain an inheritance to the same parent and
                    // have unmatched children
                    for (int k = j + 1; k < system.getEdges().size(); k++) {
                        final Edge skEdge = system.getEdges().get(k);
                        if (!skEdge.isLocked()
                                && skEdge.getClass2().equals(systemEdge.getClass2())
                                && matchedClasses.canMatch(skEdge, patternEdge)) {
                            copyMatchedClasses.makeMatch(skEdge, patternEdge);
                            extraMatched.add(new Integer(k));
                            skEdge.lock();
                        }
                    }
                }

                // Recursive matching
                boolean foundRecursively = recursiveMatch(pattern, system, maxNotMatchable,
                        startIndex + 1, copyMatchedClasses);
                found = found || foundRecursively;

                // Unlock all locked edges, including the multiple inherited ones
                unlockEdges(patternEdge, systemEdge);
                for (Integer getal : extraMatched) {
                    system.getEdges().get(getal.intValue()).unlock();
                }

                if (found && extraMatched.size() > 0) {
                    // In case of inheritance with multiple children, all matching edges has been found.
                    // Therefore searching for more edges may be stopped.
                    j = system.getEdges().size();
                }
            }
        }

        if (!found) {

            // Is the number of not matched edges acceptable? Can we proceed recursively?
            if (--maxNotMatchable >= 0) {
                return recursiveMatch(pattern, system, maxNotMatchable,
                        startIndex + 1, matchedClasses);
            }

            return false;
        }

        return found; // == true !!
    }


    /**
     * TODO...
     *
     * @param pattern
     * @param system
     * @param matchedClasses
     * @return
     */
    private Solution createSolution(DesignPattern pattern, SystemUnderConsideration system, MatchedClasses matchedClasses) {
        final String dpName = pattern.getName();
        final SortedSet<Clazz> boundedKeys = matchedClasses.getBoundedSortedKeySet();
        final MatchedClasses involvedClasses = matchedClasses.filter(boundedKeys);
        final List<Edge> superfluousEdges = new ArrayList<>();
        for (Edge edge : system.getEdges()) {
            if (matchedClasses.keyIsBounded(edge.getClass1())
                    && matchedClasses.keyIsBounded(edge.getClass2())
                    && !edge.isLocked()) {
                superfluousEdges.add(edge);
            }
        }
        return new Solution(dpName, involvedClasses, superfluousEdges);
    }

    /**
     * Prepares a {@link MatchedClasses} for the matching process, based on the "system under consideration".
     *
     * @param system the "system under consideration"
     * @return the prepared {@link MatchedClasses} instance.
     */
    private MatchedClasses prepareMatchedClasses(SystemUnderConsideration system) {
        MatchedClasses matchedClasses = new MatchedClasses();
        system.getEdges().forEach(edge -> matchedClasses.prepareMatch(edge));
        return matchedClasses;
    }

    /**
     * Locks a number of {@link Edge}s.
     *
     * @param edges the {@link Edge}s to lock.
     */
    private void lockEdges(Edge... edges) {
        Arrays.asList(edges).forEach(edge -> edge.lock());
    }

    /**
     * Unlocks a number of {@link Edge}s.
     *
     * @param edges the {@link Edge}s to unlock.
     */
    private void unlockEdges(Edge... edges) {
        Arrays.asList(edges).forEach(edge -> edge.unlock());
    }
}
