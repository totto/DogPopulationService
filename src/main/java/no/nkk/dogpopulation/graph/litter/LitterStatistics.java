package no.nkk.dogpopulation.graph.litter;

import no.nkk.dogpopulation.graph.BasicStatistics;

import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class LitterStatistics {

    private final Set<String> breedSet;
    private final int minYear;
    private final int maxYear;
    private final int dogCount;
    private final int dogsWithAtLeastOneLitter;
    private final BasicStatistics litterSizeStatistics;

    public LitterStatistics(Set<String> breedSet, int minYear, int maxYear, int dogCount, int dogsWithAtLeastOneLitter, BasicStatistics litterSizeStatistics) {
        this.breedSet = breedSet;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.dogCount = dogCount;
        this.dogsWithAtLeastOneLitter = dogsWithAtLeastOneLitter;
        this.litterSizeStatistics = litterSizeStatistics;
    }

    public Set<String> getBreedSet() {
        return breedSet;
    }
    public int getMinYear() {
        return minYear;
    }
    public int getMaxYear() {
        return maxYear;
    }
    public int getDogCount() {
        return dogCount;
    }
    public int getLitterCount() {
        return (int) litterSizeStatistics.getN();
    }
    public int getPuppyCount() {
        return (int) litterSizeStatistics.getSum();
    }
    public int getDogsWithAtLeastOneLitter() {
        return dogsWithAtLeastOneLitter;
    }
    public BasicStatistics getLitterSizeStatistics() {
        return litterSizeStatistics;
    }

}
