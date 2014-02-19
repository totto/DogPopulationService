package no.nkk.dogpopulation.graph.bulkwrite;

import org.neo4j.graphdb.Node;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ConcurrentProgress {
    private final boolean alreadyInProgress;
    private final Future<Node> future;

    public ConcurrentProgress(boolean alreadyInProgress, Future<Node> future) {
        this.alreadyInProgress = alreadyInProgress;
        this.future = future;
    }

    public boolean isAlreadyInProgress() {
        return alreadyInProgress;
    }
    public Future<Node> getFuture() {
        return future;
    }
}
