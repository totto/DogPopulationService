package no.nkk.dogpopulation.hdindex;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.AbstractResourceTest;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HdIndexResourceTest extends AbstractResourceTest {

    @BeforeClass
    public void startServer() {
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

        Node breedNode = commonNodes.getBreed(breed, null);
        addDog(childUuid, childName, breedNode);
        addDog(fatherUuid, fatherName, breedNode);
        connectChildToFather(childUuid, fatherUuid);
        addDog(fathersFatherUuid, fathersFatherName, breedNode);
        connectChildToFather(fatherUuid, fathersFatherUuid);
        addDog(fathersMotherUuid, fathersMotherName, breedNode);
        connectChildToMother(fatherUuid, fathersMotherUuid);
        addDog(motherUuid, motherName, breedNode);
        connectChildToMother(childUuid, motherUuid);
    }

    @Test
    public void thatPedigreeIsWellFormed() throws Exception {
        TopLevelDog dog = RestAssured.expect().statusCode(200).given().when().get("/dogpopulation/pedigree/uuid-1234567891").as(TopLevelDog.class);
        String dogName = dog.getName();
        Assert.assertEquals(dogName, "Wicked teeth Sr. II");
    }
}
