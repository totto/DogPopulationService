package no.nkk.dogpopulation.graph.dataerror.breed;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class IncorrectBreedAlgorithmTest  extends AbstractGraphTest {

    @Test
    public void thatDobermannChildOfTwoDalmatinersIsConsideredIncorrectBreed() {
        Node dalmatiner = breed("Dalmatiner");
        Node dobermann = breed("Dobermann");
        addDog("A", dobermann);
        addDog("B", dalmatiner);
        addDog("C", dalmatiner);
        connectChildToFather("A", "B");
        connectChildToMother("A", "C");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        List<String> result = graphQueryService.getAllDogsWithInconsistentBreed(0, 10);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);

        IncorrectBreedRecord igr = graphQueryService.getDogWithInconsistentBreed("A");
        Assert.assertNotNull(igr);
        Assert.assertEquals(igr.getUuid(), "A");
    }

}
