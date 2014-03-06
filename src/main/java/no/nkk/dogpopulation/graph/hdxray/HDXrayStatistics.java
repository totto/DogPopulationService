package no.nkk.dogpopulation.graph.hdxray;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"breed", "minYear", "maxYear", "dogCount", "percentageWithDiagnose", "countByDiagnose"})
public class HDXrayStatistics {
    private final Set<String> breed;
    private final int minYear;
    private final int maxYear;
    private final int dogCount;
    private final Map<String, Integer> countByDiagnose;

    public HDXrayStatistics(Set<String> breed, int minYear, int maxYear, int dogCount, Map<String, Integer> countByDiagnose) {
        this.breed = breed;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.dogCount = dogCount;
        this.countByDiagnose = countByDiagnose;
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

    public int getDogCount() {
        return dogCount;
    }

    public double getPercentageWithDiagnose() {
        int sum = 0;
        for (Integer value : countByDiagnose.values()) {
            sum += value;
        }
        return 100.0 * sum / dogCount;
    }

    public Map<String, Integer> getCountByDiagnose() {
        return countByDiagnose;
    }
}
