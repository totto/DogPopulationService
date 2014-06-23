package no.nkk.dogpopulation.graph.hdindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.graph.*;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularAncestryBreedGroupAlgorithm;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularParentChainAlgorithm;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularRecord;
import no.nkk.dogpopulation.graph.dataerror.gender.IncorrectOrMissingGenderAlgorithm;
import no.nkk.dogpopulation.importer.dogsearch.DogDetails;
import no.nkk.dogpopulation.importer.dogsearch.DogHealth;
import no.nkk.dogpopulation.importer.dogsearch.DogHealthHD;
import no.nkk.dogpopulation.importer.dogsearch.DogId;
import org.joda.time.DateTime;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuHdIndexAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmuHdIndexAlgorithm.class);

    private final GraphDatabaseService graphDb;

    private final Set<String> breed;

    private final CommonTraversals commonTraversals;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CircularAncestryBreedGroupAlgorithm circularAncestryBreedGroupAlgorithm;
    private final CircularParentChainAlgorithm circularParentChainAlgorithm;
    private final IncorrectOrMissingGenderAlgorithm incorrectOrMissingGenderAlgorithm;

    private final DmuDataset dataset = new DmuDataset();

    public DmuHdIndexAlgorithm(GraphDatabaseService graphDb, ExecutionEngine engine, Set<String> breed) {
        this.graphDb = graphDb;
        this.breed = breed;
        this.commonTraversals = new CommonTraversals(graphDb);
        this.circularAncestryBreedGroupAlgorithm = new CircularAncestryBreedGroupAlgorithm(graphDb, engine);
        this.circularParentChainAlgorithm = new CircularParentChainAlgorithm(graphDb, engine);
        this.incorrectOrMissingGenderAlgorithm = new IncorrectOrMissingGenderAlgorithm(graphDb, engine);
    }

    public void writeFiles(File dataFile, File pedigreeFile, File uuidMappingFile, File breedCodeMappingFile, File dataErrorFile) {
        DmuDataset dmuDataset = extractData();
        DmuDatasetWriter dmuDatasetWriter = new DmuDatasetWriter(dataFile, pedigreeFile, uuidMappingFile, breedCodeMappingFile, dataErrorFile);
        dmuDatasetWriter.writeFiles(dmuDataset);
    }

    DmuDataset extractData() {
        Set<Long> visitedNodes = new HashSet<>();
        Set<Long> dataErrorDogNodes = new LinkedHashSet<>();
        markDogsWithCircularAncestry(dataErrorDogNodes);
        markDogsWithIncorrectGender(dataErrorDogNodes);
        for (Path breedSynonymPath : commonTraversals.traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(breed)) {
            buildDataset(visitedNodes, breedSynonymPath, dataErrorDogNodes);
        }
        return dataset;
    }


    private void markDogsWithCircularAncestry(Set<Long> dataErrorDogNodes) {
        List<String> circleDogs = circularAncestryBreedGroupAlgorithm.run(breed);
        for (int i=0; i<circleDogs.size(); i++) {
            String circleDog = circleDogs.get(i);
            List<CircularRecord> circle = circularParentChainAlgorithm.run(circleDog);
            if (circle == null) {
                continue;
            }
            for (CircularRecord cr : circle) {
                Node dog = GraphUtils.getSingleNode(graphDb, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, cr.getUuid());
                if (dog == null) {
                    continue;
                }
                dataErrorDogNodes.add(dog.getId());
                dataset.add(new DmuDataErrorRecord(dog.getId(), "CIRCLE " + i + " -- " + dog.getId() + "  " + cr.getUuid()));
            }
        }
    }


    private void markDogsWithIncorrectGender(Set<Long> dataErrorDogNodes) {
        for (String breedSynonym : breed) {
            List<String> uuids = incorrectOrMissingGenderAlgorithm.findDataError(0, 10000000, breedSynonym);
            for (String uuid : uuids) {
                Node dog = GraphUtils.getSingleNode(graphDb, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
                if (dog == null) {
                    continue;
                }
                dataErrorDogNodes.add(dog.getId());
                dataset.add(new DmuDataErrorRecord(dog.getId(), "GENDER -- " + dog.getId() + "  " + uuid));
            }
        }
    }


    private void buildDataset(Set<Long> visitedNodes, Path breedSynonymPath, Set<Long> dataErrorDogNodes) {
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

        dataset.add(new DmuBreedCodeRecord(breedNkkId, breedName));

        for (Path path : commonTraversals.traverseDogsOfBreed(breedSynonymNode)) {
            Node dogNode = path.endNode();

            if (visitedNodes.contains(dogNode.getId())) {
                continue;
            }

            if (dataErrorDogNodes.contains(dogNode.getId())) {
                continue; // do not include dogs that have known data-errors
            }

            String uuid = (String) dogNode.getProperty(DogGraphConstants.DOG_UUID);

            DogDetails dogDetails = getDogDetails(dogNode, uuid);
            if (dogDetails == null) {
                continue;
            }

            if (!dogIsRegisteredInNKK(dogDetails)) {
                continue;
            }

            visitedNodes.add(dogNode.getId());

            int born = DmuPedigreeRecord.UNKNOWN;
            if (dogNode.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                int year = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                int month = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_MONTH);
                int day = (Integer) dogNode.getProperty(DogGraphConstants.DOG_BORN_DAY);
                born = 10000 * year + 100 * month + day;
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


            int litterId = DmuDataRecord.UNKNOWN;
            if (dogNode.hasRelationship(DogGraphRelationshipType.IN_LITTER)) {
                for (Relationship inLitter : dogNode.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.IN_LITTER)) {
                    Node litterNode = inLitter.getEndNode();
                    litterId = (int) litterNode.getId();
                    break; // use first valid litter-id that can be found
                }
            }

            long motherId = -breedNkkId;
            long fatherId = -breedNkkId;
            if (dogNode.hasRelationship(DogGraphRelationshipType.HAS_PARENT)) {
                for (Relationship hasParent : dogNode.getRelationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING)) {
                    Node parentNode = hasParent.getEndNode();
                    ParentRole parentRole = ParentRole.valueOf(((String) hasParent.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
                    long parentNodeId = parentNode.getId();
                    if (dataErrorDogNodes.contains(parentNodeId)) {
                        continue; // do not link to parents that have known data-errors
                    }
                    switch (parentRole) {
                        case FATHER:
                            fatherId = parentNodeId;
                            break;
                        case MOTHER:
                            motherId = parentNodeId;
                            break;
                    }
                }
            }

            long id = dogNode.getId();

            HdYearAndScore hdYearAndScore = getHdScore(dogDetails, uuid);
            if (hdYearAndScore != null) {
                // only use records with known HD score in data file.
                int xRayYear = hdYearAndScore.hdXray.getYear();
                int hdScore = hdYearAndScore.hdScore;
                int breedHdXrayYearGender = (100000 * breedNkkId) + (10 * xRayYear) + gender;
                dataset.add(new DmuDataRecord(id, breedNkkId, xRayYear, gender, breedHdXrayYearGender, litterId, motherId, hdScore));
            }

            dataset.add(new DmuPedigreeRecord(id, fatherId, motherId, born, breedNkkId));

            dataset.add(new DmuUuidRecord(id, uuid));
        }
    }


    private boolean dogIsRegisteredInNKK(DogDetails dogDetails) {
        DogId[] ids = dogDetails.getIds();
        if (ids == null) {
            return false;
        }
        boolean hasHuid = false;
        boolean hasRegno = false;
        for (DogId dogId : ids) {
            if ("huid".equalsIgnoreCase(dogId.getType())) {
                hasHuid = true;
            } else if ("RegNo".equalsIgnoreCase(dogId.getType())) {
                hasRegno = true;
            }
        }
        if (!hasHuid) {
            return true;
        }
        return hasRegno;
    }


    private DogDetails getDogDetails(Node dogNode, String uuid) {
        if (!dogNode.hasProperty(DogGraphConstants.DOG_JSON)) {
            return null;
        }
        String json = (String) dogNode.getProperty(DogGraphConstants.DOG_JSON);
        try {
            return objectMapper.readValue(json, DogDetails.class);
        } catch (IOException e) {
            LOGGER.warn("JSON of dog {} cannot be parsed", uuid);
        }
        return null;
    }

    private HdYearAndScore getHdScore(DogDetails dogDetails, String uuid) {
        DogHealth health = dogDetails.getHealth();
        if (health == null) {
            return null;
        }
        DogHealthHD[] dogHealthHDs = health.getHd();
        if (dogHealthHDs == null) {
            return null;
        }
        HdYearAndScore bestCandidate = null;
        for (DogHealthHD dogHealthHD : dogHealthHDs) {
            String hdDiag = dogHealthHD.getDiagnosis();
            if (hdDiag == null) {
                continue;
            }
            int hdScore;
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
            } else {
                continue; // not a valid diagnosis
            }
            DateTime xRay = getXray(uuid, dogHealthHD);
            if (xRay == null) {
                continue;
            }
            if (bestCandidate == null || xRay.isAfter(bestCandidate.hdXray)) {
                bestCandidate = new HdYearAndScore(xRay, hdScore);
            }
        }
        return bestCandidate;
    }

    private DateTime getXray(String uuid, DogHealthHD dogHealthHD) {
        String xray = dogHealthHD.getXray();
        if (xray == null) {
            return null;
        }
        if (xray.trim().isEmpty()) {
            return null;
        }
        try {
            DateTime dateTime = DateTime.parse(xray);
            return dateTime;
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to parse xRay as DateTime: uuid={}, xRay={}", uuid, xray);
        }
        return null;
    }


    static class HdYearAndScore {
        final DateTime hdXray;
        final int hdScore;
        HdYearAndScore(DateTime hdXray, int hdScore) {
            this.hdXray = hdXray;
            this.hdScore = hdScore;
        }
    }
}
