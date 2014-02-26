package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractRelationshipBuilder implements Builder<Relationship> {

    protected final Object lock = new Object();

    protected Relationship result;

    protected AbstractRelationshipBuilder() {
    }

    @Override
    public Relationship build(GraphDatabaseService graphDb) {
        synchronized (lock) {
            if (result == null) {
                result = doBuild(graphDb);;
            }
            return result;
        }
    }

    @Override
    public void reset() {
        synchronized (lock) {
            result = null;
        }
    }

    protected abstract Relationship doBuild(GraphDatabaseService graphDb);
}
