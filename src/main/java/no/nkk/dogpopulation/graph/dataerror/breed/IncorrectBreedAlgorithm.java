package no.nkk.dogpopulation.graph.dataerror.breed;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import no.nkk.dogpopulation.graph.dataerror.gender.ChildRecord;
import no.nkk.dogpopulation.graph.dataerror.gender.IncorrectGenderRecord;
import no.nkk.dogpopulation.graph.dataerror.gender.LitterRecord;
import org.joda.time.LocalDate;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.*;
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


    private IncorrectGenderRecord populateRecord(Node dogNode) {
        /*
         * Dog properties
         */
        IncorrectGenderRecord record = new IncorrectGenderRecord();
        record.setUuid((String) dogNode.getProperty(DogGraphConstants.DOG_UUID));
        if (dogNode.hasProperty(DogGraphConstants.DOG_NAME)) {
            record.setName((String) dogNode.getProperty(DogGraphConstants.DOG_NAME));
        }
        if (dogNode.hasProperty(DogGraphConstants.DOG_GENDER)) {
            record.setGender((String) dogNode.getProperty(DogGraphConstants.DOG_GENDER));
        }
        if (dogNode.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
            int bornYear = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_YEAR);
            int bornMonth = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_MONTH);
            int bornDay = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_DAY);
            record.setBorn(new LocalDate(bornYear, bornMonth, bornDay).toString());
        }
        Relationship isBreed = dogNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = isBreed.getEndNode();
        record.setBreed((String) breedNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM));

        /*
         * Litters
         */
        List<LitterRecord> litters = new ArrayList<>();
        for (Relationship hasLitter : dogNode.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
            Node litterNode = hasLitter.getEndNode();
            LitterRecord litterRecord = new LitterRecord();
            if (litterNode.hasProperty(DogGraphConstants.LITTER_ID)) {
                litterRecord.setId((String) litterNode.getProperty(DogGraphConstants.LITTER_ID));
            }
            if (litterNode.hasProperty(DogGraphConstants.LITTER_COUNT)) {
                litterRecord.setCount((Integer) litterNode.getProperty(DogGraphConstants.LITTER_COUNT));
            }
            if (litterNode.hasProperty(DogGraphConstants.LITTER_YEAR)) {
                int bornYear = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_YEAR);
                int bornMonth = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_MONTH);
                int bornDay = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_DAY);
                litterRecord.setBorn(new LocalDate(bornYear, bornMonth, bornDay).toString());
            }
            if (hasLitter.hasProperty(DogGraphConstants.HASLITTER_ROLE)) {
                litterRecord.setRole((String) hasLitter.getProperty(DogGraphConstants.HASLITTER_ROLE));
            }
            litters.add(litterRecord);
        }
        record.setLitters(litters);

        /*
         * Children
         */
        List<ChildRecord> children = new ArrayList<>();
        for (Relationship hasParent : dogNode.getRelationships(Direction.INCOMING, DogGraphRelationshipType.HAS_PARENT)) {
            Node childNode = hasParent.getStartNode();
            ChildRecord childRecord = new ChildRecord();
            childRecord.setUuid((String) childNode.getProperty(DogGraphConstants.DOG_UUID));
            if (childNode.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                int bornYear = (Integer) childNode.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                int bornMonth = (Integer) childNode.getProperty(DogGraphConstants.DOG_BORN_MONTH);
                int bornDay = (Integer) childNode.getProperty(DogGraphConstants.DOG_BORN_DAY);
                childRecord.setBorn(new LocalDate(bornYear, bornMonth, bornDay).toString());
            }
            if (hasParent.hasProperty(DogGraphConstants.HASLITTER_ROLE)) {
                childRecord.setParentRole((String) hasParent.getProperty(DogGraphConstants.HASLITTER_ROLE));
            }
            for (Relationship otherHasParent : childNode.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT)) {
                if (otherHasParent.equals(hasParent)) {
                    continue;
                }
                Node otherParentNode = otherHasParent.getEndNode();
                childRecord.setOtherParentUuid((String) otherParentNode.getProperty(DogGraphConstants.DOG_UUID));
                childRecord.setOtherParentName((String) otherParentNode.getProperty(DogGraphConstants.DOG_NAME));
                childRecord.setOtherParentRole((String) otherHasParent.getProperty(DogGraphConstants.HASPARENT_ROLE));
                break; // only consider first other hasParent relationship
            }
            children.add(childRecord);
        }
        record.setChildren(children);

        return record;
    }
}
