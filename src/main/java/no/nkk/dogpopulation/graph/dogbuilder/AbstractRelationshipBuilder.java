package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractRelationshipBuilder implements Builder<Relationship> {

    protected Relationship result;

    @Override
    public Relationship build(GraphDatabaseService graphDb) {
        if (result != null) {
            return result;
        }
        return doBuild(graphDb);
    }

    protected abstract Relationship doBuild(GraphDatabaseService graphDb);
}
