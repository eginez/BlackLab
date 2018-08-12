package nl.inl.blacklab.search.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import nl.inl.blacklab.resultproperty.HitProperty;
import nl.inl.blacklab.search.indexmetadata.Annotation;

/**
 * A basic Hits object implemented with a list.
 */
public class HitsList extends HitsAbstract {

    /** Our window stats, if this is a window; null otherwise. */
    WindowStats windowStats;
    
    private SampleParameters parameters;

    @Override
    public SampleParameters sampleParameters() {
        return parameters;
    }
    
    /**
     * Make a wrapper Hits object for a list of Hit objects.
     *
     * Does not copy the list, but reuses it.
     *
     * @param queryInfo query info
     * @param hits the list of hits to wrap, or null for a new list
     */
    protected HitsList(QueryInfo queryInfo, List<Hit> hits) {
        super(queryInfo);
        this.hits = hits == null ? new ArrayList<>() : hits;
        hitsCounted = this.hits.size();
        int prevDoc = -1;
        for (Hit h : this.hits) {
            if (h.doc() != prevDoc) {
                docsRetrieved++;
                docsCounted++;
                prevDoc = h.doc();
            }
        }
    }
    
    /**
     * Make an empty list of hits.
     *
     * @param queryInfo query info
     */
    protected HitsList(QueryInfo queryInfo) {
        this(queryInfo, null);
    }
    
    /**
     * Construct a hits window from a Hits instance.
     *
     * @param source the larger Hits object we would like a window into
     * @param first the first hit in our window
     * @param windowSize the size of our window
     */
    HitsList(Hits source, int first, int windowSize) {
        super(source.queryInfo());

        // Error if first out of range
        boolean emptyResultSet = !source.hitsProcessedAtLeast(1);
        if (first < 0 || (emptyResultSet && first > 0) ||
                (!emptyResultSet && !source.hitsProcessedAtLeast(first + 1))) {
            throw new IllegalArgumentException("First hit out of range");
        }

        // Auto-clamp number
        int number = windowSize;
        if (!source.hitsProcessedAtLeast(first + number))
            number = source.size() - first;

        // Copy the hits we're interested in.
        hits = new ArrayList<>();
        if (source.hasCapturedGroups())
            capturedGroups = new CapturedGroupsImpl(source.capturedGroups().names());
        int prevDoc = -1;
        hitsCounted = 0;
        for (int i = first; i < first + number; i++) {
            Hit hit = source.get(i);
            hits.add(hit);
            if (capturedGroups != null)
                capturedGroups.put(hit, source.capturedGroups().get(hit));
            // OPT: copy context as well..?
            
            if (hit.doc() != prevDoc) {
                docsRetrieved++;
                docsCounted++;
                prevDoc = hit.doc();
            }
        }
        boolean hasNext = source.hitsProcessedAtLeast(first + windowSize + 1);
        windowStats = new WindowStats(hasNext, first, windowSize, number);
    }
    
    /**
     * Samples hits.
     * 
     * @param hits hits to sample from
     * @param parameters how much to sample, and optionally, a sample seed
     */
    HitsList(Hits hits, SampleParameters parameters) {
        super(hits.queryInfo());
        this.hits = new ArrayList<>();
        this.parameters = parameters;
        Random random = new Random(parameters.seed());
        int numberOfHitsToSelect = parameters.numberOfHits(hits.size());
        if (numberOfHitsToSelect > hits.size())
            numberOfHitsToSelect = hits.size(); // default to all hits in this case
        // Choose the hits
        Set<Integer> chosenHitIndices = new TreeSet<>();
        for (int i = 0; i < numberOfHitsToSelect; i++) {
            // Choose a hit we haven't chosen yet
            int hitIndex;
            do {
                hitIndex = random.nextInt(hits.size());
            } while (chosenHitIndices.contains(hitIndex));
            chosenHitIndices.add(hitIndex);
        }
        
        // Add the hits in order of their index
        int previousDoc = -1;
        for (Integer hitIndex : chosenHitIndices) {
            Hit hit = hits.get(hitIndex);
            if (hit.doc() != previousDoc) {
                docsRetrieved++;
                docsCounted++;
                previousDoc = hit.doc();
            }
            this.hits.add(hit);
            hitsCounted++;
        }
    }
    
    @Override
    public WindowStats windowStats() {
        return windowStats;
    }

    /** Construct a copy of a hits object in sorted order.
     * 
     * @param hitsToSort the hits to sort
     * @param sortProp property to sort on
     * @param reverseSort if true, reverse the sort
     */
    HitsList(HitsAbstract hitsToSort, HitProperty sortProp, boolean reverseSort) {
        super(hitsToSort.queryInfo());
        try {
            hitsToSort.ensureAllHitsRead();
        } catch (InterruptedException e) {
            // (should be detected by the client)
        }
        hits = hitsToSort.hits;
        capturedGroups = hitsToSort.capturedGroups;
        hitsCounted = hitsToSort.hitsCountedSoFar();
        docsRetrieved = hitsToSort.docsProcessedSoFar();
        docsCounted = hitsToSort.docsCountedSoFar();
        sortProp = sortProp.copyWithHits(this); // we need a HitProperty with the correct Hits object
        
        // Make sure we have a sort order array of sufficient size
        int n = hitsToSort.size();
        sortOrder = new Integer[n];
        
        // Fill the array with the original hit order (0, 1, 2, ...)
        for (int i = 0; i < n; i++)
            sortOrder[i] = i;

        // If we need context, make sure we have it.
        List<Annotation> requiredContext = sortProp.needsContext();
        if (requiredContext != null)
            sortProp.setContexts(new Contexts(hitsToSort, requiredContext, sortProp.needsContextSize()));

        // Perform the actual sort.
        Arrays.sort(sortOrder, sortProp);

        if (reverseSort) {
            // Instead of creating a new Comparator that reverses the order of the
            // sort property (which adds an extra layer of indirection to each of the
            // O(n log n) comparisons), just reverse the hits now (which runs
            // in linear time).
            for (int i = 0; i < n / 2; i++) {
                sortOrder[i] = sortOrder[n - i - 1];
            }
        }
    }

    @Override
    public String toString() {
        return "HitsList#" + hitsObjId + " (hits.size()=" + hits.size() + "; isWindow=" + isWindow() + ")";
    }
    
    /**
     * Ensure that we have read at least as many hits as specified in the parameter.
     *
     * @param number the minimum number of hits that will have been read when this
     *            method returns (unless there are fewer hits than this); if
     *            negative, reads all hits
     * @throws InterruptedException if the thread was interrupted during this
     *             operation
     */
    protected void ensureHitsRead(int number) throws InterruptedException {
        // subclasses may override
    }

    @Override
    public boolean doneProcessingAndCounting() {
        return true;
    }

    @Override
    public MaxStats maxStats() {
        return MaxStats.NOT_EXCEEDED;
    }

}