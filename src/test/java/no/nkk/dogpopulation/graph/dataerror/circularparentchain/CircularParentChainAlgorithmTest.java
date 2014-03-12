package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CircularParentChainAlgorithmTest extends AbstractGraphTest {

    @Test
    public void thatSimpleCircleCanBeDetected() {
        Node breedNode = breed("Unit-test Breed");
        addDog("A", breedNode, new LocalDate(2007, 6, 17));
        addDog("B", breedNode);
        addDog("C", breedNode, new LocalDate(2002, 10, 21));
        connectChildToFather("A", "B");
        connectChildToFather("B", "C");
        connectChildToFather("C", "A");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        List<CircularRecord> circle = graphQueryService.getCircluarParentChainInAncestryOf("A");
        Assert.assertNotNull(circle);
        Assert.assertEquals(circle.size(), 3);
        List<CircularRecord> circle2 = graphQueryService.getCircluarParentChainInAncestryOf("B");
        Assert.assertNotNull(circle2);
        Assert.assertEquals(circle2.size(), 3);
        List<CircularRecord> circle3 = graphQueryService.getCircluarParentChainInAncestryOf("C");
        Assert.assertNotNull(circle3);
        Assert.assertEquals(circle3.size(), 3);
    }

    @Test
    public void thatCircleInAncestryNotIncludingDogItselfCanBeDetected() {
        Node breedNode = breed("Unit-test Breed");
        addDog("A", breedNode, new LocalDate(2007, 6, 17));
        addDog("B", breedNode, new LocalDate(2004, 2, 2));
        addDog("C", breedNode, new LocalDate(2002, 10, 21));
        addDog("D", breedNode, new LocalDate(1998, 4, 13));
        connectChildToFather("A", "B");
        connectChildToFather("B", "C");
        connectChildToFather("C", "D");
        connectChildToFather("D", "B");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        List<CircularRecord> circle = graphQueryService.getCircluarParentChainInAncestryOf("A");
        Assert.assertNotNull(circle);
        Assert.assertEquals(circle.size(), 3);
    }

    @Test
    public void thatCorrectPedigreeWontBedetectedAsHavingCircle() {
        Node breedNode = breed("Unit-test Breed");
        addDog("A", breedNode, new LocalDate(2007, 6, 17));
        addDog("B", breedNode, new LocalDate(2004, 2, 2));
        addDog("C", breedNode, new LocalDate(2002, 10, 21));
        connectChildToFather("A", "B");
        connectChildToFather("B", "C");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        List<CircularRecord> circle = graphQueryService.getCircluarParentChainInAncestryOf("A");
        Assert.assertNull(circle);
        List<CircularRecord> circle2 = graphQueryService.getCircluarParentChainInAncestryOf("B");
        Assert.assertNull(circle2);
        List<CircularRecord> circle3 = graphQueryService.getCircluarParentChainInAncestryOf("C");
        Assert.assertNull(circle3);
    }
}
