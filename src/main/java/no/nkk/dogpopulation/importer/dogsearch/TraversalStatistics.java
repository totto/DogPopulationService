package no.nkk.dogpopulation.importer.dogsearch;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class TraversalStatistics {
    public final String id;
    public final AtomicInteger dogsAdded = new AtomicInteger();
    public final AtomicInteger dogsUpdated = new AtomicInteger();
    public final AtomicInteger litterCount = new AtomicInteger();
    public final AtomicInteger puppiesAdded = new AtomicInteger();
    public final AtomicInteger dogsearchHit = new AtomicInteger();
    public final AtomicInteger dogsearchMiss = new AtomicInteger();
    public final AtomicInteger dogsearchPuppyHit = new AtomicInteger();
    public final AtomicInteger dogsearchPuppyMiss = new AtomicInteger();
    public final AtomicInteger graphPuppyHit = new AtomicInteger();
    public final AtomicInteger graphPuppyMiss = new AtomicInteger();
    public final AtomicInteger graphHit = new AtomicInteger();
    public final AtomicInteger graphMiss = new AtomicInteger();
    public final AtomicInteger fathersAdded = new AtomicInteger();
    public final AtomicInteger mothersAdded = new AtomicInteger();
    public final AtomicInteger maxDepth = new AtomicInteger();
    public final AtomicInteger minDepth = new AtomicInteger(Integer.MAX_VALUE);
    public final AtomicInteger graphBuildCount = new AtomicInteger();

    public TraversalStatistics(String id) {
        this.id = id;
    }

    public void recordMinimumDepth(int depth) {
        if ((depth - 1) < minDepth.get()) {
            minDepth.set(depth - 1); // TODO perform this check-then-add operation in atomically
        }
    }

    public void recordMaximumDepth(int depth) {
        if (depth > maxDepth.get()) {
            maxDepth.set(depth); // TODO perform this check-then-add operation in atomically
        }
    }

    @Override
    public String toString() {
        return "TraversalStatistics{" +
                "id='" + id + '\'' +
                ", dogsAdded=" + dogsAdded +
                ", dogsUpdated=" + dogsUpdated +
                ", litterCount=" + litterCount +
                ", puppiesAdded=" + puppiesAdded +
                ", dogsearchHit=" + dogsearchHit +
                ", dogsearchMiss=" + dogsearchMiss +
                ", graphHit=" + graphHit +
                ", graphMiss=" + graphMiss +
                ", maxDepth=" + maxDepth +
                ", minDepth=" + minDepth +
                '}';
    }
}
