package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.Main;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Iterator;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphAdminServiceTest {

    GraphDatabaseService graphDb;

    @BeforeMethod
    public void initGraph() {
        String dbPath = "target/unittestdogdb";
        File dbFolder = new File(dbPath);
        FileUtils.deleteQuietly(dbFolder);
        graphDb = Main.createGraphDb(dbPath);
    }

    @AfterMethod
    public void closeGraph() {
        graphDb.shutdown();
    }

    @Test
    public void thatAddDogOnEmptyGraphCreatesCorrectLineageToRoot() throws InterruptedException {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr.";
        String breed = "Rottweiler";

        // when
        graphAdminService.addDog(uuid, name, breed);

        // then
        validateCorrectBreedLineage(uuid, name, breed);
    }

    @Test
    public void thatAddDogTwiceDoesNotGenerateAdditionalNodes() throws InterruptedException {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr.";
        String breed = "Rottweiler";

        // when
        Node testDog = graphAdminService.addDog(uuid, name, breed);
        Node testDogDuplicate = graphAdminService.addDog(uuid, name, breed);

        // then
        Assert.assertEquals(testDogDuplicate, testDog);
        validateCorrectBreedLineage(uuid, name, breed);
    }

    @Test
    public void thatAddDogAndConnectParentGeneratesCorrectPedigree() throws InterruptedException {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        String breed = "Rottweiler";
        String childUuid = "uuid-1234567890";
        String childName = "Wicked teeth Jr. III";
        String fatherUuid = "uuid-1234567891";
        String fatherName = "Wicked teeth Sr. II";
        String fathersFatherUuid = "uuid-1234567892";
        String fathersFatherName = "Wicked teeth I";
        String fathersMotherUuid = "uuid-1234567893";
        String fathersMotherName = "Daisy";
        String motherUuid = "uuid-1234567894";
        String motherName = "Tigerclaws";

        // when
        graphAdminService.addDog(childUuid, childName, breed);
        graphAdminService.addDog(fatherUuid, fatherName, breed);
        graphAdminService.connectChildToParent(childUuid, fatherUuid, ParentRole.FATHER);
        graphAdminService.addDog(fathersFatherUuid, fathersFatherName, breed);
        graphAdminService.connectChildToParent(fatherUuid, fathersFatherUuid, ParentRole.FATHER);
        graphAdminService.addDog(fathersMotherUuid, fathersMotherName, breed);
        graphAdminService.connectChildToParent(fatherUuid, fathersMotherUuid, ParentRole.MOTHER);
        graphAdminService.addDog(motherUuid, motherName, breed);
        graphAdminService.connectChildToParent(childUuid, motherUuid, ParentRole.MOTHER);

        // then
        validateCorrectBreedLineage(childUuid, childName, breed);
        validateCorrectBreedLineage(fatherUuid, fatherName, breed);
        validateCorrectBreedLineage(fathersFatherUuid, fathersFatherName, breed);
        validateCorrectBreedLineage(fathersMotherUuid, fathersMotherName, breed);
        validateCorrectBreedLineage(motherUuid, motherName, breed);
        validateParentRelations(childUuid, fatherUuid, motherUuid);
        validateParentRelations(fatherUuid, fathersFatherUuid, fathersMotherUuid);
    }

    private void validateParentRelations(String childUuid, String fatherUuid, String motherUuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = findDog(childUuid);
            Iterable<Relationship> childNodeRelationships = dog.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
            Iterator<Relationship> iterator = childNodeRelationships.iterator();
            Relationship childParentRelation1 = iterator.next();
            Relationship childParentRelation2 = iterator.next();
            Assert.assertFalse(iterator.hasNext()); // no more than two parents
            ParentRole parent1Role = ParentRole.valueOf(((String) childParentRelation1.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            ParentRole parent2Role = ParentRole.valueOf(((String) childParentRelation2.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            Assert.assertNotEquals(parent1Role, parent2Role); // not two fathers or two mothers
            Relationship fatherRelation;
            Relationship motherRelation;
            if (parent1Role == ParentRole.FATHER) {
                fatherRelation = childParentRelation1;
                motherRelation = childParentRelation2;
            } else {
                fatherRelation = childParentRelation2;
                motherRelation = childParentRelation1;
            }
            Node fatherNode = fatherRelation.getEndNode();
            Assert.assertEquals((String) fatherNode.getProperty(DogGraphConstants.DOG_UUID), fatherUuid); // correct father
            Node motherNode = motherRelation.getEndNode();
            Assert.assertEquals((String) motherNode.getProperty(DogGraphConstants.DOG_UUID), motherUuid); // correct mother
        }
    }

    private void validateCorrectBreedLineage(String uuid, String name, String breed) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dogNode = findDog(uuid);
            Assert.assertEquals((String) dogNode.getProperty(DogGraphConstants.DOG_UUID), uuid);
            Assert.assertEquals((String) dogNode.getProperty(DogGraphConstants.DOG_NAME), name);
            Relationship dogToBreedRelation = dogNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
            Node breedNode = dogToBreedRelation.getEndNode();
            Assert.assertEquals((String) breedNode.getProperty(DogGraphConstants.BREED_BREED), breed);
            Relationship breedToCategoryRelation = breedNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            Node breedCategoryNode = breedToCategoryRelation.getEndNode();
            Relationship breedToRootRelation = breedCategoryNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            Node rootNode = breedToRootRelation.getEndNode();
            String category = (String) rootNode.getProperty(DogGraphConstants.CATEGORY_CATEGORY);
            Assert.assertEquals(category, DogGraphConstants.CATEGORY_CATEGORY_ROOT);
            tx.success();
        }
    }

    private Node findDog(String uuid) {
        ResourceIterable<Node> nodeIterator = graphDb.findNodesByLabelAndProperty(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
        ResourceIterator<Node> iterator = nodeIterator.iterator();
        Node dogNode = iterator.next(); // exception thrown if dog with UUID does not exist
        Assert.assertFalse(iterator.hasNext()); // only one dog with given UUID
        return dogNode;
    }

}
