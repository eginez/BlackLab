package nl.inl.blacklab.search.results;

import java.util.Collections;
import java.util.Map;

public abstract class ResultsStats implements SearchResult {

    public static final ResultsStats SEARCH_NOT_STARTED_YET = new ResultsStats() {
        @Override
        public boolean processedAtLeast(int lowerBound) {
            return false;
        }

        @Override
        public int processedTotal() {
            throw new RuntimeException("cannot access total, search not started");
        }

        @Override
        public int processedSoFar() {
            return 0;
        }

        @Override
        public int countedSoFar() {
            return 0;
        }

        @Override
        public int countedTotal() {
            throw new RuntimeException("cannot access total, search not started");
        }

        @Override
        public boolean done() {
            return false;
        }

        @Override
        public MaxStats maxStats() {
            return MaxStats.NOT_EXCEEDED;
        }

        @Override
        public String toString() {
            return "ResultsStats.SEARCH_NOT_STARTED_YET";
        }
    };

    public abstract boolean processedAtLeast(int lowerBound);

    /**
     * This is an alias of resultsProcessedTotal().
     *
     * @return number of hits processed total
     */
    public int size() {
        return processedTotal();
    }

    public abstract int processedTotal();

    public abstract int processedSoFar();

    public abstract int countedSoFar();

    public abstract int countedTotal();

    public abstract boolean done();

    /**
     * Save the current counts to a static object.
     *
     * The resulting object doesn't hold a reference to the search anymore.
     *
     * It only saves the results processed and counted so far, and considers those
     * the totals.
     *
     * @return static instance of current stats
     */
    public ResultsStats save() {
        return new ResultsStatsStatic(processedSoFar(), countedSoFar(), maxStats());
    }

    /**
     * Is this a static count?
     *
     * @return true if this is a static (saved) count, false if it is dynamically linked to a search
     */
    public boolean isStatic() {
        return false;
    }

    /**
     * Get information about exceeding maximums.
     *
     * @return max stats
     */
    public abstract MaxStats maxStats();

    /**
     * Was this count interrupted?
     *
     * This can happen if you implement a system that aborts long-running or memory-hungry searches.
     * If so, the total counts may not reflect reality.
     *
     * @return true if the count was interrupted, false if not
     */
    public boolean wasInterrupted() {
        return false;
    }

    @Override
    public abstract String toString();

    /**
     * How many result objects are stored here?
     * @return
     */
    @Override
    public int numberOfResultObjects() {
        return 1;
    }

    /**
     * Return debug info.
     */
    @Override
    public Map<String, Object> getDebugInfo() {
        return Collections.emptyMap();
    }
}
