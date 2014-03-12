package no.nkk.dogpopulation.graph.dataerror.gender;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
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
public class IncorrectOrMissingGenderAlgorithm {

    private final GraphDatabaseService graphDb;
    private final ExecutionEngine engine;

    public IncorrectOrMissingGenderAlgorithm(GraphDatabaseService graphDb, ExecutionEngine engine) {
        this.graphDb = graphDb;
        this.engine = engine;
    }

    public List<String> findDataError(int skip, int limit) {
        List<String> list = new ArrayList<>();
        ExecutionResult result1 = engine.execute("MATCH (p:DOG)<-[r:HAS_PARENT]-(c) WHERE (p.gender='female' AND r.role='father') OR (p.gender='male' AND r.role='mother') RETURN DISTINCT p.uuid SKIP " + skip + " LIMIT " + limit);
        try (ResourceIterator<Map<String,Object>> iterator = result1.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                list.add((String) record.get("p.uuid"));
            }
        }
        return list;
    }

    public IncorrectGenderRecord findDataError(String uuid) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("uuid", uuid);
        ExecutionResult result1 = engine.execute("MATCH (p:DOG {uuid:{uuid}})<-[r:HAS_PARENT]-(c) WHERE (p.gender='female' AND r.role='father') OR (p.gender='male' AND r.role='mother') RETURN DISTINCT p", params);
        try (ResourceIterator<Map<String,Object>> iterator = result1.iterator()) {
            for (Map<String,Object> record : IteratorUtil.asIterable(iterator)) {
                Node parent = (Node) record.get("p");
                IncorrectGenderRecord igr = populateRecord(parent);
                return igr;
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
