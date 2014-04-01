package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.ParentRole;
import org.neo4j.graphdb.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Iterator;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeBuilderTest extends AbstractGraphTest {

    @Test(groups = "fast")
    public void thatAddDogOnEmptyGraphCreatesCorrectLineageToRoot() throws InterruptedException {
        // given
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr.";
        String breed = "Rottweiler";

        // when

        addDog(uuid, name, breed(breed));

        // then
        validateCorrectBreedLineage(uuid, name, breed);
    }

    @Test(groups = "fast")
    public void thatAddDogTwiceDoesNotGenerateAdditionalNodes() throws InterruptedException {
        // given
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr.";
        String breed = "Rottweiler";

        // when
        Node breedNode = breed(breed);
        Node testDog = addDog(uuid, name, breedNode);
        Node testDogDuplicate = addDog(uuid, name, breedNode);

        // then
        Assert.assertEquals(testDogDuplicate, testDog);
        validateCorrectBreedLineage(uuid, name, breed);
    }

    @Test(groups = "fast")
    public void thatAddDogAndConnectParentGeneratesCorrectPedigree() throws InterruptedException {
        // given
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
        Node breedNode = breed(breed);
        addDog(childUuid, childName, breedNode);
        addDog(fatherUuid, fatherName, breedNode);
        connectChildToFather(childUuid, fatherUuid);
        addDog(fathersFatherUuid, fathersFatherName, breedNode);
        connectChildToFather(fatherUuid, fathersFatherUuid);
        addDog(fathersMotherUuid, fathersMotherName, breedNode);
        connectChildToMother(fatherUuid, fathersMotherUuid);
        addDog(motherUuid, motherName, breedNode);
        connectChildToMother(childUuid, motherUuid);

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
            Assert.assertEquals((String) breedNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM), breed);
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
