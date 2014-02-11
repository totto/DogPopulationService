package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.*;
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


    public void populateDescendantUuids(String uuid, Set<String> descendants) {
        try (Transaction tx = graphDb.beginTx()) {
            ResourceIterable<Node> iterator = graphDb.findNodesByLabelAndProperty(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
            Node node = iterator.iterator().next();
            recursivePopulateDescendantUuids(node, descendants);
        }
    }

    private void recursivePopulateDescendantUuids(Node dog, Collection<? super String> descendants) {
        for (Relationship relationship : dog.getRelationships(Direction.INCOMING, DogGraphRelationshipType.HAS_PARENT)) {
            Node descendant = relationship.getStartNode();
            String uuid = (String) descendant.getProperty(DogGraphConstants.DOG_UUID);
            if (descendants.contains(uuid)) {
                return; // more than one path to descendant, this is because of inbreeding
            }
            descendants.add(uuid);
            recursivePopulateDescendantUuids(descendant, descendants);
        }
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
        String uuid = (String) node.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) node.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);

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

    public List<String> getBreedList(String breed) {
        try (Transaction tx = graphDb.beginTx()) {

            ResourceIterable<Node> breedNodeIterator = graphDb.findNodesByLabelAndProperty(DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);

            Node breedRoot;
            try (ResourceIterator<Node> iterator = breedNodeIterator.iterator()) {
                if (iterator.hasNext()) {
                    breedRoot = iterator.next();
                } else {
                    return Collections.emptyList();
                }
            }

            Iterable<Relationship> relationships = breedRoot.getRelationships(Direction.INCOMING, DogGraphRelationshipType.IS_BREED);
            List<String> dogIds = new ArrayList<>(1000);
            for (Relationship relationship : relationships) {
                Node dogNode = relationship.getStartNode();
                dogIds.add((String) dogNode.getProperty(DogGraphConstants.DOG_UUID));
            }

            tx.success();
            return dogIds;
        }
    }
}
