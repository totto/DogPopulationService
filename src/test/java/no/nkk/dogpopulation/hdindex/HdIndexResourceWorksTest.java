package no.nkk.dogpopulation.hdindex;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.AbstractResourceTest;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HdIndexResourceWorksTest extends AbstractResourceTest {

    @Test
    public void thatCreatingHdFilesForOneDogWithBreedCodeWorks() throws Exception {
        String breed = "Rottweiler";
        String uuid = "uuid-1234567890";
        String name = "Wicked teeth Jr. III";
        Node breedSynonymNode = breedSynonymNodeCache.getBreed(breed);
        addDog(uuid, name, breedSynonymNode);
        try (Transaction tx = graphDb.beginTx()) {
            Node breedNode = graphDb.createNode(DogGraphLabel.BREED);
            breedNode.setProperty(DogGraphConstants.BREED_BREED_NAME, breed);
            breedNode.setProperty(DogGraphConstants.BREED_NKK_BREED_ID, "234");
            breedSynonymNode.createRelationshipTo(breedNode, DogGraphRelationshipType.MEMBER_OF);
            tx.success();
        }

        RestAssured.expect().statusCode(200).given().when().param("breed", "Rottweiler").get("/dogpopulation/hdindex/Rottweiler_54321/pedigree");
    }
}
