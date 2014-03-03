package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CircularParentChainAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(CircularParentChainAlgorithm.class);

    private final GraphDatabaseService graphDb;
    private final ExecutionEngine engine;

    public CircularParentChainAlgorithm(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        engine = new ExecutionEngine(graphDb);
    }

    public List<Relationship> run(String uuid) {
        StringBuilder query = new StringBuilder();
        query.append("MATCH (dog:DOG {uuid:{uuid}})-[p:HAS_PARENT*]->(dog)\n");
        query.append("RETURN p");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uuid", uuid);
        ExecutionResult result = engine.execute(query.toString(), params);
        try (ResourceIterator<Map<String,Object>> iterator = result.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                List<Relationship> hasParent = (List<Relationship>) record.get("p");
                return hasParent;
            }
        }
        return null;
    }
}
