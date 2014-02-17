package no.nkk.dogpopulation.graph;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Inbreeding {
    private final int generations; // typically 3 or 6
    private final BasicStatistics statistics;

    public Inbreeding(int generations, BasicStatistics statistics) {
        this.generations = generations;
        this.statistics = statistics;
    }

    public int getGenerations() {
        return generations;
    }

    public BasicStatistics getStatistics() {
        return statistics;
    }
}
