package no.nkk.dogpopulation.graph.inbreeding;

import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class InbreedingAlgorithmTest {

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
    public void thatCoefficientOfInbreedingIsCorrectForOffspringOfAFatherWithDaughterMatingUsingPedigreeOfThreeGenerations() {
        // given

        /*
         *  --- --- --- ---
         * |   |   |   | G |
         * |   |   | X  ---
         * |   |   |   | H |
         * |   | B  --- ---
         * |   |   |   | I |
         * |   |   | Y  ---
         * |   |   |   | J |
         * | A  --- --- ---
         * |   |   |   | X |
         * |   |   | B  ---
         * |   |   |   | Y |
         * |   | C  --- ---
         * |   |   |   | E |
         * |   |   | D  ---
         * |   |   |   | F |
         *  --- --- --- ---
         *
         * B is common ancestor, Distinct dogs traversing from father to mother through common ancestor are: BC = 2 dogs.
         * This gives COI = (0.5)^2 = 0.25 = 25%
         */

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
        graphAdminService.addDog("X", "X", "Unit-test Breed");
        graphAdminService.addDog("Y", "Y", "Unit-test Breed");

        // 1st gen
        graphAdminService.connectChildToParent("A", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("A", "C", ParentRole.MOTHER);
        // 2nd gen
        graphAdminService.connectChildToParent("B", "X", ParentRole.FATHER);
        graphAdminService.connectChildToParent("B", "Y", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("C", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("C", "D", ParentRole.MOTHER);
        // 3rd gen
        graphAdminService.connectChildToParent("X", "G", ParentRole.FATHER);
        graphAdminService.connectChildToParent("X", "H", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("Y", "I", ParentRole.FATHER);
        graphAdminService.connectChildToParent("Y", "J", ParentRole.MOTHER);
        // no need to repeat inbreed X father of B
        // no need to repeat inbreed Y mother of B
        graphAdminService.connectChildToParent("D", "E", ParentRole.FATHER);
        graphAdminService.connectChildToParent("D", "F", ParentRole.MOTHER);

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        double coi = graphQueryService.computeCoefficientOfInbreeding("A", 3);

        // then
        Assert.assertEquals(coi, 0.25, 0.000001);
    }

    @Test
    public void thatCoefficientOfInbreedingIsCorrectForOffspringOfAComplexInbreedingUsingPedigreeOfFourGenerations() {
        // given

        /*
         *  --- --- --- --- ---
         * |   |   |   |   | L |
         * |   |   |   | G  ---
         * |   |   |   |   | H |
         * |   |   | X  --- ---
         * |   |   |   |   |   |
         * |   |   |   | H  ---
         * |   |   |   |   |   |
         * |   | B  --- --- ---
         * |   |   |   |   | G |
         * |   |   |   | I  ---
         * |   |   |   |   |   |
         * |   |   | Y  --- ---
         * |   |   |   |   | G |
         * |   |   |   | J  ---
         * |   |   |   |   | H |
         * | A  --- --- --- ---
         * |   |   |   |   | L |
         * |   |   |   | G  ---
         * |   |   |   |   | H |
         * |   |   | X  --- ---
         * |   |   |   |   |   |
         * |   |   |   | H  ---
         * |   |   |   |   |   |
         * |   | C  --- --- ---
         * |   |   |   |   | G |
         * |   |   |   | I  ---
         * |   |   |   |   |   |
         * |   |   | M  --- ---
         * |   |   |   |   | I |
         * |   |   |   | K  ---
         * |   |   |   |   | H |
         *  --- --- --- --- ---
         *
         * B is common ancestor, Distinct dogs traversing from father to mother through common ancestor are: BC = 2 dogs.
         * This gives COI = (0.5)^2 = 0.25 = 25%
         */

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
        graphAdminService.addDog("X", "X", "Unit-test Breed");
        graphAdminService.addDog("Y", "Y", "Unit-test Breed");

        // 1st gen
        graphAdminService.connectChildToParent("A", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("A", "C", ParentRole.MOTHER);
        // 2nd gen
        graphAdminService.connectChildToParent("B", "X", ParentRole.FATHER);
        graphAdminService.connectChildToParent("B", "Y", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("C", "X", ParentRole.FATHER);
        graphAdminService.connectChildToParent("C", "M", ParentRole.MOTHER);
        // 3rd gen
        graphAdminService.connectChildToParent("X", "G", ParentRole.FATHER);
        graphAdminService.connectChildToParent("X", "H", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("Y", "I", ParentRole.FATHER);
        graphAdminService.connectChildToParent("Y", "J", ParentRole.MOTHER);
        // no need to repeat inbreed G father of X
        // no need to repeat inbreed H mother of X
        graphAdminService.connectChildToParent("M", "I", ParentRole.FATHER);
        graphAdminService.connectChildToParent("M", "K", ParentRole.MOTHER);
        // 4th gen
        graphAdminService.connectChildToParent("G", "L", ParentRole.FATHER);
        graphAdminService.connectChildToParent("G", "H", ParentRole.MOTHER);
        // unknown father of H
        // unknown mother of H
        graphAdminService.connectChildToParent("I", "G", ParentRole.FATHER);
        // unknown mother of I
        graphAdminService.connectChildToParent("J", "G", ParentRole.FATHER);
        graphAdminService.connectChildToParent("J", "H", ParentRole.MOTHER);
        // no need to repeat inbreed L father of G
        // no need to repeat inbreed H mother of G
        // no need to repeat inbreed father of H
        // no need to repeat inbreed mother of H
        // no need to repeat inbreed G father of I
        // no need to repeat inbreed mother of I
        graphAdminService.connectChildToParent("K", "I", ParentRole.FATHER);
        graphAdminService.connectChildToParent("K", "H", ParentRole.MOTHER);

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        double coi = graphQueryService.computeCoefficientOfInbreeding("A", 4);

        // then
        Assert.assertEquals(coi, 0.3125, 0.000001);
    }

}
