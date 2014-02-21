package no.nkk.dogpopulation.graph.pedigreecompleteness;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nkk.dogpopulation.graph.BasicStatistics;

import java.util.List;
import java.util.Set;

/**
 * Describes the pedigree completeness for a a group of dogs.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"generations", "breed", "minYear", "maxYear", "numberOfDogs", "sizeOfCompletePedigree", "numberOfDogsWithCompletePedigree", "numberOfDogsWithEmptyPedigree", "pedigreeCompletenessStatistics", "pedigreeSizeStatistics", "pedigreeSizeHistogram", "dogsWithEmptyPedigree", "dogsWithJustOneParent"})
public class PedigreeCompleteness {
    private final int generations; // typically 3 or 6
    private final Set<String> breed;
    private final int minYear;
    private final int maxYear;
    private final BasicStatistics pedigreeSizeStatistics;
    private final BasicStatistics pedigreeCompletenessStatistics;
    private final int[] pedigreeSizeHistogram;
    private final String[] dogsWithEmptyPedigree;
    private final String[] dogsWithJustOneParent;

    public PedigreeCompleteness(int generations, Set<String> breed, int minYear, int maxYear, BasicStatistics pedigreeSizeStatistics, BasicStatistics pedigreeCompletenessStatistics, int[] pedigreeSizeHistogram, List<String> dogsWithEmptyPedigree, List<String> dogsWithJustOneParent) {
        this.generations = generations;
        this.pedigreeSizeStatistics = pedigreeSizeStatistics;
        this.pedigreeCompletenessStatistics = pedigreeCompletenessStatistics;
        this.breed = breed;
        this.minYear = minYear;
        this.maxYear =  maxYear;
        this.pedigreeSizeHistogram = pedigreeSizeHistogram;
        this.dogsWithEmptyPedigree = dogsWithEmptyPedigree.toArray(new String[dogsWithEmptyPedigree.size()]);
        this.dogsWithJustOneParent = dogsWithJustOneParent.toArray(new String[dogsWithJustOneParent.size()]);
    }

    public int getGenerations() {
        return generations;
    }

    public int getNumberOfDogs() {
        return (int) pedigreeSizeStatistics.getN();
    }

    public int getSizeOfCompletePedigree() {
        return getSizeOfCompletePedigree(generations);
    }

    public static int getSizeOfCompletePedigree(int generations) {
        return ((int) Math.pow(2, generations)) - 2;
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

    public int getNumberOfDogsWithCompletePedigree() {
        return pedigreeSizeHistogram[pedigreeSizeHistogram.length - 1];
    }

    public int getNumberOfDogsWithEmptyPedigree() {
        return pedigreeSizeHistogram[0];
    }

    public int[] getPedigreeSizeHistogram() {
        return pedigreeSizeHistogram;
    }

    public String[] getDogsWithEmptyPedigree() {
        return dogsWithEmptyPedigree;
    }

    public String[] getDogsWithJustOneParent() {
        return dogsWithJustOneParent;
    }
}
