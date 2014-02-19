package no.nkk.dogpopulation.importer.dogsearch;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedImportStatus {

    private final AtomicInteger originalPedigreeCount = new AtomicInteger();
    private final AtomicInteger tasksCompleted = new AtomicInteger();
    private final String breed;
    private final AtomicLong startTimeMs = new AtomicLong();
    private final AtomicLong elapsedTimeMs = new AtomicLong();
    private final AtomicBoolean active = new AtomicBoolean(false);

    private final AtomicInteger count = new AtomicInteger();

    public BreedImportStatus(String breed) {
        this.breed = breed;
    }

    public void setOriginalPedigreeCount(int originalPedigreeCount) {
        this.originalPedigreeCount.set(originalPedigreeCount);
    }

    public int getOriginalPedigreeCount() {
        return originalPedigreeCount.get();
    }

    public void updateStartTime() {
        active.set(true);
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

    public void updateWith(TraversalStatistics ts) {
        count.addAndGet(ts.dogCount.get());
        tasksCompleted.incrementAndGet();
        updateReferenceTime();
    }

    public int getCount() {
        return count.get();
    }

    public String getBreed() {
        return breed;
    }

    public int getElapsedSeconds() {
        return (int) (elapsedTimeMs.get() / 1000);
    }

    public double computeDogsPerSecond() {
        int elapsedSeconds = getElapsedSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (double) count.get() / elapsedSeconds;
    }

    public String getDogsPerSecond() {
        return new DecimalFormat("0.0").format(computeDogsPerSecond());
    }

    public String getProgress() {
        if (tasksCompleted.get() == 0) {
            return "0.00";
        }
        return new DecimalFormat("0.00").format(100.0 * tasksCompleted.get() / originalPedigreeCount.get());
    }

    public int getTasksCompleted() {
        return tasksCompleted.get();
    }
}
