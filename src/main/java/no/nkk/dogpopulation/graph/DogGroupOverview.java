package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogGroupOverview {
    private final int numberOfDogs;
    private final String breed;
    private final int minYear;
    private final int maxYear;
    private final PedigreeCompleteness pedigreeCompleteness;
    private final Inbreeding inbreeding;

    public DogGroupOverview(int numberOfDogs, String breed, int minYear, int maxYear, PedigreeCompleteness pedigreeCompleteness, Inbreeding inbreeding) {
        this.numberOfDogs = numberOfDogs;
        this.breed = breed;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.pedigreeCompleteness = pedigreeCompleteness;
        this.inbreeding = inbreeding;
    }

    public int getNumberOfDogs() {
        return numberOfDogs;
    }

    public String getBreed() {
        return breed;
    }

    public int getMinYear() {
        return minYear;
    }

    public int getMaxYear() {
        return maxYear;
    }

    public PedigreeCompleteness getPedigreeCompleteness() {
        return pedigreeCompleteness;
    }

    public Inbreeding getInbreeding() {
        return inbreeding;
    }
}
