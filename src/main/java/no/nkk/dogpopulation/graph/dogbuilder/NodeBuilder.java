package no.nkk.dogpopulation.graph.dogbuilder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface NodeBuilder {

    Node build(GraphDatabaseService graphDb);
}
