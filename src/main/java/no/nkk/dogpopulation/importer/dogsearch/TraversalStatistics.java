package no.nkk.dogpopulation.importer.dogsearch;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class TraversalStatistics {
    final String id;
    final AtomicInteger dogCount = new AtomicInteger();
    final AtomicInteger maxDepth = new AtomicInteger();
    final AtomicInteger minDepth = new AtomicInteger(Integer.MAX_VALUE);

    TraversalStatistics(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "TraversalStatistics{" +
                "dogCount=" + dogCount.get() +
                ", maxDepth=" + maxDepth.get() +
                ", minDepth=" + minDepth.get() +
                '}';
    }
}
