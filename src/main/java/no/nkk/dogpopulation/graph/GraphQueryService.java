package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.*;

import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphQueryService {

    private final GraphDatabaseService graphDb;

    public GraphQueryService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    public void populateDescendantUuids(String uuid, Set<String> descendants) {
        // TODO - populate set with the uuids of all descendants of the provided uuid (do not add the provided uuid itself).
        // // This is needed to ensure correct own-ancestor detection and to avoid circular parent relations in graph.
    }


    public Dog getPedigree(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node node = findDog(uuid);
            if (node == null) {
                return null; // dog not found
            }
            Dog dog = recursiveGetDog(node);
            tx.success();
            return dog;
        }
    }

    private Dog recursiveGetDog(Node node) {
        String uuid = (String) node.getProperty("uuid");
        String name = (String) node.getProperty("name");
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty("breed");
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);

        Dog fatherDog = null;
        Dog motherDog = null;

        Iterable<Relationship> parentRelationIterable = node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
        for (Relationship parentRelation : parentRelationIterable) {
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty("role")).toUpperCase());
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
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty("role")).toUpperCase());
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
            return dog; // no known parents, do not create ancestry
        }

        // at least one known parent in graph

        Ancestry ancestry = new Ancestry(fatherDog, motherDog);
        dog.setAncestry(ancestry);

        return dog;
    }

    private Dog getParentDog(Node parent) {
        if (parent == null) {
            return null;
        }
        return recursiveGetDog(parent);
    }

    private Dog getInvalidAncestorDog(Node parent) {
        if (parent == null) {
            return null;
        }

        String uuid = (String) parent.getProperty("uuid");
        String name = (String) parent.getProperty("name");
        Relationship breedRelation = parent.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty("breed");
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);

        dog.setOwnAncestor(true); // makes an invalid ancestor

        return dog;
    }

    private Node findDog(String uuid) {
        ResourceIterable<Node> dogIterator = graphDb.findNodesByLabelAndProperty(DogGraphLabel.DOG, "uuid", uuid);
        try (ResourceIterator<Node> iterator = dogIterator.iterator()) {
            if (iterator.hasNext()) {
                // found the dog
                return iterator.next();
            }
        }
        return null;
    }
}
