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
        graphAdminService.addDog("a", "A", "Unit-test Breed");
        graphAdminService.addDog("b", "B", "Unit-test Breed");
        graphAdminService.addDog("c", "C", "Unit-test Breed");
        graphAdminService.addDog("d", "D", "Unit-test Breed");
        graphAdminService.addDog("e", "E", "Unit-test Breed");
        graphAdminService.addDog("f", "F", "Unit-test Breed");
        graphAdminService.addDog("g", "G", "Unit-test Breed");
        graphAdminService.addDog("h", "H", "Unit-test Breed");
        graphAdminService.addDog("i", "I", "Unit-test Breed");
        graphAdminService.addDog("j", "J", "Unit-test Breed");
        graphAdminService.addDog("k", "K", "Unit-test Breed");
        graphAdminService.addDog("l", "L", "Unit-test Breed");
        graphAdminService.addDog("m", "M", "Unit-test Breed");
        graphAdminService.addDog("n", "N", "Unit-test Breed");
        graphAdminService.addDog("o", "O", "Unit-test Breed");
        graphAdminService.connectChildToParent("b", "a", ParentRole.FATHER);
        graphAdminService.connectChildToParent("c", "b", ParentRole.FATHER);
        graphAdminService.connectChildToParent("d", "c", ParentRole.FATHER);
        graphAdminService.connectChildToParent("e", "c", ParentRole.FATHER);
        graphAdminService.connectChildToParent("f", "b", ParentRole.FATHER);
        graphAdminService.connectChildToParent("g", "f", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("h", "f", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("i", "a", ParentRole.FATHER);
        graphAdminService.connectChildToParent("j", "i", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("k", "j", ParentRole.FATHER);
        graphAdminService.connectChildToParent("l", "j", ParentRole.FATHER);
        graphAdminService.connectChildToParent("m", "i", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("n", "m", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("o", "m", ParentRole.MOTHER);
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        LinkedHashSet<String> descendants = new LinkedHashSet<>();
        graphQueryService.populateDescendantUuids("a", descendants);

        // then
        Assert.assertEquals(descendants.size(), 14);
        Assert.assertFalse(descendants.contains("a"));
        Assert.assertTrue(descendants.contains("b"));
        Assert.assertTrue(descendants.contains("c"));
        Assert.assertTrue(descendants.contains("d"));
        Assert.assertTrue(descendants.contains("e"));
        Assert.assertTrue(descendants.contains("f"));
        Assert.assertTrue(descendants.contains("g"));
        Assert.assertTrue(descendants.contains("h"));
        Assert.assertTrue(descendants.contains("i"));
        Assert.assertTrue(descendants.contains("j"));
        Assert.assertTrue(descendants.contains("k"));
        Assert.assertTrue(descendants.contains("l"));
        Assert.assertTrue(descendants.contains("m"));
        Assert.assertTrue(descendants.contains("n"));
        Assert.assertTrue(descendants.contains("o"));
    }

    @Test
    public void thatGetBreedDogsWorks() {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        graphAdminService.addDog("a", "A", "Unwanted Breed");
        graphAdminService.addDog("b", "B", "Unit-test Breed");
        graphAdminService.addDog("c", "C", "Unit-test Breed");
        graphAdminService.addDog("d", "D", "Unit-test Breed");
        graphAdminService.addDog("e", "E", "Unit-test Breed");
        graphAdminService.connectChildToParent("b", "a", ParentRole.FATHER);
        graphAdminService.connectChildToParent("c", "b", ParentRole.FATHER);
        graphAdminService.connectChildToParent("d", "c", ParentRole.FATHER);
        graphAdminService.connectChildToParent("e", "c", ParentRole.FATHER);
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        List<String> dogsOfBreed = graphQueryService.getBreedList("Unit-test Breed");

        // then
        Assert.assertEquals(dogsOfBreed.size(), 4);
        Assert.assertTrue(dogsOfBreed.contains("b"));
        Assert.assertTrue(dogsOfBreed.contains("c"));
        Assert.assertTrue(dogsOfBreed.contains("d"));
        Assert.assertTrue(dogsOfBreed.contains("e"));
    }

    @Test
    public void thatCoefficientOfInbreedingIsCorrectForOffspringOfAFatherWithDaughterMatingUsingPedigreeOfThreeGenerations() {
        /*
         *  ---------------
         * |   |   |   | G |
         * |   |   | X  ---
         * |   |   |   | H |
         * |   | B  --- ---
         * |   |   |   | I |
         * |   |   | Y  ---
         * |   |   |   | J |
         *   A  -----------
         * |   |   |   | X |
         * |   |   | B  ---
         * |   |   |   | Y |
         * |   | C  --- ---
         * |   |   |   | E |
         * |   |   | D  ---
         * |   |   |   | F |
         *  ---------------
         *
         * B is common ancestor, Distinct dogs traversing from father to mother through common ancestor are: BC = 2 dogs.
         * This gives COI = (0.5)^2 = 0.25 = 25%
         */

        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        graphAdminService.addDog("a", "A", "Unit-test Breed");
        graphAdminService.addDog("b", "B", "Unit-test Breed");
        graphAdminService.addDog("c", "C", "Unit-test Breed");
        graphAdminService.addDog("d", "D", "Unit-test Breed");
        graphAdminService.addDog("e", "E", "Unit-test Breed");
        graphAdminService.addDog("f", "F", "Unit-test Breed");
        graphAdminService.addDog("g", "G", "Unit-test Breed");
        graphAdminService.addDog("h", "H", "Unit-test Breed");
        graphAdminService.addDog("i", "I", "Unit-test Breed");
        graphAdminService.addDog("j", "J", "Unit-test Breed");
        graphAdminService.addDog("x", "X", "Unit-test Breed");
        graphAdminService.addDog("y", "Y", "Unit-test Breed");

        // 1st gen
        graphAdminService.connectChildToParent("a", "b", ParentRole.FATHER);
        graphAdminService.connectChildToParent("a", "c", ParentRole.MOTHER);
        // 2nd gen
        graphAdminService.connectChildToParent("b", "x", ParentRole.FATHER);
        graphAdminService.connectChildToParent("b", "y", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("c", "b", ParentRole.FATHER);
        graphAdminService.connectChildToParent("c", "d", ParentRole.MOTHER);
        // 3rd gen
        graphAdminService.connectChildToParent("x", "g", ParentRole.FATHER);
        graphAdminService.connectChildToParent("x", "h", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("y", "i", ParentRole.FATHER);
        graphAdminService.connectChildToParent("y", "j", ParentRole.MOTHER);
        // no need to repeat inbreed X father of B from 2nd gen
        // no need to repeat inbreed Y mother of B from 2nd gen
        graphAdminService.connectChildToParent("d", "e", ParentRole.FATHER);
        graphAdminService.connectChildToParent("d", "f", ParentRole.MOTHER);

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        double coi = graphQueryService.computeCoefficientOfInbreeding("a", 3);

        // then
        Assert.assertEquals(coi, 0.25, 0.0001);
    }

}
