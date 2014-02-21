package no.nkk.dogpopulation.graph.inbreeding;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nkk.dogpopulation.graph.BasicStatistics;
import no.nkk.dogpopulation.graph.UuidAndRegNo;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"numberOfDogs", "breed", "minYear", "maxYear", "generations", "coefficient", "statistics", "frequency", "dogsWithCoefficientAbove1250", "dogsWithCoefficientAbove2500"})
public class InbreedingOfGroup {

    private final Set<String> breed;
    private final int minYear;
    private final int maxYear;
    private final int generations; // typically 3 or 6
    private final BasicStatistics statistics;
    private final int[] frequency;
    private final UuidAndRegNo[] dogsWithCoefficientAbove1250;
    private final UuidAndRegNo[] dogsWithCoefficientAbove2500;
    private final UuidAndRegNo[] dogsWithCoefficientAbove3000;


    public InbreedingOfGroup(Set<String> breed, int minYear, int maxYear, int generations, BasicStatistics statistics, int[] frequency, List<UuidAndRegNo> dogsWithCoefficientAbove1250, List<UuidAndRegNo> dogsWithCoefficientAbove2500, List<UuidAndRegNo> dogsWithCoefficientAbove3000) {
        this.breed = breed;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.generations = generations;
        this.statistics = statistics;
        this.frequency = frequency;
        this.dogsWithCoefficientAbove1250 = dogsWithCoefficientAbove1250.toArray(new UuidAndRegNo[dogsWithCoefficientAbove1250.size()]);
        this.dogsWithCoefficientAbove2500 = dogsWithCoefficientAbove2500.toArray(new UuidAndRegNo[dogsWithCoefficientAbove2500.size()]);
        this.dogsWithCoefficientAbove3000 = dogsWithCoefficientAbove3000.toArray(new UuidAndRegNo[dogsWithCoefficientAbove3000.size()]);
    }

    public int getNumberOfDogs() {
        return (int) statistics.getN();
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

    public int getGenerations() {
        return generations;
    }

    public BasicStatistics getStatistics() {
        return statistics;
    }

    public int[] getFrequency() {
        return frequency;
    }

    public UuidAndRegNo[] getDogsWithCoefficientAbove1250() {
        return dogsWithCoefficientAbove1250;
    }

    public UuidAndRegNo[] getDogsWithCoefficientAbove2500() {
        return dogsWithCoefficientAbove2500;
    }

    public UuidAndRegNo[] getDogsWithCoefficientAbove3000() {
        return dogsWithCoefficientAbove3000;
    }

}
