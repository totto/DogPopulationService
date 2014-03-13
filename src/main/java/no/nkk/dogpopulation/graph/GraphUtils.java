package no.nkk.dogpopulation.graph;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphUtils.class);


    public static Node getSingleNode(GraphDatabaseService graphDb, DogGraphLabel label, String property, String value) {
        ResourceIterable<Node> nodeIterator = graphDb.findNodesByLabelAndProperty(label, property, value);
        try (ResourceIterator<Node> iterator = nodeIterator.iterator()) {
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


    public static Node findOrCreateNode(GraphDatabaseService graphDb, Label label, String propertyKey, String propertyValue) {
        ResourceIterable<Node> iterable = graphDb.findNodesByLabelAndProperty(label, propertyKey, propertyValue);
        Node node;
        try (ResourceIterator<Node> iterator = iterable.iterator()) {
            if (iterator.hasNext()) {

                // node already exists

                node = iterator.next();

            } else {

                // create new node

                node = graphDb.createNode(label);
                node.setProperty(propertyKey, propertyValue);
            }
        }
        return node;
    }


    public static Long getBreedNodeId(ExecutionEngine engine, String breedSynonym) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("synonym", breedSynonym);
        StringBuilder synonymQuery = new StringBuilder();
        synonymQuery.append("MATCH \n");
        synonymQuery.append("  (s:BREED_SYNONYM {synonym:{synonym}})-[:MEMBER_OF]->(b:BREED) \n");
        synonymQuery.append("RETURN \n");
        synonymQuery.append("  ID(b)\n");
        ExecutionResult syonymResult = engine.execute(synonymQuery.toString(), params);
        try (ResourceIterator<Map<String,Object>> iterator = syonymResult.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                return (Long) record.get("ID(b)");
            }
        }
        return null;
    }

}
