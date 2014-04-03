package no.nkk.dogpopulation.graph.dataerror.breed;

import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class IncorrectBreedAlgorithm {

    private final GraphDatabaseService graphDb;
    private final ExecutionEngine engine;

    public IncorrectBreedAlgorithm(GraphDatabaseService graphDb, ExecutionEngine engine) {
        this.graphDb = graphDb;
        this.engine = engine;
    }

    public List<String> findDataError(int skip, int limit, String breedSynonym) {
        List<String> list = new ArrayList<>();

        Long idOfBreed = GraphUtils.getBreedNodeId(engine, breedSynonym);
        if (idOfBreed == null) {
            return list;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("breedNodeId", idOfBreed);
        StringBuilder query = new StringBuilder();
        query.append("MATCH \n");
        query.append("  (d:DOG)-[:HAS_PARENT {role:'father'}]->(f), \n");
        query.append("  (d)-[:HAS_PARENT {role:'mother'}]->(m), \n");
        query.append("  (d)-[:IS_BREED]->(bd), \n");
        query.append("  (f)-[:IS_BREED]->(bf), \n");
        query.append("  (m)-[:IS_BREED]->(bm), \n");
        query.append("  (bd)-[:MEMBER_OF]->(theBreed:BREED) \n");
        query.append("WHERE \n");
        query.append("  bf = bm AND bd <> bf \n");
        query.append("  AND ID(theBreed)={breedNodeId} \n");
        query.append("RETURN \n");
        query.append("  d.uuid \n");
        query.append("ORDER BY d.uuid \n");
        query.append("SKIP ").append(skip).append(" \n");
        query.append("LIMIT ").append(limit).append(" \n");
        ExecutionResult result1 = engine.execute(query.toString(), params);
        try (ResourceIterator<Map<String,Object>> iterator = result1.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                list.add((String) record.get("d.uuid"));
            }
        }
        return list;
    }

    public IncorrectBreedRecord findDataError(String uuid) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uuid", uuid);
        StringBuilder query = new StringBuilder();
        query.append("MATCH \n");
        query.append("  (d:DOG {uuid:{uuid}})-[:HAS_PARENT {role:'father'}]->(f), \n");
        query.append("  (d)-[:HAS_PARENT {role:'mother'}]->(m), \n");
        query.append("  (d)-[:IS_BREED]->(bd), \n");
        query.append("  (f)-[:IS_BREED]->(bf), \n");
        query.append("  (m)-[:IS_BREED]->(bm) \n");
        query.append("WHERE \n");
        query.append("  bf = bm AND bd <> bf \n");
        query.append("RETURN \n");
        query.append("  d.uuid, f.uuid, m.uuid, bd.synonym, bf.synonym, bm.synonym \n");
        query.append("ORDER BY d.uuid \n");
        ExecutionResult result1 = engine.execute(query.toString(), params);
        try (ResourceIterator<Map<String,Object>> iterator = result1.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                IncorrectBreedRecord ibr = new IncorrectBreedRecord();
                ibr.setUuid((String) record.get("d.uuid"));
                ibr.setBreed((String) record.get("bd.synonym"));
                ibr.setFatherUuid((String) record.get("f.uuid"));
                ibr.setFatherBreed((String) record.get("bf.synonym"));
                ibr.setMotherUuid((String) record.get("m.uuid"));
                ibr.setMotherBreed((String) record.get("bm.synonym"));
                return ibr;
            }
        }
        return null;
    }

}
