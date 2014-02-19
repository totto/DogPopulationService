package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface Builder<V> {

    V build(GraphDatabaseService graphDb);
}
