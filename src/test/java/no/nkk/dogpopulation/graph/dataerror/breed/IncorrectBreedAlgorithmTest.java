package no.nkk.dogpopulation.graph.dataerror.breed;

import no.nkk.dogpopulation.AbstractGraphTest;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class IncorrectBreedAlgorithmTest  extends AbstractGraphTest {

    @Test(groups = "fast")
    public void thatDobermannChildOfTwoDalmatinersIsConsideredIncorrectBreed() {
        Node dalmatiner = addBreed("Dalmatiner", "123");
        Node dobermann = addBreed("Dobermann", "456");
        addDog("A", dobermann);
        addDog("B", dalmatiner);
        addDog("C", dalmatiner);
        connectChildToFather("A", "B");
        connectChildToMother("A", "C");

        List<String> result = graphQueryService.getAllDogsWithInconsistentBreed(0, 10, "Dobermann");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);

        IncorrectBreedRecord igr = graphQueryService.getDogWithInconsistentBreed("A");
        Assert.assertNotNull(igr);
        Assert.assertEquals(igr.getUuid(), "A");
    }

    @Test(groups = "fast")
    public void thatDobermannChildOfTwoDalmatinerSynonymsIsConsideredIncorrectBreed() {
        Node dalmatiner1 = addBreed("Dalmatiner", "123");
        Node dalmatiner2 = addBreed("dalmatiner", "123");
        Node dobermann = addBreed("Dobermann", "456");
        addDog("A", dobermann);
        addDog("B", dalmatiner1);
        addDog("C", dalmatiner2);
        connectChildToFather("A", "B");
        connectChildToMother("A", "C");

        List<String> result = graphQueryService.getAllDogsWithInconsistentBreed(0, 10, "Dobermann");
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 1);

        IncorrectBreedRecord igr = graphQueryService.getDogWithInconsistentBreed("A");
        Assert.assertNotNull(igr);
        Assert.assertEquals(igr.getUuid(), "A");
    }
}
