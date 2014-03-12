package no.nkk.dogpopulation.graph.dataerror.gender;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.DogGender;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class IncorrectOrMissingGenderAlgorithmTest extends AbstractGraphTest {

    @Test
    public void thatFemaleFatherIsIllegal() {
        Node breedNode = breed("Unit-test Breed");
        Node A = addDog("A", breedNode);
        setProperty(A, DogGraphConstants.DOG_GENDER, DogGender.FEMALE.name().toLowerCase());
        Node B = addDog("B", breedNode);
        setProperty(B, DogGraphConstants.DOG_GENDER, DogGender.MALE.name().toLowerCase());
        Node C = addDog("C", breedNode);
        setProperty(C, DogGraphConstants.DOG_GENDER, DogGender.FEMALE.name().toLowerCase());
        connectChildToFather("A", "B");
        connectChildToFather("A", "C");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        List<String> result = graphQueryService.getAllDogsWithInconsistentGender(0, 10);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);

        IncorrectGenderRecord igr = graphQueryService.getDogWithInconsistentGender("C");
        Assert.assertNotNull(igr);
        Assert.assertEquals(igr.getUuid(), "C");
    }

}
