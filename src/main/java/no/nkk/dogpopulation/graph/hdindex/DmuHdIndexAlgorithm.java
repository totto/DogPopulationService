package no.nkk.dogpopulation.graph.hdindex;

import no.nkk.dogpopulation.graph.*;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuHdIndexAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmuHdIndexAlgorithm.class);

    private final GraphDatabaseService graphDb;

    private final PrintWriter dataFile;
    private final PrintWriter pedigreeFile;
    private final PrintWriter uuidMappingfile;
    private final Set<String> breed;

    private final CommonTraversals commonTraversals;

    public DmuHdIndexAlgorithm(GraphDatabaseService graphDb, PrintWriter dataFile, PrintWriter pedigreeFile, PrintWriter uuidMappingfile, Set<String> breed) {
        this.graphDb = graphDb;
        this.dataFile = dataFile;
        this.pedigreeFile = pedigreeFile;
        this.uuidMappingfile = uuidMappingfile;
        this.breed = breed;
        this.commonTraversals = new CommonTraversals(graphDb);
    }


    public void writeFiles() {
        Set<Long> visitedNodes = new HashSet<>();
        Node categoryBreedNode = GraphUtils.getSingleNode(graphDb, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
        for (Path breedPath : commonTraversals.traverseBreedInSet(categoryBreedNode, breed)) {
            int breedCode = DmuDataRecord.UNKNOWN;
            Node breedNode = breedPath.endNode();
            if (breedNode.hasProperty(DogGraphConstants.BREED_ID)) {
                String breedIdStr = (String) breedNode.getProperty(DogGraphConstants.BREED_ID);
                try {
                    breedCode = Integer.parseInt(breedIdStr);
                } catch (RuntimeException ignore) {
                }
            }
            for (Path path : traverseDogsOfBreed(breedNode)) {
                Node dogNode = path.endNode();

                if (visitedNodes.contains(dogNode.getId())) {
                    continue;
                }

                visitedNodes.add(dogNode.getId());

                String uuid = (String) dogNode.getProperty(DogGraphConstants.DOG_UUID);

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

                int hdScore = DmuDataRecord.UNKNOWN;
                if (dogNode.hasProperty(DogGraphConstants.DOG_HDDIAG)) {
                    String hdDiag = (String) dogNode.getProperty(DogGraphConstants.DOG_HDDIAG);
                    if (hdDiag.startsWith("A")) {
                        hdScore = 1;
                    } else if (hdDiag.startsWith("B")) {
                        hdScore = 2;
                    } else if (hdDiag.startsWith("C")) {
                        hdScore = 3;
                    } else if (hdDiag.startsWith("D")) {
                        hdScore = 4;
                    } else if (hdDiag.startsWith("E")) {
                        hdScore = 5;
                    }
                }

                int gender = 1; // TODO assign correct gender! 1 means MALE, and 2 means FEMALE
                int breedHdXrayYearGender = (100000 * breedCode) + (10 * hdXrayYear) + gender;


                int litterId = DmuDataRecord.UNKNOWN;
                if (dogNode.hasRelationship(DogGraphRelationshipType.IN_LITTER)) {
                    for (Relationship inLitter : dogNode.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.IN_LITTER)) {
                        Node litterNode = inLitter.getEndNode();
                        try {
                            litterId = Integer.parseInt((String) litterNode.getProperty(DogGraphConstants.LITTER_ID));
                            break; // use first valid litter-id that can be found
                        } catch (RuntimeException ignore) {
                        }
                    }
                }

                int motherId = -breedCode;
                int fatherId = -breedCode;
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

                // TODO use a reproducible id generation scheme
                int id = (int) dogNode.getId();

                if (hdScore != DmuDataRecord.UNKNOWN) {
                    // only use records with known HD score in data file.
                    DmuDataRecord dmuDataRecord = new DmuDataRecord(id, breedCode, hdXrayYear, gender, breedHdXrayYearGender, litterId, motherId, hdScore);
                    dmuDataRecord.writeTo(dataFile);
                }

                DmuPedigreeRecord dmuPedigreeRecord = new DmuPedigreeRecord(id, fatherId, motherId, born, breedCode);
                dmuPedigreeRecord.writeTo(pedigreeFile);

                writeUuidMappingRecord(id, uuid);
            }
        }
    }


    private void writeUuidMappingRecord(int id, String uuid) {
        final String NEWLINE = "\r\n";
        uuidMappingfile.print(id);
        uuidMappingfile.print(" ");
        uuidMappingfile.print(uuid);
        uuidMappingfile.print(NEWLINE);
    }


    private Traverser traverseDogsOfBreed(Node breedNode) {
        return graphDb.traversalDescription()
                .depthFirst()
                .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                .evaluator(Evaluators.atDepth(1))
                .traverse(breedNode);
    }

}
