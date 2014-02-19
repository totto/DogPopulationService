package no.nkk.dogpopulation.graph.dogbuilder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractNodeBuilder implements NodeBuilder {

    protected Node result;

    protected AbstractNodeBuilder() {
    }

    @Override
    public Node build(GraphDatabaseService graphDb) {
        if (result != null) {
            return result;
        }
        return doBuild(graphDb);
    }

    protected abstract Node doBuild(GraphDatabaseService graphDb);
}
