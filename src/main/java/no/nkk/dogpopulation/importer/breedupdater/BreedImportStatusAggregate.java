package no.nkk.dogpopulation.importer.breedupdater;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"numberOfActiveImports", "activeList", "dogsPerSecond", "dogsearchLookupsPerSecond", "graphLookupsPerSecond", "graphBuildOperationsPerSecond", "breedList"})
public class BreedImportStatusAggregate {

    private final List<BreedImportStatus> breedList;

    public BreedImportStatusAggregate(List<BreedImportStatus> breedList) {
        this.breedList = breedList;
    }

    public int getNumberOfActiveImports() {
        int n = 0;
        for (BreedImportStatus progress : breedList) {
            if (progress.isActive()) {
                n++;
            }
        }
        return n;
    }

    public String getDogsPerSecond() {
        double combinedThroughput = 0;
        for (BreedImportStatus progress : breedList) {
            if (progress.isActive()) {
                combinedThroughput += progress.computeDogsPerSecond();
            }
        }
        return new DecimalFormat("0.0").format(combinedThroughput);
    }

    public String getDogsearchLookupsPerSecond() {
        double combinedThroughput = 0;
        for (BreedImportStatus progress : breedList) {
            if (progress.isActive()) {
                combinedThroughput += progress.computeDogsearchLookupsPerSecond();
            }
        }
        return new DecimalFormat("0.0").format(combinedThroughput);
    }

    public String getGraphLookupsPerSecond() {
        double combinedThroughput = 0;
        for (BreedImportStatus progress : breedList) {
            if (progress.isActive()) {
                combinedThroughput += progress.computeGraphLookupsPerSecond();
            }
        }
        return new DecimalFormat("0.0").format(combinedThroughput);
    }

    public String getGraphBuildOperationsPerSecond() {
        double combinedThroughput = 0;
        for (BreedImportStatus progress : breedList) {
            if (progress.isActive()) {
                combinedThroughput += progress.computeGraphBuildOperationsPerSecond();
            }
        }
        return new DecimalFormat("0.0").format(combinedThroughput);
    }

    public List<String> getActiveList() {
        List<String> activeList = new ArrayList<>();
        for (BreedImportStatus progress : breedList) {
            if (progress.isActive()) {
                activeList.add(progress.getBreed());
            }
        }
        return activeList;
    }

    public List<BreedImportStatus> getBreedList() {
        return breedList;
    }
}
