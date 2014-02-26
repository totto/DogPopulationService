package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractNodeBuilder implements Builder<Node> {

    protected final Object lock = new Object();

    protected Node result;
    protected boolean dirty = true;

    protected AbstractNodeBuilder() {
    }

    @Override
    public Node build(GraphDatabaseService graphDb) {
        synchronized (lock) {
            if (result == null) {
                result = doBuild(graphDb);
            }
            return result;
        }
    }

    @Override
    public void reset() {
        synchronized (lock) {
            if (dirty) {
                result = null;
            }
        }
    }

    public <T> T build(Node result) {
        synchronized (lock) {
            this.result = result;
            dirty = false; // make sure that the result will never be reset.
        }
        return (T) this;
    }

    protected abstract Node doBuild(GraphDatabaseService graphDb);
}
