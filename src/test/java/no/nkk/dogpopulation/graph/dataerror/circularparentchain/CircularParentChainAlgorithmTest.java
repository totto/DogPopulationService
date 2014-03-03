package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import no.nkk.dogpopulation.AbstractGraphTest;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CircularParentChainAlgorithmTest extends AbstractGraphTest {

    @Test
    public void thatSimpleCircleCanBeDetected() {
        Node breedNode = breed("Unit-test Breed", "1");
        Node A = addDog("A", breedNode);
        addDog("B", breedNode);
        addDog("C", breedNode);
        connectChildToFather("A", "B");
        connectChildToFather("B", "C");
        connectChildToFather("C", "A");

        CircularParentChainAlgorithm algorithm = new CircularParentChainAlgorithm(graphDb);

        List<Relationship> circle = algorithm.run("A");
        Assert.assertNotNull(circle);
        Assert.assertEquals(circle.size(), 3);
        List<Relationship> circle2 = algorithm.run("B");
        Assert.assertNotNull(circle2);
        Assert.assertEquals(circle2.size(), 3);
        List<Relationship> circle3 = algorithm.run("C");
        Assert.assertNotNull(circle3);
        Assert.assertEquals(circle3.size(), 3);
    }

    @Test
    public void thatCorrectPedigreeWontBedetectedAsHavingCircle() {
        Node breedNode = breed("Unit-test Breed", "1");
        Node A = addDog("A", breedNode);
        addDog("B", breedNode);
        addDog("C", breedNode);
        connectChildToFather("A", "B");
        connectChildToFather("B", "C");

        CircularParentChainAlgorithm algorithm = new CircularParentChainAlgorithm(graphDb);

        List<Relationship> circle = algorithm.run("A");
        Assert.assertNull(circle);
        List<Relationship> circle2 = algorithm.run("B");
        Assert.assertNull(circle2);
        List<Relationship> circle3 = algorithm.run("C");
        Assert.assertNull(circle3);
    }
}
