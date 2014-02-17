package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.graph.BasicStatistics;

import java.util.Set;

/**
 * Describes the pedigree completeness for a a group of dogs.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeCompleteness {
    private final int generations; // typically 3 or 6
    private final Set<String> breed;
    private final int minYear;
    private final int maxYear;
    private final BasicStatistics pedigreeSizeStatistics;
    private final BasicStatistics pedigreeCompletenessStatistics;

    public PedigreeCompleteness(int generations, Set<String> breed, int minYear, int maxYear, BasicStatistics pedigreeSizeStatistics, BasicStatistics pedigreeCompletenessStatistics) {
        this.generations = generations;
        this.pedigreeSizeStatistics = pedigreeSizeStatistics;
        this.pedigreeCompletenessStatistics = pedigreeCompletenessStatistics;
        this.breed = breed;
        this.minYear = minYear;
        this.maxYear =  maxYear;
    }

    public int getGenerations() {
        return generations;
    }

    public int getCompletePedigreeSize() {
        return getCompletePedigreeSize(generations);
    }

    public static int getCompletePedigreeSize(int generations) {
        return ((int) Math.pow(2, generations + 1)) - 2;
    }

    public BasicStatistics getPedigreeSizeStatistics() {
        return pedigreeSizeStatistics;
    }

    public BasicStatistics getPedigreeCompletenessStatistics() {
        return pedigreeCompletenessStatistics;
    }

    public Set<String> getBreed() {
        return breed;
    }

    public int getMinYear() {
        return minYear;
    }

    public int getMaxYear() {
        return maxYear;
    }
}
