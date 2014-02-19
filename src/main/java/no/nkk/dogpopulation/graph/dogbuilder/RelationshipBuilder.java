package no.nkk.dogpopulation.graph.dogbuilder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface RelationshipBuilder {

    Relationship build(GraphDatabaseService graphDb);
}
