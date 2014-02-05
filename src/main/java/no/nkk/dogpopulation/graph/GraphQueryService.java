package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphQueryService {

    private final GraphDatabaseService graphDb;

    public GraphQueryService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public Dog getPedigree(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Dog dog = recursiveGetDog(uuid);
            tx.success();
            return dog;
        }
    }

    private Dog recursiveGetDog(String uuid) {
        Node node = findDog(uuid);
        if (node == null) {
            return null; // dog not found
        }

        String name = (String) node.getProperty("name");
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty("breed");
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);

        Node fatherNode = null;
        Node motherNode = null;
        Iterable<Relationship> parentRelationIterable = node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
        for (Relationship parentRelation : parentRelationIterable) {
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty("role")).toUpperCase());
            switch(parentRole) {
                case FATHER:
                    fatherNode = parentRelation.getEndNode();
                    break;
                case MOTHER:
                    motherNode = parentRelation.getEndNode();
                    break;
            }
        }

        if (fatherNode == null && motherNode == null) {
            return dog; // no known parents, do not create ancestry
        }

        // at least one known parent in graph

        Dog fatherDog = getParentDog(fatherNode);
        Dog motherDog = getParentDog(motherNode);

        Ancestry ancestry = new Ancestry(fatherDog, motherDog);
        dog.setAncestry(ancestry);

        return dog;
    }

    private Dog getParentDog(Node fatherNode) {
        if (fatherNode == null) {
            return null;
        }
        String fatherUuid = (String) fatherNode.getProperty("uuid");
        return recursiveGetDog(fatherUuid);
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
