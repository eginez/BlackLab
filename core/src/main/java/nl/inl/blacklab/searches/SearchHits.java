package nl.inl.blacklab.searches;

import java.util.List;

import nl.inl.blacklab.exceptions.RegexpTooLarge;
import nl.inl.blacklab.exceptions.WildcardTermTooBroad;
import nl.inl.blacklab.resultproperty.HitProperty;
import nl.inl.blacklab.resultproperty.PropertyValue;
import nl.inl.blacklab.search.indexmetadata.Annotation;
import nl.inl.blacklab.search.indexmetadata.MatchSensitivity;
import nl.inl.blacklab.search.results.ContextSize;
import nl.inl.blacklab.search.results.Hit;
import nl.inl.blacklab.search.results.Hits;
import nl.inl.blacklab.search.results.QueryInfo;
import nl.inl.blacklab.search.results.SampleParameters;

/** A search that yields hits. */
public abstract class SearchHits extends SearchResults<Hit> {

    SearchHits(QueryInfo queryInfo, List<SearchResultObserver> ops) {
        super(queryInfo, ops);
    }

    /**
     * Execute the search operation, returning the final response.
     * 
     * @return result of the operation
     * @throws RegexpTooLarge if a regular expression was too large
     * @throws WildcardTermTooBroad if a wildcard term or regex matched too many terms
     */
    @Override
    public abstract Hits execute() throws WildcardTermTooBroad, RegexpTooLarge;

    @Override
    public abstract SearchHits observe(SearchResultObserver operation);

    /**
     * Group hits by document.
     * 
     * This is a special case because it takes advantage of the fact that Lucene
     * returns results per document, so we don't have to fetch all hits to produce
     * document results.
     * 
     * @param maxResultsToGatherPerGroup how many results to gather per group
     * @return resulting operation
     */
    public SearchDocs docs(int maxResultsToGatherPerGroup) {
        return new SearchDocsFromHits(queryInfo(), observers, this, maxResultsToGatherPerGroup);
    }

    /**
     * Group hits by a property.
     * 
     * @param groupBy what to group by
     * @param maxResultsToGatherPerGroup how many results to gather per group
     * @return resulting operation
     */
    public SearchHitGroups group(HitProperty groupBy, int maxResultsToGatherPerGroup) {
        return new SearchHitGroupsFromHits(queryInfo(), (List<SearchResultObserver>)null, this, groupBy, maxResultsToGatherPerGroup);
    }

    /**
     * Sort hits.
     * 
     * @param sortBy what to sort by
     * @return resulting operation
     */
    public SearchHits sort(HitProperty sortBy) {
        return new SearchHitsSorted(queryInfo(), (List<SearchResultObserver>)null, this, sortBy);
    }
    
    /**
     * Sample hits.
     * 
     * @param par how many hits to sample; seed
     * @return resulting operation
     */
    public SearchHits sample(SampleParameters par) {
        return new SearchHitsSampled(queryInfo(), (List<SearchResultObserver>)null, this, par);
    }

    /**
     * Get hits with a certain property value.
     * 
     * @param property property to test 
     * @param value value to test for
     * @return resulting operation
     */
    public SearchHits filter(HitProperty property, PropertyValue value) {
        return new SearchHitsFiltered(queryInfo(), (List<SearchResultObserver>)null, this, property, value);
    }

    /**
     * Get window of hits.
     * 
     * @param first first hit to select
     * @param number number of hits to select
     * @return resulting operation
     */
    public SearchHits window(int first, int number) {
        return new SearchHitsWindow(queryInfo(), (List<SearchResultObserver>)null, this, first, number);
    }
    
    /**
     * Count words occurring near these hits.
     * 
     * @param annotation the property to use for the collocations (must have a
     *            forward index)
     * @param size context size to use for determining collocations
     * @param sensitivity sensitivity settings
     * @return resulting operation
     */
    public SearchCollocations collocations(Annotation annotation, ContextSize size, MatchSensitivity sensitivity) {
        return new SearchCollocationsFromHits(queryInfo(), (List<SearchResultObserver>)null, this, annotation, size, sensitivity);
    }

}
