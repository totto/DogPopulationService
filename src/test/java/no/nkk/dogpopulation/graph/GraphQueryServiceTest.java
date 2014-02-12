package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.Main;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphQueryServiceTest {

    GraphDatabaseService graphDb;

    @BeforeMethod
    public void initGraph() {
        String dbPath = "target/unittestdogdb";
        File dbFolder = new File(dbPath);
        FileUtils.deleteQuietly(dbFolder);
        graphDb = Main.createGraphDb(dbPath);
    }

    @AfterMethod
    public void closeGraph() {
        graphDb.shutdown();
    }

    @Test
    public void thatPopulateDescendantsIncludesAllButItself() {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        graphAdminService.addDog("A", "A", "Unit-test Breed");
        graphAdminService.addDog("B", "B", "Unit-test Breed");
        graphAdminService.addDog("C", "C", "Unit-test Breed");
        graphAdminService.addDog("D", "D", "Unit-test Breed");
        graphAdminService.addDog("E", "E", "Unit-test Breed");
        graphAdminService.addDog("F", "F", "Unit-test Breed");
        graphAdminService.addDog("G", "G", "Unit-test Breed");
        graphAdminService.addDog("H", "H", "Unit-test Breed");
        graphAdminService.addDog("I", "I", "Unit-test Breed");
        graphAdminService.addDog("J", "J", "Unit-test Breed");
        graphAdminService.addDog("K", "K", "Unit-test Breed");
        graphAdminService.addDog("L", "L", "Unit-test Breed");
        graphAdminService.addDog("M", "M", "Unit-test Breed");
        graphAdminService.addDog("N", "N", "Unit-test Breed");
        graphAdminService.addDog("O", "O", "Unit-test Breed");
        graphAdminService.connectChildToParent("B", "A", ParentRole.FATHER);
        graphAdminService.connectChildToParent("C", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("D", "C", ParentRole.FATHER);
        graphAdminService.connectChildToParent("E", "C", ParentRole.FATHER);
        graphAdminService.connectChildToParent("F", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("G", "F", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("H", "F", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("I", "A", ParentRole.FATHER);
        graphAdminService.connectChildToParent("J", "I", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("K", "J", ParentRole.FATHER);
        graphAdminService.connectChildToParent("L", "J", ParentRole.FATHER);
        graphAdminService.connectChildToParent("M", "I", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("N", "M", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("O", "M", ParentRole.MOTHER);
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        LinkedHashSet<String> descendants = new LinkedHashSet<>();
        graphQueryService.populateDescendantUuids("A", descendants);

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

    @Test
    public void thatGetBreedDogsWorks() {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        graphAdminService.addDog("A", "A", "Unwanted Breed");
        graphAdminService.addDog("B", "B", "Unit-test Breed");
        graphAdminService.addDog("C", "C", "Unit-test Breed");
        graphAdminService.addDog("D", "D", "Unit-test Breed");
        graphAdminService.addDog("E", "E", "Unit-test Breed");
        graphAdminService.connectChildToParent("B", "A", ParentRole.FATHER);
        graphAdminService.connectChildToParent("C", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("D", "C", ParentRole.FATHER);
        graphAdminService.connectChildToParent("E", "C", ParentRole.FATHER);
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        List<String> dogsOfBreed = graphQueryService.getBreedList("Unit-test Breed");

        // then
        Assert.assertEquals(dogsOfBreed.size(), 4);
        Assert.assertTrue(dogsOfBreed.contains("B"));
        Assert.assertTrue(dogsOfBreed.contains("C"));
        Assert.assertTrue(dogsOfBreed.contains("D"));
        Assert.assertTrue(dogsOfBreed.contains("E"));
    }

}
