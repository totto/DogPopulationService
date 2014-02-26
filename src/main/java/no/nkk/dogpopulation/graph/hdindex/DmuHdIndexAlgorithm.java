package no.nkk.dogpopulation.graph.hdindex;

import no.nkk.dogpopulation.graph.*;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuHdIndexAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmuHdIndexAlgorithm.class);

    private final GraphDatabaseService graphDb;

    public DmuHdIndexAlgorithm(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public DmuFiles getDmuFiles(String breed) {
        DmuDataFile dmuDataFile = new DmuDataFile();
        DmuPedigreeFile dmuPedigreeFile = new DmuPedigreeFile();
        Node breedNode = GraphUtils.getSingleNode(graphDb, DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);
        if (breedNode == null) {
            return null;
        }
        Integer breedCode = DmuDataRecord.UNKNOWN;
        if (breedNode.hasProperty(DogGraphConstants.BREED_ID)) {
            String breedIdStr = (String) breedNode.getProperty(DogGraphConstants.BREED_ID);
            try {
                breedCode = Integer.valueOf(breedIdStr);
            } catch (Exception ignore) {
            }
        }
        int nextId = 1;
        for (Path path : traverseDogsOfBreed(breedNode)) {
            Node dogNode = path.endNode();

            int born = DmuPedigreeRecord.UNKNOWN;
            if (dogNode.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                int year = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                int month = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_MONTH);
                int day = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_DAY);
                LocalDate bornDate = new LocalDate(year, month, day);
                born = 10000 * year + 100 * month + day;
            }

            int hdXrayYear = DmuDataRecord.UNKNOWN;
            if (dogNode.hasProperty(DogGraphConstants.DOG_HDYEAR)) {
                hdXrayYear = (Integer) dogNode.getProperty(DogGraphConstants.DOG_HDYEAR);
            }

            double hdScore = DmuDataRecord.UNKNOWN;
            if (dogNode.hasProperty(DogGraphConstants.DOG_HDDIAG)) {
                String hdDiag = (String) dogNode.getProperty(DogGraphConstants.DOG_HDDIAG);
                if (hdDiag.startsWith("A")) {
                    hdScore = 1;
                } else if (hdDiag.startsWith("B")) {
                    hdScore = 1.5;
                } else if (hdDiag.startsWith("C")) {
                    hdScore = 2;
                } else if (hdDiag.startsWith("D")) {
                    hdScore = 3;
                } else if (hdDiag.startsWith("E")) {
                    hdScore = 4.3;
                }
            }

            if (hdScore == DmuDataRecord.UNKNOWN) {
                continue; // TODO for demo purposes, only use records with HD score.
            }

            int gender = 1; // TODO figure out numeric values for male and female
            int breedHdXrayYearGender = (100000 * breedCode) + (10 * hdXrayYear) + gender;


            int litterId = DmuDataRecord.UNKNOWN;
            if (dogNode.hasRelationship(DogGraphRelationshipType.IN_LITTER)) {
                Relationship inLitter = dogNode.getSingleRelationship(DogGraphRelationshipType.IN_LITTER, Direction.OUTGOING);
                Node litterNode = inLitter.getEndNode();
                try {
                    litterId = Integer.parseInt((String) litterNode.getProperty(DogGraphConstants.LITTER_ID));
                } catch (RuntimeException ignore) {
                }
            }

            int motherId = DmuDataRecord.UNKNOWN;
            int fatherId = DmuPedigreeRecord.UNKNOWN;
            if (dogNode.hasRelationship(DogGraphRelationshipType.HAS_PARENT)) {
                for (Relationship hasParent : dogNode.getRelationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING)) {
                    Node parentNode = hasParent.getEndNode();
                    ParentRole parentRole = ParentRole.valueOf(((String) hasParent.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
                    switch (parentRole) {
                        case FATHER:
                            fatherId = (int) parentNode.getId();
                            break;
                        case MOTHER:
                            motherId = (int) parentNode.getId();
                            break;
                    }
                }
            }

            // TODO use a proper id generation scheme
            int id = (int) dogNode.getId();

            DmuDataRecord dmuDataRecord = new DmuDataRecord(id, breedCode, hdXrayYear, breedHdXrayYearGender, litterId, motherId, hdScore);
            dmuDataFile.getRecords().add(dmuDataRecord);

            DmuPedigreeRecord dmuPedigreeRecord = new DmuPedigreeRecord(id, fatherId, motherId, born, breedCode);
            dmuPedigreeFile.getRecords().add(dmuPedigreeRecord);

            if (nextId++ > 100) {
                break; // TODO remove break, for now we limit data for demo purposes
            }
        }

        // TODO sort records

        DmuFiles dmuFiles = new DmuFiles(dmuDataFile, dmuPedigreeFile);
        return dmuFiles;
    }

    private Traverser traverseDogsOfBreed(Node breedNode) {
        return graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NODE_PATH)
                .relationships(DogGraphRelationshipType.IS_BREED)
                .evaluator(Evaluators.excludeStartPosition())
                .traverse(breedNode);
    }

}
