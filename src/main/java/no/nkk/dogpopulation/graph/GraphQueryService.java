package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.graph.inbreeding.InbreedingAlgorithm;
import no.nkk.dogpopulation.graph.pedigree.PedigreeAlgorithm;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompletenessAlgorithm;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


    public GraphQueryService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    public Node getDog(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
            tx.success();
            return dog;
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


    public Node getDogIfItHasAtLeastOneParent(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(uuid);
            if (dog == null) {
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


    public TopLevelDog getPedigree(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node node = getDogNode(uuid);
            if (node == null) {
                return null; // dog not found
            }
            TopLevelDog dog = new PedigreeAlgorithm().getPedigree(node);
            double coi3 = computeCoefficientOfInbreeding(uuid, 3);
            double coi6 = computeCoefficientOfInbreeding(uuid, 6);
            dog.setInbreedingCoefficient3((int) Math.round(10000 * coi3));
            dog.setInbreedingCoefficient6((int) Math.round(10000 * coi6));
            tx.success();
            return dog;
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
    public double computeCoefficientOfInbreeding(String uuid, int generations) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(uuid);
            double coi = new InbreedingAlgorithm(graphDb, generations).computeSewallWrightCoefficientOfInbreeding(dog);
            tx.success();
            return coi;
        }
    }


    public PedigreeCompleteness getPedigreeCompletenessOfGroup(int generations, Set<String> breedSet, int minYear, int maxYear) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        try (Transaction tx = graphDb.beginTx()) {
            Node categoryBreedNode = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            PedigreeCompletenessAlgorithm algorithm = new PedigreeCompletenessAlgorithm(graphDb, generations);
            PedigreeCompleteness pedigreeCompletenessOfGroup = algorithm.getPedigreeCompletenessOfGroup(categoryBreedNode, breedSet, minYear, maxYear);
            tx.success();
            return pedigreeCompletenessOfGroup;
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
        ResourceIterable<Node> breedNodeIterator = graphDb.findNodesByLabelAndProperty(label, property, value);
        try (ResourceIterator<Node> iterator = breedNodeIterator.iterator()) {
            if (!iterator.hasNext()) {
                return null; // node not found
            }
            Node firstMatch = iterator.next();
            if (!iterator.hasNext()) {
                return firstMatch; // only match
            }
            // more than one node match
            LOGGER.warn("More than one node match: label={}, property={}, value={}", label.name(), property, value);
            return firstMatch; // we could throw an exception here
        }
    }

    Node getDogNode(String uuid) {
        return getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
    }
}
