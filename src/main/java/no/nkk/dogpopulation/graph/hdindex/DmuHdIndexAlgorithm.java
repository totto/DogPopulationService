package no.nkk.dogpopulation.graph.hdindex;

import no.nkk.dogpopulation.graph.*;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuHdIndexAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmuHdIndexAlgorithm.class);

    private final GraphDatabaseService graphDb;

    private final File dataFile;
    private final File pedigreeFile;
    private final File uuidMappingFile;
    private final File breedCodeMappingFile;

    private final Set<String> breed;

    private final CommonTraversals commonTraversals;

    public DmuHdIndexAlgorithm(GraphDatabaseService graphDb, File dataFile, File pedigreeFile, File uuidMappingFile, File breedCodeMappingFile, Set<String> breed) {
        this.graphDb = graphDb;
        this.dataFile = dataFile;
        this.pedigreeFile = pedigreeFile;
        this.uuidMappingFile = uuidMappingFile;
        this.breedCodeMappingFile = breedCodeMappingFile;
        this.breed = breed;
        this.commonTraversals = new CommonTraversals(graphDb);
    }


    public void writeFiles() {
        boolean success = false;
        try(PrintWriter dataOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dataFile), Charset.forName("ISO-8859-1")))) {
            try(PrintWriter pedigreeOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pedigreeFile), Charset.forName("ISO-8859-1")))) {
                try(PrintWriter uuidMappingOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(uuidMappingFile), Charset.forName("ISO-8859-1")))) {
                    try(PrintWriter breedMappingOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(breedCodeMappingFile), Charset.forName("ISO-8859-1")))) {
                        writeFiles(dataOut, pedigreeOut, uuidMappingOut, breedMappingOut);
                    }
                    uuidMappingOut.flush();
                }
                pedigreeOut.flush();
            }
            dataOut.flush();
            success = true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (!success) {
                File folder = dataFile.getParentFile();
                dataFile.delete();
                pedigreeFile.delete();
                uuidMappingFile.delete();
                breedCodeMappingFile.delete();
                folder.delete();
            }
        }
    }

    public void writeFiles(PrintWriter dataWriter, PrintWriter pedigreeWriter, PrintWriter uuidMappingWriter, PrintWriter breedMappingWriter) {
        Set<Long> visitedNodes = new HashSet<>();
        for (Path breedSynonymPath : commonTraversals.traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(breed)) {
            writeBreedToFiles(dataWriter, pedigreeWriter, uuidMappingWriter, breedMappingWriter, visitedNodes, breedSynonymPath);
        }
    }

    private void writeBreedToFiles(PrintWriter dataWriter, PrintWriter pedigreeWriter, PrintWriter uuidMappingWriter, PrintWriter breedMappingWriter, Set<Long> visitedNodes, Path breedSynonymPath) {
        int breedNkkId = -1;
        Node breedSynonymNode = breedSynonymPath.endNode();
        String breedName = "Breed Node does not have breed name set!";
        if (breedSynonymNode.hasProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM)) {
            breedName = (String) breedSynonymNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM);
        }
        Relationship memberOf = breedSynonymNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
        if (memberOf == null) {
            LOGGER.warn("Unable to produce HDIndex files. Breed synonym node not connected to breed node: \"{}\"", breedName);
            throw new UnknownBreedCodeException(breedName);
        }
        Node breedNode = memberOf.getEndNode();
        if (breedNode.hasProperty(DogGraphConstants.BREED_NKK_BREED_ID)) {
            String breedIdStr = (String) breedNode.getProperty(DogGraphConstants.BREED_NKK_BREED_ID);
            try {
                breedNkkId = Integer.parseInt(breedIdStr);
            } catch (RuntimeException ignore) {
            }
        }
        if (breedNkkId <= 0) {
            LOGGER.warn("Unable to produce HDIndex files. No valid breed-code registered for breed: \"{}\"", breedName);
            throw new UnknownBreedCodeException(breedName);
        }

        writeBreedCodeMappingRecord(breedMappingWriter, breedName, breedNkkId);

        for (Path path : commonTraversals.traverseDogsOfBreed(breedSynonymNode)) {
            Node dogNode = path.endNode();

            if (visitedNodes.contains(dogNode.getId())) {
                continue;
            }

            visitedNodes.add(dogNode.getId());

            String uuid = (String) dogNode.getProperty(DogGraphConstants.DOG_UUID);

            int born = DmuPedigreeRecord.UNKNOWN;
            int bornYear = 0; // part of breedBornYearGender, where we probably don't want more than 4 digits.
            if (dogNode.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                bornYear = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                int month = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_MONTH);
                int day = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_DAY);
                born = 10000 * bornYear + 100 * month + day;
            }

            /*
            int hdXrayYear = DmuDataRecord.UNKNOWN;
            if (dogNode.hasProperty(DogGraphConstants.DOG_HDYEAR)) {
                hdXrayYear = (Integer) dogNode.getProperty(DogGraphConstants.DOG_HDYEAR);
            }
            */

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

            int gender = 1; // Default to MALE if gender is unknown
            if (dogNode.hasProperty(DogGraphConstants.DOG_GENDER)) {
                DogGender dogGender = DogGender.valueOf(((String) dogNode.getProperty(DogGraphConstants.DOG_GENDER)).toUpperCase());
                if (dogGender == DogGender.FEMALE) {
                    gender = 2; // FEMALE
                } else if (dogGender == DogGender.MALE) {
                    gender = 1; // MALE
                }
            }
            int breedBornYearGender = (100000 * breedNkkId) + (10 * bornYear) + gender;


            int litterId = DmuDataRecord.UNKNOWN;
            if (dogNode.hasRelationship(DogGraphRelationshipType.IN_LITTER)) {
                for (Relationship inLitter : dogNode.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.IN_LITTER)) {
                    Node litterNode = inLitter.getEndNode();
                    litterId = (int) litterNode.getId();
                    break; // use first valid litter-id that can be found
                }
            }

            int motherId = -breedNkkId;
            int fatherId = -breedNkkId;
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
                DmuDataRecord dmuDataRecord = new DmuDataRecord(id, breedNkkId, bornYear, gender, breedBornYearGender, litterId, motherId, hdScore);
                dmuDataRecord.writeTo(dataWriter);
            }

            DmuPedigreeRecord dmuPedigreeRecord = new DmuPedigreeRecord(id, fatherId, motherId, born, breedNkkId);
            dmuPedigreeRecord.writeTo(pedigreeWriter);

            writeUuidMappingRecord(id, uuid, uuidMappingWriter);

        }
    }


    private void writeUuidMappingRecord(int id, String uuid, PrintWriter uuidMappingWriter) {
        final String NEWLINE = "\r\n";
        uuidMappingWriter.print(id);
        uuidMappingWriter.print(" ");
        uuidMappingWriter.print(uuid);
        uuidMappingWriter.print(NEWLINE);
    }

    private void writeBreedCodeMappingRecord(PrintWriter breedMappingWriter, String breedName, int breedCode) {
        final String NEWLINE = "\r\n";
        breedMappingWriter.print(breedCode);
        breedMappingWriter.print(" ");
        breedMappingWriter.print(breedName);
        breedMappingWriter.print(NEWLINE);
    }

}
