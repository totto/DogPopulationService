package no.nkk.dogpopulation.graph;

import org.joda.time.LocalDate;
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
    private final GraphQueryService graphQueryService;

    private final Object dogLock = new Object();
    private final Object litterLock = new Object();


    public GraphAdminService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        graphQueryService = new GraphQueryService(graphDb);
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


    public Node addDog(String uuid, String regNo, String name, String breed) {
        return addDog(uuid, regNo, name, null, breed, null, null, null);
    }


    /**
     * Add a dog the graph. If a dog with the provided UUID already exists in the graph then no new node is created,
     * and the existing node is updated with the correct name and breed if applicable.
     *
     * @param uuid
     * @param regNo
     * @param name
     * @param breedId
     * @param breed
     * @param bornLocalDate
     * @return the node in the graph with the provided UUID.
     */
    public Node addDog(String uuid, String regNo, String name, String breedId, String breed, LocalDate bornLocalDate, String hdDiag, LocalDate hdXray) {
        synchronized(dogLock) {
            try (Transaction tx = graphDb.beginTx()) {
                Node dogNode = createDogNode(uuid, name, regNo, bornLocalDate, hdDiag, hdXray);
                connectToBreed(dogNode, breedId, breed);
                tx.success();
                return dogNode;
            }
        }
    }


    public Node addLitter(String litterId, LocalDate litterBorn, int count) {
        synchronized(litterLock) {
            try (Transaction tx = graphDb.beginTx()) {
                Node litterNode = findOrCreateLitterNode(litterId);
                litterNode.setProperty(DogGraphConstants.LITTER_COUNT, count);
                litterNode.setProperty(DogGraphConstants.LITTER_YEAR, litterBorn.getYear());
                litterNode.setProperty(DogGraphConstants.LITTER_MONTH, litterBorn.getMonthOfYear());
                litterNode.setProperty(DogGraphConstants.LITTER_DAY, litterBorn.getDayOfMonth());
                tx.success();
                return litterNode;
            }
        }
    }


    public void connectDogAsParentOfLitter(ParentRole parentRole, Node parent, Node litter) {
        try (Transaction tx = graphDb.beginTx()) {
            for (Relationship existingHasLitter : parent.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
                Node existingLitter = existingHasLitter.getEndNode();
                if (existingLitter.equals(litter)) {
                    // already connected to correct litter
                    if (parentRole != null) {
                        if (existingHasLitter.hasProperty(DogGraphConstants.HASLITTER_ROLE)) {
                            ParentRole existingRole = ParentRole.valueOf(((String) existingHasLitter.getProperty(DogGraphConstants.HASLITTER_ROLE)).toUpperCase());
                            if (!existingRole.equals(parentRole)) {
                                String parentUuid = (String) parent.getProperty(DogGraphConstants.DOG_UUID);
                                LOGGER.warn("Inconsistent HAS_LITTER role. Dog {} has now changed role to {}", parentUuid, parentRole.name().toLowerCase());
                                existingHasLitter.setProperty(DogGraphConstants.HASLITTER_ROLE, parentRole.name().toLowerCase());
                            }
                        } else {
                            // role was missing before
                            existingHasLitter.setProperty(DogGraphConstants.HASLITTER_ROLE, parentRole.name().toLowerCase());
                        }
                    }
                    tx.success();
                    return;
                }
            }
            Relationship hasLitter = parent.createRelationshipTo(litter, DogGraphRelationshipType.HAS_LITTER);
            if (parentRole != null) {
                hasLitter.setProperty(DogGraphConstants.HASLITTER_ROLE, parentRole.name().toLowerCase());
            }
            tx.success();
        }
    }


    public void addPuppyToLitter(Node puppy, String litterId) {
        try (Transaction tx = graphDb.beginTx()) {
            Node litter = findOrCreateLitterNode(litterId);
            for (Relationship existingInLitter : puppy.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.IN_LITTER)) {
                Node existingLitter = existingInLitter.getEndNode();
                if (!existingLitter.equals(litter)) {
                    String puppyUuid = (String) puppy.getProperty(DogGraphConstants.DOG_UUID);
                    String existingLitterId = (String) existingLitter.getProperty(DogGraphConstants.LITTER_ID);
                    LOGGER.warn("LITTER CONFLICT: Dog {} is already in litter {} but will now be moved to litter {}.", puppyUuid, existingLitterId, litterId);
                    existingInLitter.delete();
                } else {
                    tx.success();
                    return; // already connected to correct litter
                }
            }
            puppy.createRelationshipTo(litter, DogGraphRelationshipType.IN_LITTER);
            tx.success();
        }
    }


    public Relationship connectChildToParent(String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        LOGGER.trace("Connected DOG with {}: child:{}, parent:{}", parentRole.name(), childUuid, parentUuid);
        try (Transaction tx = graphDb.beginTx()) {
            Relationship relationship = connectChildToParent(DogGraphRelationshipType.HAS_PARENT, childUuid, parentUuid, parentRole);
            tx.success();
            return relationship;
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
    public Relationship connectChildToParent(Node child, String childUuid, Node parent, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        LOGGER.trace("Connected DOG with {}: child:{}, parent:{}", parentRole.name(), childUuid, parentUuid);
        try (Transaction tx = graphDb.beginTx()) {
            Relationship relationship = connectChildToParent(DogGraphRelationshipType.HAS_PARENT, childUuid, parentUuid, parentRole);
            tx.success();
            return relationship;
        }
    }

    public Relationship connectChildAsOwnAncestor(String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        LOGGER.trace("Connected DOG with _INVALID_ {}: child:{}, parent:{}", parentRole.name(), childUuid, parentUuid);
        try (Transaction tx = graphDb.beginTx()) {
            Relationship relationship = connectChildToParent(DogGraphRelationshipType.OWN_ANCESTOR, childUuid, parentUuid, parentRole);
            tx.success();
            return relationship;
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
    public Relationship connectChildAsOwnAncestor(Node child, String childUuid, Node parent, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
        LOGGER.trace("Connected DOG with _INVALID_ {}: child:{}, parent:{}", parentRole.name(), childUuid, parentUuid);
        try (Transaction tx = graphDb.beginTx()) {
            Relationship relationship = connectChildToParent(DogGraphRelationshipType.OWN_ANCESTOR, childUuid, parentUuid, parentRole);
            tx.success();
            return relationship;
        }
    }


    private Relationship connectChildToParent(RelationshipType relationshipType, String childUuid, String parentUuid, ParentRole parentRole) throws DogUuidUnknownException {
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
        return relationship;
    }


    private Node findOrCreateBreedCategoryNode() {
        Node breed = findOrCreateNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
        if (!breed.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breed.createRelationshipTo(findOrCreateRootNode(), DogGraphRelationshipType.MEMBER_OF);
        }
        return breed;
    }


    private Node findOrCreateBreedNode(String breedId, String breed) {
        Node breedNode = findOrCreateNode(DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);
        if (breedId != null) {
            if (breedNode.hasProperty(DogGraphConstants.BREED_ID)) {
                String existingBreedId = (String) breedNode.getProperty(DogGraphConstants.BREED_ID);
                if (!existingBreedId.equals(breedId)) {
                    LOGGER.warn("Breed-ID conflict: ID \"{}\" (in graph) and \"{}\" are both assigned to breed \"{}\"", existingBreedId, breedId, breed);
                }
            } else {
                LOGGER.trace("Added breed.id={} to \"{}\"", breedId, breed);
                breedNode.setProperty(DogGraphConstants.BREED_ID, breedId);
            }
        }
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


    /**
     * Creates a new dog node in the graph. If a node already exists with the given uuid, then no changes are performed
     * on the graph by this method.
     *
     * @param uuid
     * @param name
     * @param regNo
     * @param bornLocalDate
     * @param hdDiag
     * @param hdXray
     * @return the node of the dog with the uuid
     */
    private Node createDogNode(String uuid, String name, String regNo, LocalDate bornLocalDate, String hdDiag, LocalDate hdXray) {

        Node dogNode = findOrCreateNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);

        if (dogNode.hasProperty(DogGraphConstants.DOG_NAME)) {
            LOGGER.trace("DOG already exists, properties will not be updated");
            return dogNode;
        }

        // new dog

        dogNode.setProperty(DogGraphConstants.DOG_NAME, name);
        if (regNo != null) {
            dogNode.setProperty(DogGraphConstants.DOG_REGNO, regNo);
        }
        if (bornLocalDate != null) {
            dogNode.setProperty(DogGraphConstants.DOG_BORN_YEAR, bornLocalDate.getYear());
            dogNode.setProperty(DogGraphConstants.DOG_BORN_MONTH, bornLocalDate.getMonthOfYear());
            dogNode.setProperty(DogGraphConstants.DOG_BORN_DAY, bornLocalDate.getDayOfMonth());
        }
        if (hdDiag != null) {
            dogNode.setProperty(DogGraphConstants.DOG_HDDIAG, hdDiag);
        }
        if (hdXray != null) {
            dogNode.setProperty(DogGraphConstants.DOG_HDYEAR, hdXray.getYear());
        }

        LOGGER.trace("Added DOG to graph {}", uuid);

        return dogNode;
    }


    private Relationship connectToBreed(Node dogNode, String breedId, String breed) {

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
        Node breedNode = findOrCreateBreedNode(breedId, breed);
        Relationship relationship = dogNode.createRelationshipTo(breedNode, DogGraphRelationshipType.IS_BREED);
        return relationship;
    }


    private Node findOrCreateLitterNode(String litterId) {
        Node litter = graphQueryService.getSingleNode(DogGraphLabel.LITTER, DogGraphConstants.LITTER_ID, litterId);
        if (litter != null) {
            return litter;
        }
        litter = graphDb.createNode(DogGraphLabel.LITTER);
        litter.setProperty(DogGraphConstants.LITTER_ID, litterId);
        return litter;
    }

}
