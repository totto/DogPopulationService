package no.nkk.dogpopulation.importer.dogsearch;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class TraversalStatistics {
    final String id;
    final AtomicInteger dogsAdded = new AtomicInteger();
    final AtomicInteger dogsUpdated = new AtomicInteger();
    final AtomicInteger litterCount = new AtomicInteger();
    final AtomicInteger puppiesAdded = new AtomicInteger();
    final AtomicInteger dogsearchHit = new AtomicInteger();
    final AtomicInteger dogsearchMiss = new AtomicInteger();
    final AtomicInteger dogsearchPuppyHit = new AtomicInteger();
    final AtomicInteger dogsearchPuppyMiss = new AtomicInteger();
    final AtomicInteger graphPuppyHit = new AtomicInteger();
    final AtomicInteger graphPuppyMiss = new AtomicInteger();
    final AtomicInteger graphHit = new AtomicInteger();
    final AtomicInteger graphMiss = new AtomicInteger();
    final AtomicInteger fathersAdded = new AtomicInteger();
    final AtomicInteger mothersAdded = new AtomicInteger();
    final AtomicInteger maxDepth = new AtomicInteger();
    final AtomicInteger minDepth = new AtomicInteger(Integer.MAX_VALUE);
    final AtomicInteger graphBuildCount = new AtomicInteger();

    TraversalStatistics(String id) {
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
