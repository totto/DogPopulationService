package no.nkk.dogpopulation.graph.dogbuilder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractRelationshipBuilder implements RelationshipBuilder {

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
