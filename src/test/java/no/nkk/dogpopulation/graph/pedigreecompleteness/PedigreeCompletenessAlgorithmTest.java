package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeCompletenessAlgorithmTest {
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
    public void thatPedigreeWithInbreedingComputesWithCorrectSize() {
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
         * |   |   |   |   |   |
         * |   |   |   |    ---
         * |   |   |   |   |   |
         *  --- --- --- --- ---
         *
         * Given that we want to compute the pedigree completeness of 4 generations we get the following completeness
         * for the above pedigree:
         *   G = generations to compute pedigree-completeness of.
         *   D = number of dogs in a complete pedigree (not including the reference dog itself).
         *   N = number of dogs in current pedigree (not including the reference dog itself).
         *   C = pedigree completeness (in %)
         *
         *   Assuming G = 4 and C = 100%, then D = 2^(G+1)-2 = 2^5-2 = 30.
         *   Assuming G = 4 and N = 21 (as in pedigree drawn above), then C = N/D = 21/30 = 70.97%
         *
         */

        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        Node A = graphAdminService.addDog("A", "A", "Unit-test Breed");
        graphAdminService.addDog("B", "B", "Unit-test Breed");
        graphAdminService.addDog("C", "C", "Unit-test Breed");
        graphAdminService.addDog("D", "D", "Unit-test Breed");
        graphAdminService.addDog("E", "E", "Unit-test Breed");
        graphAdminService.addDog("F", "F", "Unit-test Breed");
        graphAdminService.addDog("G", "G", "Unit-test Breed");
        graphAdminService.addDog("H", "H", "Unit-test Breed");
        graphAdminService.addDog("I", "I", "Unit-test Breed");
        graphAdminService.addDog("J", "J", "Unit-test Breed");
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

        // when
        PedigreeCompletenessAlgorithm algorithm = new PedigreeCompletenessAlgorithm(graphDb, 4);
        int pedigreeSize;
        try (Transaction tx = graphDb.beginTx()) {
            pedigreeSize = algorithm.computePedigreeSize(A);
            tx.success();
        }

        // then
        Assert.assertEquals(pedigreeSize, 21);
    }

    @Test
    public void thatPedigreeCompletenessOfRottweilerAndPointerComputesCorrectlyWithoutAlsoIncludingBoxer() {
        // given
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        // Rottweiler
        graphAdminService.addDog("A", "A", "Rottweiler", LocalDate.parse("2000-11-14"));
        graphAdminService.addDog("B", "B", "Rottweiler", LocalDate.parse("1998-01-03"));
        graphAdminService.addDog("C", "C", "Rottweiler", LocalDate.parse("1998-01-21"));
        graphAdminService.addDog("D", "D", "Rottweiler", LocalDate.parse("1994-12-04"));
        graphAdminService.connectChildToParent("A", "B", ParentRole.FATHER);
        graphAdminService.connectChildToParent("A", "C", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("C", "D", ParentRole.MOTHER);
        // Pointer
        graphAdminService.addDog("E", "E", "Pointer", LocalDate.parse("2001-02-05"));
        graphAdminService.addDog("F", "F", "Pointer", LocalDate.parse("1999-08-18"));
        graphAdminService.addDog("G", "G", "Pointer");
        graphAdminService.addDog("H", "H", "Pointer", LocalDate.parse("2000-04-25"));
        graphAdminService.addDog("I", "I", "Pointer", LocalDate.parse("1996-05-05"));
        graphAdminService.addDog("J", "J", "Pointer", LocalDate.parse("1997-07-30"));
        graphAdminService.addDog("L", "L", "Pointer", LocalDate.parse("1989-10-01"));
        graphAdminService.connectChildToParent("E", "F", ParentRole.FATHER);
        graphAdminService.connectChildToParent("F", "G", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("H", "I", ParentRole.FATHER);
        graphAdminService.connectChildToParent("H", "J", ParentRole.MOTHER);
        graphAdminService.connectChildToParent("I", "L", ParentRole.FATHER);
        // Boxer
        graphAdminService.addDog("M", "M", "Boxer", LocalDate.parse("1999-02-14"));
        graphAdminService.addDog("X", "X", "Boxer", LocalDate.parse("2000-03-12"));
        graphAdminService.connectChildToParent("M", "X", ParentRole.FATHER);
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        Set<String> breedSet = new LinkedHashSet<>();
        breedSet.add("Rottweiler");
        breedSet.add("Pointer");
        PedigreeCompleteness completeness = graphQueryService.getPedigreeCompletenessOfGroup(4, breedSet, 1999, 2000);

        // then
        Assert.assertEquals(completeness.getGenerations(), 4);
        Assert.assertEquals(completeness.getCompletePedigreeSize(), 30); // 2^(4+1) - 2
        Assert.assertEquals(completeness.getStatistics().getN(), 3); // A, F, H
        double delta = 0.001;
        Assert.assertEquals(completeness.getStatistics().getMin(), 1, delta); // F
        Assert.assertEquals(completeness.getStatistics().getMax(), 3, delta); // A, H
        Assert.assertEquals(completeness.getStatistics().getSum(), 7, delta); // 3 + 1 + 3
        Assert.assertEquals(completeness.getStatistics().getMean(), 2.333, delta); // Sum/N = 7/3
        Assert.assertEquals(completeness.getStatistics().getPercentile50(), 3, delta); // median
        Assert.assertEquals(completeness.getStatistics().getPercentile90(), 3, delta);
        Assert.assertEquals(completeness.getStatistics().getStandardDeviation(), 1.155, delta);
    }
}
