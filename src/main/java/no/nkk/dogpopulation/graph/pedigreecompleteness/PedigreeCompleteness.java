package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.graph.BasicStatistics;

/**
 * Describes the pedigree completeness for a a group of dogs.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeCompleteness {
    private final int generations; // typically 3 or 6
    private final BasicStatistics statistics;

    public PedigreeCompleteness(int generations, BasicStatistics statistics) {
        this.generations = generations;
        this.statistics = statistics;
    }

    public int getGenerations() {
        return generations;
    }

    public int getCompletePedigreeSize() {
        return ((int) Math.pow(2, generations + 1)) - 2;
    }

    public BasicStatistics getStatistics() {
        return statistics;
    }
}
