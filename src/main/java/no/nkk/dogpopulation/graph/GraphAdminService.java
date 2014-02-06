package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide methods to add dogs and dog family-relationships to the graph. These methods have REDO semantics,
 * which means that they are safe to run again and again without risking duplicated or lost data.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphAdminService {


    private static final Logger LOGGER = LoggerFactory.getLogger(GraphAdminService.class);


    private final GraphDatabaseService graphDb;


    public GraphAdminService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    /**
     * Add a dog the graph. If a dog with the provided UUID already exists in the graph then no new node is created,
     * and the existing node is updated with the correct name and breed if applicable.
     *
     * @param uuid
     * @param name
     * @param breed
     * @return the node in the graph with the provided UUID.
     */
    public Node addDog(String uuid, String name, String breed) {
        LOGGER.trace("Added DOG to graph {}", uuid);
        try (Transaction tx = graphDb.beginTx()) {
            Node dogNode = createDogNode(uuid, name);
            connectToBreed(dogNode, breed);
            tx.success();
            return dogNode;
        }
    }


    /**
     * Connect a child dog with its parent in the graph. This method assumes that both the child-UUID and the
     * parent-UUID already exists in the graph, otherwise a runtime-exception is thrown.
     *
     * @param childUuid
     * @param parentUuid
     * @param parentRole
     * @return
     * @throws DogUuidUnknownException
     */
    public Relationship connectChildToParent(String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        LOGGER.trace("Connected DOG with {}: child:{}, parent:{}", parentRole.name(), childUuid, parentUuid);
        return connectChildToParent(DogGraphRelationshipType.HAS_PARENT, childUuid, parentUuid, parentRole);
    }

    /**
     * Connect a child dog with its parent in the graph. This method assumes that both the child-UUID and the
     * parent-UUID already exists in the graph, otherwise a runtime-exception is thrown.
     *
     * @param childUuid
     * @param parentUuid
     * @param parentRole
     * @return
     * @throws DogUuidUnknownException
     */
    public Relationship connectChildAsOwnAncestor(String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        LOGGER.trace("Connected DOG with _INVALID_ {}: child:{}, parent:{}", parentRole.name(), childUuid, parentUuid);
        return connectChildToParent(DogGraphRelationshipType.OWN_ANCESTOR, childUuid, parentUuid, parentRole);
    }

    private Relationship connectChildToParent(DogGraphRelationshipType relationshipType, String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        try (Transaction tx = graphDb.beginTx()) {
            Node child = findDog(childUuid);
            if (child == null) {
                throw new DogUuidUnknownException("Dog (child) with uuid " + childUuid + " does not exist in graph.");
            }
            Iterable<Relationship> parentRelationshipIterator = child.getRelationships(relationshipType, Direction.OUTGOING);
            for (Relationship relationship : parentRelationshipIterator) {
                // iterate through parents already known by graph
                ParentRole existingParentRole = ParentRole.valueOf(((String) relationship.getProperty("role")).toUpperCase());
                if (parentRole.equals(existingParentRole)) {
                    Node existingParent = relationship.getEndNode();
                    if (existingParent.getProperty("uuid").equals(parentUuid)) {
                        return relationship; // the child and parent is already connected
                    }
                    // child-parent relationship in graph is wrong
                    relationship.delete();
                    break;
                }
            }
            Node parent = findDog(parentUuid);
            if (parent == null) {
                throw new DogUuidUnknownException("Dog (parent) with uuid " + parentUuid + " does not exist in graph.");
            }
            Relationship relationship = child.createRelationshipTo(parent, relationshipType);
            relationship.setProperty("role", parentRole.name().toLowerCase());
            tx.success();
            return relationship;
        }
    }


    private Node findOrCreateBreedCategoryNode() {
        Node breed = findOrCreateNode(DogGraphLabel.CATEGORY, "category", "Breed");
        if (!breed.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breed.createRelationshipTo(findOrCreateRootNode(), DogGraphRelationshipType.MEMBER_OF);
        }
        return breed;
    }


    private Node findOrCreateBreedNode(String breed) {
        Node breedNode = findOrCreateNode(DogGraphLabel.BREED, "breed", breed);
        if (!breedNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breedNode.createRelationshipTo(findOrCreateBreedCategoryNode(), DogGraphRelationshipType.MEMBER_OF);
        }
        return breedNode;
    }


    private Node findOrCreateRootNode() {
        return findOrCreateNode(DogGraphLabel.CATEGORY, "category", "Root");
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


    private Node findOrCreateNode(DogGraphLabel label, String propertyKey, String propertyValue) {
        ResourceIterable<Node> rootIterator = graphDb.findNodesByLabelAndProperty(label, propertyKey, propertyValue);
        Node node;
        try (ResourceIterator<Node> iterator = rootIterator.iterator()) {
            if (iterator.hasNext()) {

                // node already exists

                node = iterator.next();

            } else {

                // create new node

                node = graphDb.createNode(label);
                node.setProperty(propertyKey, propertyValue);
            }
        }
        return node;
    }


    private Node createDogNode(String uuid, String name) {

        Node dogNode = findOrCreateNode(DogGraphLabel.DOG, "uuid", uuid);

        if (!dogNode.hasProperty("name")) {
            // new dog
            dogNode.setProperty("name", name);
            return dogNode;

        }

        // existing dog

        String existingName = (String) dogNode.getProperty("name");
        if (existingName.equals(name)) {
            return dogNode; // existing name in graph matches - do nothing
        }

        LOGGER.warn("NAME of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingName, name);
        dogNode.setProperty("name", name);
        return dogNode;
    }


    private Relationship connectToBreed(Node dogNode, String breed) {

        if (dogNode.hasRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING)) {
            // relationship already exists
            Relationship relationship = dogNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
            Node breedNode = relationship.getEndNode();
            String existingBreed = (String) breedNode.getProperty("breed");
            if (existingBreed.equals(breed)) {
                return relationship; // breed is correct - do nothing
            }

            String uuid = (String) dogNode.getProperty("uuid");
            LOGGER.warn("BREED of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingBreed, breed);
            relationship.delete();
        }

        // establish new relationship to breed
        Node breedNode = findOrCreateBreedNode(breed);
        Relationship relationship = dogNode.createRelationshipTo(breedNode, DogGraphRelationshipType.IS_BREED);
        return relationship;
    }

}
