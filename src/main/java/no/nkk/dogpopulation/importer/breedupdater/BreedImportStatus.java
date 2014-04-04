package no.nkk.dogpopulation.importer.breedupdater;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.importer.dogsearch.TraversalStatistics;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"breed", "active", "updatedTo", "progress", "windowTasks", "windowCompleted", "tasksCompleted", "totalTasks", "dogsAddedToGraph", "elapsedSeconds", "dogsPerSecond",
        "dogsAdded", "dogsUpdated", "litterCount", "puppiesAdded", "dogsearchHit", "dogsearchMiss", "graphHit", "graphMiss", "fathersAdded", "mothersAdded", "graphBuildCount"})
public class BreedImportStatus {

    private final AtomicInteger windowTasks = new AtomicInteger();
    private final AtomicInteger windowCompleted = new AtomicInteger();
    private final AtomicInteger totalTasks = new AtomicInteger();
    private final AtomicInteger tasksCompleted = new AtomicInteger();
    private final String breed;
    private final AtomicLong startTimeMs = new AtomicLong();
    private final AtomicLong elapsedTimeMs = new AtomicLong();
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicReference<String> updatedTo = new AtomicReference<>(DogGraphConstants.BEGINNING_OF_TIME.toString());

    @JsonIgnore
    private final TraversalStatistics ts;

    
    public BreedImportStatus(String breed) {
        this.breed = breed;
        this.ts = new TraversalStatistics(breed);
    }

    public void setWindowTasks(int windowTasks) {
        this.windowTasks.set(windowTasks);
    }

    public int getWindowTasks() {
        return windowTasks.get();
    }

    public void setWindowCompleted(int windowCompleted) {
        this.windowCompleted.set(windowCompleted);
    }

    public int getWindowCompleted() {
        return windowCompleted.get();
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks.set(totalTasks);
    }

    public int getTotalTasks() {
        return totalTasks.get();
    }

    public void updateStartTime() {
        startTimeMs.set(System.currentTimeMillis());
    }

    public void updateReferenceTime() {
        elapsedTimeMs.set(System.currentTimeMillis() - startTimeMs.get());
    }

    public void updateComplete() {
        active.set(false);
    }

    public boolean isActive() {
        return active.get();
    }

    public void recordTaskComplete() {
        tasksCompleted.incrementAndGet();
        windowCompleted.incrementAndGet();
        updateReferenceTime();
    }

    public String getBreed() {
        return breed;
    }

    public int getElapsedSeconds() {
        return (int) (elapsedTimeMs.get() / 1000);
    }


    public double computeGraphLookupsPerSecond() {
        int elapsedSeconds = getElapsedSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (double) (ts.graphHit.get() + ts.graphMiss.get() + ts.graphPuppyHit.get() + ts.graphPuppyMiss.get()) / elapsedSeconds;
    }

    public double computeDogsearchLookupsPerSecond() {
        int elapsedSeconds = getElapsedSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (double) (ts.dogsearchHit.get() + ts.dogsearchMiss.get() + ts.dogsearchPuppyHit.get() + ts.dogsearchPuppyMiss.get()) / elapsedSeconds;
    }

    public double computeGraphBuildOperationsPerSecond() {
        int elapsedSeconds = getElapsedSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (double) ts.graphBuildCount.get() / elapsedSeconds;
    }

    public double computeDogsPerSecond() {
        int elapsedSeconds = getElapsedSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (double) (ts.dogsAdded.get() + ts.dogsUpdated.get() + ts.puppiesAdded.get()) / elapsedSeconds;
    }

    public String getDogsPerSecond() {
        return new DecimalFormat("0.0").format(computeDogsPerSecond());
    }

    public String getProgress() {
        if (windowCompleted.get() == 0) {
            return "0.00";
        }
        return new DecimalFormat("0.00").format(100.0 * windowCompleted.get() / windowTasks.get());
    }

    public void setUpdatedTo(String updatedTo) {
        this.updatedTo.set(updatedTo);
    }

    public String getUpdatedTo() {
        return updatedTo.get();
    }

    @Override
    public String toString() {
        return "(" + breed + ", " + getUpdatedTo() + ", " + getProgress() + "%)";
    }

    @JsonIgnore
    public TraversalStatistics getTraversalStatistics() {
        return ts;
    }

    public int getTasksCompleted() {
        return tasksCompleted.get();
    }

    public int getDogsAdded() {
        return ts.dogsAdded.get();
    }

    public int getDogsUpdated() {
        return ts.dogsUpdated.get();
    }

    public int getLitterCount() {
        return ts.litterCount.get();
    }

    public int getPuppiesAdded() {
        return ts.puppiesAdded.get();
    }

    public int getDogsearchHit() {
        return ts.dogsearchHit.get();
    }

    public int getDogsearchMiss() {
        return ts.dogsearchMiss.get();
    }

    public int getGraphHit() {
        return ts.graphHit.get();
    }

    public int getGraphMiss() {
        return ts.graphMiss.get();
    }

    public int getFathersAdded() {
        return ts.fathersAdded.get();
    }

    public int getMothersAdded() {
        return ts.mothersAdded.get();
    }

    public int getDogsearchPuppyHit() {
        return ts.dogsearchPuppyHit.get();
    }

    public int getDogsearchPuppyMiss() {
        return ts.dogsearchPuppyMiss.get();
    }

    public int getGraphPuppyHit() {
        return ts.graphPuppyHit.get();
    }

    public int getGraphPuppyMiss() {
        return ts.graphPuppyMiss.get();
    }

    public int getGraphBuildCount() {
        return ts.graphBuildCount.get();
    }
}
