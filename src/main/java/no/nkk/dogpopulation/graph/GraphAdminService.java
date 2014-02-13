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
        return addDog(uuid, null, name, breed);
    }


    /**
     * Add a dog the graph. If a dog with the provided UUID already exists in the graph then no new node is created,
     * and the existing node is updated with the correct name and breed if applicable.
     *
     * @param uuid
     * @param regNo
     * @param name
     * @param breed
     * @return the node in the graph with the provided UUID.
     */
    public Node addDog(String uuid, String regNo, String name, String breed) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dogNode = createDogNode(uuid, name, regNo);
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

    private Relationship connectChildToParent(RelationshipType relationshipType, String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        try (Transaction tx = graphDb.beginTx()) {
            Node child = findDog(childUuid);
            if (child == null) {
                throw new DogUuidUnknownException("Dog (child) with uuid " + childUuid + " does not exist in graph.");
            }
            Iterable<Relationship> parentRelationshipIterator = child.getRelationships(relationshipType, Direction.OUTGOING);
            for (Relationship relationship : parentRelationshipIterator) {
                // iterate through parents already known by graph
                ParentRole existingParentRole = ParentRole.valueOf(((String) relationship.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
                if (parentRole.equals(existingParentRole)) {
                    Node existingParent = relationship.getEndNode();
                    if (existingParent.getProperty(DogGraphConstants.DOG_UUID).equals(parentUuid)) {
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
            relationship.setProperty(DogGraphConstants.HASPARENT_ROLE, parentRole.name().toLowerCase());
            tx.success();
            return relationship;
        }
    }


    private Node findOrCreateBreedCategoryNode() {
        Node breed = findOrCreateNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
        if (!breed.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breed.createRelationshipTo(findOrCreateRootNode(), DogGraphRelationshipType.MEMBER_OF);
        }
        return breed;
    }


    private Node findOrCreateBreedNode(String breed) {
        Node breedNode = findOrCreateNode(DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);
        if (!breedNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breedNode.createRelationshipTo(findOrCreateBreedCategoryNode(), DogGraphRelationshipType.MEMBER_OF);
        }
        return breedNode;
    }


    private Node findOrCreateRootNode() {
        return findOrCreateNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_ROOT);
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


    private Node findOrCreateNode(Label label, String propertyKey, String propertyValue) {
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


    private Node createDogNode(String uuid, String name, String regNo) {

        Node dogNode = findOrCreateNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);

        if (!dogNode.hasProperty(DogGraphConstants.DOG_NAME)) {
            // new dog
            dogNode.setProperty(DogGraphConstants.DOG_NAME, name);
            if (regNo != null) {
                dogNode.setProperty(DogGraphConstants.DOG_REGNO, regNo);
            }
            LOGGER.trace("Added DOG to graph {}", uuid);
            return dogNode;

        }

        // existing dog

        String existingName = (String) dogNode.getProperty(DogGraphConstants.DOG_NAME);
        if (!existingName.equals(name)) {
            LOGGER.warn("NAME of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingName, name);
            dogNode.setProperty(DogGraphConstants.DOG_NAME, name);
        }

        if (regNo != null) {
            String existingRegNo = (String) dogNode.getProperty(DogGraphConstants.DOG_REGNO);
            if (existingRegNo == null || !existingName.equals(regNo)) {
                LOGGER.warn("NAME of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingName, name);
                dogNode.setProperty(DogGraphConstants.DOG_REGNO, regNo);
            }
        }

        return dogNode;
    }


    private Relationship connectToBreed(Node dogNode, String breed) {

        if (dogNode.hasRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING)) {
            // relationship already exists
            Relationship relationship = dogNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
            Node breedNode = relationship.getEndNode();
            String existingBreed = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
            if (existingBreed.equals(breed)) {
                return relationship; // breed is correct - do nothing
            }

            String uuid = (String) dogNode.getProperty(DogGraphConstants.DOG_UUID);
            LOGGER.warn("BREED of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingBreed, breed);
            relationship.delete();
        }

        // establish new relationship to breed
        Node breedNode = findOrCreateBreedNode(breed);
        Relationship relationship = dogNode.createRelationshipTo(breedNode, DogGraphRelationshipType.IS_BREED);
        return relationship;
    }

}
