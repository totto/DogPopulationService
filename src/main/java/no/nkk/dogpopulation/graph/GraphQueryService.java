package no.nkk.dogpopulation.graph;

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


    public TopLevelDog getPedigree(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node node = findDog(uuid);
            if (node == null) {
                return null; // dog not found
            }
            TopLevelDog dog = getPedigree(node);
            double coi3 = computeCoefficientOfInbreeding(uuid, 3);
            double coi6 = computeCoefficientOfInbreeding(uuid, 6);
            dog.setInbreedingCoefficient3((int) Math.round(100 * coi3));
            dog.setInbreedingCoefficient6((int) Math.round(100 * coi6));
            tx.success();
            return dog;
        }
    }


    public void populateDescendantUuids(String uuid, Set<String> descendants) {
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterable<Node> iterator = graphDb.findNodesByLabelAndProperty(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
            Node node = iterator.iterator().next();
            populateDescendantUuids(node, descendants);
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
            Node dog = getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
            double coi = new InbreedingAlgorithm(graphDb).computeSewallWrightCoefficientOfInbreeding(dog, generations);
            tx.success();
            return coi;
        }
    }


    private void populateDescendantUuids(Node dog, Collection<? super String> descendants) {
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

    private TopLevelDog getPedigree(Node node) {
        return getPedigreeOfTopLevel(node);
    }

    private TopLevelDog getPedigreeOfTopLevel(Node node) {
        String uuid = (String) node.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) node.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        TopLevelDog dog = new TopLevelDog();
        dog.setName(name);
        dog.setBreed(breed);
        dog.setUuid(uuid);
        if (node.hasProperty(DogGraphConstants.DOG_REGNO)) {
            String regNo = (String) node.getProperty(DogGraphConstants.DOG_REGNO);
            dog.getIds().put(DogGraphConstants.DOG_REGNO, regNo);
        }

        Ancestry ancestry = getAncestry(node);
        if (ancestry != null) {
            dog.setAncestry(ancestry);
        }

        return dog;
    }

    private Dog getPedigreeOfDog(Node node) {
        String uuid = (String) node.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) node.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);
        if (node.hasProperty(DogGraphConstants.DOG_REGNO)) {
            String regNo = (String) node.getProperty(DogGraphConstants.DOG_REGNO);
            dog.getIds().put(DogGraphConstants.DOG_REGNO, regNo);
        }

        Ancestry ancestry = getAncestry(node);
        if (ancestry != null) {
            dog.setAncestry(ancestry);
        }

        return dog;
    }

    private Ancestry getAncestry(Node node) {
        Dog fatherDog = null;
        Dog motherDog = null;

        Iterable<Relationship> parentRelationIterable = node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
        for (Relationship parentRelation : parentRelationIterable) {
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            switch(parentRole) {
                case FATHER:
                    fatherDog = getParentDog(parentRelation.getEndNode());
                    break;
                case MOTHER:
                    motherDog = getParentDog(parentRelation.getEndNode());
                    break;
            }
        }

        Iterable<Relationship> invalidParentRelationIterable = node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.OWN_ANCESTOR);
        for (Relationship parentRelation : invalidParentRelationIterable) {
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            switch(parentRole) {
                case FATHER:
                    fatherDog = getInvalidAncestorDog(parentRelation.getEndNode());
                    break;
                case MOTHER:
                    motherDog = getInvalidAncestorDog(parentRelation.getEndNode());
                    break;
            }
        }

        if (fatherDog == null && motherDog == null) {
            return null; // no known parents, do not create ancestry
        }

        // at least one known parent in graph

        Ancestry ancestry = new Ancestry(fatherDog, motherDog);
        return ancestry;
    }

    private Dog getParentDog(Node parent) {
        if (parent == null) {
            return null;
        }
        return getPedigreeOfDog(parent);
    }

    private Dog getInvalidAncestorDog(Node parent) {
        if (parent == null) {
            return null;
        }

        String uuid = (String) parent.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) parent.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = parent.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);

        dog.setOwnAncestor(true); // makes an invalid ancestor

        return dog;
    }

    private Node findDog(String uuid) {
        ResourceIterable<Node> dogIterator = graphDb.findNodesByLabelAndProperty(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
        try (ResourceIterator<Node> iterator = dogIterator.iterator()) {
            if (iterator.hasNext()) {
                // found the dog
                return iterator.next();
            }
        }
        return null;
    }

    private Node getBreedNode(String breed) {
        return getSingleNode(DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);
    }

    private Node getSingleNode(DogGraphLabel label, String property, String value) {
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
}
