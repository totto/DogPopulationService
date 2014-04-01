package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.AbstractGraphTest;
import org.joda.time.LocalDateTime;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphQueryServiceTest extends AbstractGraphTest {

    @Test(groups = "fast")
    public void thatPopulateDescendantsIncludesAllButItself() {
        // given
        Node breedNode = breed("Unit-test Breed");
        Node A = addDog("A", breedNode);
        addDog("B", breedNode);
        addDog("C", breedNode);
        addDog("D", breedNode);
        addDog("E", breedNode);
        addDog("F", breedNode);
        addDog("G", breedNode);
        addDog("H", breedNode);
        addDog("I", breedNode);
        addDog("J", breedNode);
        addDog("K", breedNode);
        addDog("L", breedNode);
        addDog("M", breedNode);
        addDog("N", breedNode);
        addDog("O", breedNode);
        connectChildToFather("B", "A");
        connectChildToFather("C", "B");
        connectChildToFather("D", "C");
        connectChildToFather("E", "C");
        connectChildToFather("F", "B");
        connectChildToMother("G", "F");
        connectChildToMother("H", "F");
        connectChildToFather("I", "A");
        connectChildToMother("J", "I");
        connectChildToFather("K", "J");
        connectChildToFather("L", "J");
        connectChildToMother("M", "I");
        connectChildToMother("N", "M");
        connectChildToMother("O", "M");

        // when
        LinkedHashSet<String> descendants = new LinkedHashSet<>();
        graphQueryService.populateDescendantUuids(A, descendants);

        // then
        Assert.assertEquals(descendants.size(), 14);
        Assert.assertFalse(descendants.contains("A"));
        Assert.assertTrue(descendants.contains("B"));
        Assert.assertTrue(descendants.contains("C"));
        Assert.assertTrue(descendants.contains("D"));
        Assert.assertTrue(descendants.contains("E"));
        Assert.assertTrue(descendants.contains("F"));
        Assert.assertTrue(descendants.contains("G"));
        Assert.assertTrue(descendants.contains("H"));
        Assert.assertTrue(descendants.contains("I"));
        Assert.assertTrue(descendants.contains("J"));
        Assert.assertTrue(descendants.contains("K"));
        Assert.assertTrue(descendants.contains("L"));
        Assert.assertTrue(descendants.contains("M"));
        Assert.assertTrue(descendants.contains("N"));
        Assert.assertTrue(descendants.contains("O"));
    }

    @Test(groups = "fast")
    public void thatGetBreedDogsWorks() {
        // given
        Node breedNode = breed("Unit-test Breed");
        addDog("A", breed("Unwanted Breed"));
        addDog("B", breedNode);
        addDog("C", breedNode);
        addDog("D", breedNode);
        addDog("E", breedNode);
        connectChildToFather("B", "A");
        connectChildToFather("C", "B");
        connectChildToFather("D", "C");
        connectChildToFather("E", "C");

        // when
        List<String> dogsOfBreed = graphQueryService.getBreedList("Unit-test Breed");

        // then
        Assert.assertEquals(dogsOfBreed.size(), 4);
        Assert.assertTrue(dogsOfBreed.contains("B"));
        Assert.assertTrue(dogsOfBreed.contains("C"));
        Assert.assertTrue(dogsOfBreed.contains("D"));
        Assert.assertTrue(dogsOfBreed.contains("E"));
    }

    @Test(groups = "fast")
    public void thatUpdatedToCanBeWrittenAndRead() {
        String breedSynonym = "some-breed-for-testing";
        LocalDateTime initialValue = graphQueryService.getUpdatedTo(breedSynonym);
        Assert.assertEquals(initialValue, DogGraphConstants.BEGINNING_OF_TIME);
        LocalDateTime to = new LocalDateTime(2013, 12, 19, 0, 15, 0);
        graphQueryService.setUpdatedTo(breedSynonym, to);
        LocalDateTime newValue = graphQueryService.getUpdatedTo(breedSynonym);
        Assert.assertEquals(newValue, to);
    }
}
