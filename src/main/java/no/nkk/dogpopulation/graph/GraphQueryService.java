package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.graph.dataerror.breed.IncorrectBreedAlgorithm;
import no.nkk.dogpopulation.graph.dataerror.breed.IncorrectBreedRecord;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularAncestryBreedGroupAlgorithm;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularParentChainAlgorithm;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularRecord;
import no.nkk.dogpopulation.graph.dataerror.gender.IncorrectGenderRecord;
import no.nkk.dogpopulation.graph.dataerror.gender.IncorrectOrMissingGenderAlgorithm;
import no.nkk.dogpopulation.graph.hdindex.DmuHdIndexAlgorithm;
import no.nkk.dogpopulation.graph.hdxray.HDXrayStatistics;
import no.nkk.dogpopulation.graph.hdxray.HDXrayStatisticsAlgorithm;
import no.nkk.dogpopulation.graph.inbreeding.InbreedingAlgorithm;
import no.nkk.dogpopulation.graph.inbreeding.InbreedingOfGroup;
import no.nkk.dogpopulation.graph.inbreeding.InbreedingOfGroupAlgorithm;
import no.nkk.dogpopulation.graph.inbreeding.InbreedingResult;
import no.nkk.dogpopulation.graph.litter.LitterStatistics;
import no.nkk.dogpopulation.graph.litter.LitterStatisticsAlgorithm;
import no.nkk.dogpopulation.graph.pedigree.Ancestry;
import no.nkk.dogpopulation.graph.pedigree.Dog;
import no.nkk.dogpopulation.graph.pedigree.PedigreeAlgorithm;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompletenessAlgorithm;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.*;

/**
 * All public methods must wrap access to the graph-database within a transaction. Private methods may assume that
 * they are called within the context of an already open transaction.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQueryService.class);


    private final GraphDatabaseService graphDb;
    private final ExecutionEngine engine;


    public GraphQueryService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        engine = new ExecutionEngine(graphDb);
    }


    public void writeDmuFiles(PrintWriter dataFile, PrintWriter pedigreeFile, PrintWriter uuidMappingfile, Set<String> breed) {
        try (Transaction tx = graphDb.beginTx()) {
            DmuHdIndexAlgorithm algorithm = new DmuHdIndexAlgorithm(graphDb, dataFile, pedigreeFile, uuidMappingfile, breed);
            algorithm.writeFiles();
            tx.success();
        }
    }


    public List<String> getBreeds() {
        try (Transaction tx = graphDb.beginTx()) {
            Node breedRoot = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            if (breedRoot == null) {
                return Collections.emptyList();
            }
            List<String> breeds = new ArrayList<>(1000);
            for (Path path : graphDb.traversalDescription()
                    .depthFirst()
                    .relationships(DogGraphRelationshipType.MEMBER_OF, Direction.INCOMING)
                    .evaluator(Evaluators.includingDepths(1, 1))
                    .traverse(breedRoot)) {
                Node dogOfBreed = path.endNode();
                breeds.add((String) dogOfBreed.getProperty(DogGraphConstants.BREED_BREED));
            }
            tx.success();
            return breeds;
        }
    }


    public List<String> getBreedList(String breed) {
        try (Transaction tx = graphDb.beginTx()) {
            Node breedRoot = getBreedNode(breed);
            if (breedRoot == null) {
                return Collections.emptyList();
            }
            List<String> dogIds = new ArrayList<>(10000);
            for (Path path : graphDb.traversalDescription()
                    .depthFirst()
                    .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                    .evaluator(Evaluators.includingDepths(1, 1))
                    .traverse(breedRoot)) {
                Node dogOfBreed = path.endNode();
                dogIds.add((String) dogOfBreed.getProperty(DogGraphConstants.DOG_UUID));
            }
            tx.success();
            return dogIds;
        }
    }


    public Node getDog(String id) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(id);
            tx.success();
            return dog;
        }
    }


    public GraphDogLookupResult lookupDog(String id) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(id);
            if (dog == null) {
                tx.success();
                return null;
            }
            Iterable<Relationship> parentRelationships = dog.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
            boolean atLeastOneConnectedParent = parentRelationships.iterator().hasNext();
            tx.success();
            return new GraphDogLookupResult(dog, atLeastOneConnectedParent);
        }
    }


    public Node getDogIfItHasAtLeastOneParent(String id) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(id);
            if (dog == null) {
                tx.success();
                return null;
            }
            Iterable<Relationship> parentRelationships = dog.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
            boolean atLeastOneConnectedParent = parentRelationships.iterator().hasNext();
            tx.success();
            if (atLeastOneConnectedParent) {
                return dog;
            }
            return null; // dog exists but without parents
        }
    }


    public TopLevelDog getPedigree(String id) {
        try (Transaction tx = graphDb.beginTx()) {
            Node node = getDogNode(id);
            if (node == null) {
                return null; // dog not found
            }
            TopLevelDog dog = new PedigreeAlgorithm(graphDb).getPedigree(node);
            InbreedingResult inbreedingResult3 = computeCoefficientOfInbreeding(id, 3);
            InbreedingResult inbreedingResult6 = computeCoefficientOfInbreeding(id, 6);
            updateInbreedingContributions(dog, inbreedingResult3, inbreedingResult6);
            tx.success();
            return dog;
        }
    }

    private void updateInbreedingContributions(Dog dog, InbreedingResult inbreedingResult3, InbreedingResult inbreedingResult6) {
        dog.setInbreedingCoefficient3(0.0);
        dog.setInbreedingCoefficient6(0.0);
        Map<String, List<Dog>> dogByUuid = new LinkedHashMap<>();
        populateMap(dog, dogByUuid);
        for (Map.Entry<String, Double> e : inbreedingResult3.getCoiByContributingAncestor().entrySet()) {
            String uuid = e.getKey();
            Double coi = e.getValue();
            List<Dog> dogs = dogByUuid.get(uuid);
            for (Dog d : dogs) {
                d.setInbreedingCoefficient3(100 * coi);
            }
        }
        for (Map.Entry<String, Double> e : inbreedingResult6.getCoiByContributingAncestor().entrySet()) {
            String uuid = e.getKey();
            Double coi = e.getValue();
            List<Dog> dogs = dogByUuid.get(uuid);
            for (Dog d : dogs) {
                d.setInbreedingCoefficient6(100 * coi);
            }
        }
    }

    private void populateMap(Dog dog, Map<String, List<Dog>> dogByUuid) {
        List<Dog> dogs = dogByUuid.get(dog.getUuid());
        if (dogs == null) {
            dogs = new LinkedList<>();
            dogByUuid.put(dog.getUuid(), dogs);
        }
        dogs.add(dog);
        Ancestry ancestry = dog.getAncestry();
        if (ancestry == null) {
            return;
        }
        Dog father = ancestry.getFather();
        if (father != null) {
            populateMap(father, dogByUuid);
        }
        Dog mother = ancestry.getMother();
        if (mother != null) {
            populateMap(mother, dogByUuid);
        }
    }

    public TopLevelDog getPedigree(String uuid, String name, String fatherUuid, String motherUuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node fatherNode = getDogNode(fatherUuid);
            if (fatherNode == null) {
                return null; // preant-1 not found
            }
            Node motherNode = getDogNode(motherUuid);
            if (motherNode == null) {
                return null; // parent-2 not found
            }
            TopLevelDog father = new PedigreeAlgorithm(graphDb).getPedigree(fatherNode);
            TopLevelDog mother = new PedigreeAlgorithm(graphDb).getPedigree(motherNode);
            InbreedingResult inbreedingResult3 = new InbreedingAlgorithm(graphDb, 3).computeSewallWrightCoefficientOfInbreeding(uuid, fatherNode, motherNode);
            InbreedingResult inbreedingResult6 = new InbreedingAlgorithm(graphDb, 6).computeSewallWrightCoefficientOfInbreeding(uuid, fatherNode, motherNode);

            TopLevelDog ficticiousDog = new TopLevelDog();
            ficticiousDog.setUuid(uuid);
            ficticiousDog.setName(name);
            updateInbreedingContributions(ficticiousDog, inbreedingResult3, inbreedingResult6);
            ficticiousDog.setAncestry(new Ancestry(father, mother));
            tx.success();
            return ficticiousDog;
        }
    }


    public void populateDescendantUuids(Node dog, Set<String> descendants) {
        try (Transaction tx = graphDb.beginTx()) {
            populateDescendantIds(dog, descendants);
            tx.success();
        }
    }


    /**
     * Compute the "Coefficient Of Inbreeding" using the method by geneticist Sewall Wright. The computation is done
     * within a single Neo4j transaction.
     *
     * @param uuid the uuid of the dog for which we want the inbreeding coefficient of.
     * @param generations how many generations to use from the pedigree.
     * @return the Coefficient Of Inbreeding.
     */
    public InbreedingResult computeCoefficientOfInbreeding(String uuid, int generations) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(uuid);
            InbreedingResult inbreedingResult = new InbreedingAlgorithm(graphDb, generations).computeSewallWrightCoefficientOfInbreeding(dog);
            tx.success();
            return inbreedingResult;
        }
    }


    public PedigreeCompleteness getPedigreeCompletenessOfGroup(int generations, Set<String> breedSet, int minYear, int maxYear) {
        try (Transaction tx = graphDb.beginTx()) {
            Node categoryBreedNode = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            PedigreeCompletenessAlgorithm algorithm = new PedigreeCompletenessAlgorithm(graphDb, generations);
            PedigreeCompleteness pedigreeCompletenessOfGroup = algorithm.getPedigreeCompletenessOfGroup(categoryBreedNode, breedSet, minYear, maxYear);
            tx.success();
            return pedigreeCompletenessOfGroup;
        }
    }


    public HDXrayStatistics getHDXrayStatisticsOfGroupBornBetween(Set<String> breedSet, int minYear, int maxYear) {
        try (Transaction tx = graphDb.beginTx()) {
            HDXrayStatisticsAlgorithm algorithm = new HDXrayStatisticsAlgorithm(graphDb);
            HDXrayStatistics hdXrayStatistics = algorithm.hdXrayStatisticsForDogsOfBreedBornBetween(breedSet, minYear, maxYear);
            tx.success();
            return hdXrayStatistics;
        }
    }


    public HDXrayStatistics getHDXrayStatisticsOfGroupHdXRayedBetween(Set<String> breedSet, int minYear, int maxYear) {
        try (Transaction tx = graphDb.beginTx()) {
            HDXrayStatisticsAlgorithm algorithm = new HDXrayStatisticsAlgorithm(graphDb);
            HDXrayStatistics hdXrayStatistics = algorithm.hdXrayStatisticsForDogsOfBreedXrayedBetween(breedSet, minYear, maxYear);
            tx.success();
            return hdXrayStatistics;
        }
    }


    public InbreedingOfGroup getInbreedingOfGroup(int generations, Set<String> breedSet, int minYear, int maxYear) {
        try (Transaction tx = graphDb.beginTx()) {
            Node categoryBreedNode = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            InbreedingOfGroupAlgorithm algorithm = new InbreedingOfGroupAlgorithm(graphDb, generations);
            InbreedingOfGroup inbreedingOfGroup = algorithm.getInbreedingOfGroup(categoryBreedNode, breedSet, minYear, maxYear);
            tx.success();
            return inbreedingOfGroup;
        }
    }


    public LitterStatistics getLitterStatisticsOfGroup(Set<String> breed, int minYear, int maxYear) {
        try (Transaction tx = graphDb.beginTx()) {
            Node categoryBreedNode = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            LitterStatisticsAlgorithm algorithm = new LitterStatisticsAlgorithm(graphDb, breed, minYear, maxYear, categoryBreedNode);
            LitterStatistics litterStatistics = algorithm.execute();
            tx.success();
            return litterStatistics;
        }
    }


    public List<String> getAllDogsWithInconsistentGender(int skip, int limit) {
        try (Transaction tx = graphDb.beginTx()) {
            IncorrectOrMissingGenderAlgorithm algorithm = new IncorrectOrMissingGenderAlgorithm(graphDb, engine);
            List<String> result = algorithm.findDataError(skip, limit);
            tx.success();
            return result;
        }
    }


    public IncorrectGenderRecord getDogWithInconsistentGender(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            IncorrectOrMissingGenderAlgorithm algorithm = new IncorrectOrMissingGenderAlgorithm(graphDb, engine);
            IncorrectGenderRecord igr = algorithm.findDataError(uuid);
            tx.success();
            return igr;
        }
    }


    public List<String> getAllDogsWithInconsistentBreed(int skip, int limit) {
        try (Transaction tx = graphDb.beginTx()) {
            IncorrectBreedAlgorithm algorithm = new IncorrectBreedAlgorithm(graphDb, engine);
            List<String> result = algorithm.findDataError(skip, limit);
            tx.success();
            return result;
        }
    }


    public IncorrectBreedRecord getDogWithInconsistentBreed(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            IncorrectBreedAlgorithm algorithm = new IncorrectBreedAlgorithm(graphDb, engine);
            IncorrectBreedRecord ibr = algorithm.findDataError(uuid);
            tx.success();
            return ibr;
        }
    }


    public List<CircularRecord> getCircluarParentChainInAncestryOf(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            CircularParentChainAlgorithm algorithm = new CircularParentChainAlgorithm(graphDb, engine);
            List<CircularRecord> circle = algorithm.run(uuid);
            tx.success();
            return circle;
        }
    }


    public List<String> getCircluarParentChainInAncestryOf(Set<String> breedSet) {
        try (Transaction tx = graphDb.beginTx()) {
            CircularAncestryBreedGroupAlgorithm algorithm = new CircularAncestryBreedGroupAlgorithm(graphDb, engine);
            List<String> uuids = algorithm.run(breedSet);
            tx.success();
            return uuids;
        }
    }


    private void populateDescendantIds(Node dog, Collection<? super String> descendants) {
        for (Path position : graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NODE_PATH)
                .relationships(DogGraphRelationshipType.HAS_PARENT, Direction.INCOMING)
                .evaluator(Evaluators.excludeStartPosition())
                .traverse(dog)) {
            Node descendant = position.endNode();
            String uuid = (String) descendant.getProperty(DogGraphConstants.DOG_UUID);
            if (descendants.contains(uuid)) {
                return; // more than one path to descendant, this is because of inbreeding
            }
            descendants.add(uuid);
        }
    }

    private Node getBreedNode(String breed) {
        return getSingleNode(DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);
    }

    Node getSingleNode(DogGraphLabel label, String property, String value) {
        return GraphUtils.getSingleNode(graphDb, label, property, value);
    }

    Node getDogNode(String id) {
        Node dog = getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, id);
        if (dog != null) {
            return dog; // found by UUID
        }
        return getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_REGNO, id);
    }
}
