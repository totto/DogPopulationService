package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphUtils.class);

    public static Node getSingleNode(GraphDatabaseService graphDb, DogGraphLabel label, String property, String value) {
        ResourceIterable<Node> breedNodeIterator = graphDb.findNodesByLabelAndProperty(label, property, value);
        try (ResourceIterator<Node> iterator = breedNodeIterator.iterator()) {
            if (!iterator.hasNext()) {
                return null; // node not found
            }
            Node firstMatch = iterator.next();
            if (!iterator.hasNext()) {
                return firstMatch; // only match
            }
            // more than one node match
            LOGGER.warn("More than one node match: label={}, property={}, value={}", label.name(), property, value);
            return firstMatch; // we could throw an exception here
        }
    }

}
