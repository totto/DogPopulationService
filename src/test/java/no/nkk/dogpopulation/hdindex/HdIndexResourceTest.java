package no.nkk.dogpopulation.hdindex;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.AbstractResourceTest;
import org.neo4j.graphdb.Node;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HdIndexResourceTest extends AbstractResourceTest {

    @Test
    public void thatCreatingHdFilesWithMissingBreedCodeDoesNotWork() throws Exception {
        String breed = "Rottweiler";
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr. III";
        Node breedNode = commonNodes.getBreed(breed, null);
        addDog(uuid, name, breedNode);

        RestAssured.expect().statusCode(500).given().when().param("breed", "Rottweiler").get("/dogpopulation/hdindex/Rottweiler_NoRaceCode/pedigree");
    }

    // TODO For now only one test can be run, more than one test will fail - debug the TestNG BeforeClass annotated method in the super-class of this class.
    /*
    @Test
    public void thatCreatingHdFilesForOneDogWithBreedCodeWorks() throws Exception {
        String breed = "Rottweiler";
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr. III";
        Node breedNode = commonNodes.getBreed(breed, "54321");
        addDog(uuid, name, breedNode);

        RestAssured.expect().statusCode(200).given().when().param("breed", "Rottweiler").get("/dogpopulation/hdindex/Rottweiler_54321/pedigree");
    }
    */
}
