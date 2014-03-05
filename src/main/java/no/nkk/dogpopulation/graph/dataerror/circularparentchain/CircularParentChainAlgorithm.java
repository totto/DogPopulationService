package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import org.joda.time.LocalDate;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    public CircularParentChainAlgorithm(GraphDatabaseService graphDb, ExecutionEngine engine) {
        this.graphDb = graphDb;
        this.engine = engine;
    }

    public List<CircularRecord> run(String uuid) {
        StringBuilder query = new StringBuilder();
        query.append("MATCH \n");
        query.append("  (dog:DOG {uuid:{uuid}})-[:HAS_PARENT*0..]->(a)-[p:HAS_PARENT*]->(a) \n");
        query.append("RETURN p");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uuid", uuid);
        ExecutionResult result = engine.execute(query.toString(), params);
        try (ResourceIterator<Map<String,Object>> iterator = result.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                List<Relationship> hasParentList = (List<Relationship>) record.get("p");
                List<CircularRecord> circle = new ArrayList<>();
                for (Relationship hasParent : hasParentList) {
                    Node child = hasParent.getStartNode();
                    Node parent = hasParent.getEndNode();
                    CircularRecord circularRecord = new CircularRecord();
                    circularRecord.setUuid((String) child.getProperty(DogGraphConstants.DOG_UUID));
                    if (child.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                        int bornYear = (Integer) child.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                        int bornMonth = (Integer) child.getProperty(DogGraphConstants.DOG_BORN_MONTH);
                        int bornDay = (Integer) child.getProperty(DogGraphConstants.DOG_BORN_DAY);
                        circularRecord.setBorn(new LocalDate(bornYear, bornMonth, bornDay).toString());
                    }
                    circularRecord.setParentRole((String) hasParent.getProperty(DogGraphConstants.HASPARENT_ROLE));
                    circularRecord.setParentUuid((String) parent.getProperty(DogGraphConstants.DOG_UUID));
                    if (parent.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                        int bornYear = (Integer) parent.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                        int bornMonth = (Integer) parent.getProperty(DogGraphConstants.DOG_BORN_MONTH);
                        int bornDay = (Integer) parent.getProperty(DogGraphConstants.DOG_BORN_DAY);
                        circularRecord.setParentBorn(new LocalDate(bornYear, bornMonth, bornDay).toString());
                    }
                    circle.add(circularRecord);
                }
                return circle;
            }
        }
        return null;
    }
}
