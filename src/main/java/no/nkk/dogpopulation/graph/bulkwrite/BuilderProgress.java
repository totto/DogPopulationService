package no.nkk.dogpopulation.graph.bulkwrite;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.Node;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BuilderProgress {
    private final boolean alreadyInProgress;
    private final Future<Node> future;
    private final Builder<Node> builder;

    public BuilderProgress(boolean alreadyInProgress, Future<Node> future, Builder<Node> builder) {
        this.alreadyInProgress = alreadyInProgress;
        this.future = future;
        this.builder = builder;
    }

    public boolean isAlreadyInProgress() {
        return alreadyInProgress;
    }
    public Future<Node> getFuture() {
        return future;
    }
    public Builder<Node> getBuilder() {
        return builder;
    }
}
