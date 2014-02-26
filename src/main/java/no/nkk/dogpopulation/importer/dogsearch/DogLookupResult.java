package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.GraphDogLookupResult;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogLookupResult {
    private final DogDetails dogsearchResult;
    private final GraphDogLookupResult graphResult;

    DogLookupResult(DogDetails dogsearchResult, GraphDogLookupResult graphResult) {
        this.dogsearchResult = dogsearchResult;
        this.graphResult = graphResult;
    }

    public DogDetails getDogsearchResult() {
        return dogsearchResult;
    }

    public GraphDogLookupResult getGraphResult() {
        return graphResult;
    }
}
