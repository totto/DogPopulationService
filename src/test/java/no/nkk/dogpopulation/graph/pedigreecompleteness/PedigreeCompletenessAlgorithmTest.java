package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeCompletenessAlgorithmTest extends AbstractGraphTest {

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

        Node breedNode = breed("Unit-test Breed", "1");
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
        addDog("L", breedNode);
        addDog("M", breedNode);
        addDog("X", breedNode);
        addDog("Y", breedNode);

        // 1st gen
        connectChildToFather("A", "B");
        connectChildToMother("A", "C");
        // 2nd gen
        connectChildToFather("B", "X");
        connectChildToMother("B", "Y");
        connectChildToFather("C", "X");
        connectChildToMother("C", "M");
        // 3rd gen
        connectChildToFather("X", "G");
        connectChildToMother("X", "H");
        connectChildToFather("Y", "I");
        connectChildToMother("Y", "J");
        // no need to repeat inbreed G father of X
        // no need to repeat inbreed H mother of X
        connectChildToFather("M", "I");
        // 4th gen
        connectChildToFather("G", "L");
        connectChildToMother("G", "H");
        // unknown father of H
        // unknown mother of H
        connectChildToFather("I", "G");
        // unknown mother of I
        connectChildToFather("J", "G");
        connectChildToMother("J", "H");
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
        Node rottweiler = breed("Rottweiler", "1");
        // Rottweiler
        addDog("A", rottweiler, LocalDate.parse("2000-11-14"));
        addDog("B", rottweiler, LocalDate.parse("1998-01-03"));
        addDog("C", rottweiler, LocalDate.parse("1998-01-21"));
        addDog("D", rottweiler, LocalDate.parse("1994-12-04"));
        connectChildToFather("A", "B");
        connectChildToMother("A", "C");
        connectChildToMother("C", "D");
        // Pointer
        Node pointer = breed("Pointer", "2");
        addDog("E", pointer, LocalDate.parse("2001-02-05"));
        addDog("F", pointer, LocalDate.parse("1999-08-18"));
        addDog("G", pointer);
        addDog("H", pointer, LocalDate.parse("2000-04-25"));
        addDog("I", pointer, LocalDate.parse("1996-05-05"));
        addDog("J", pointer, LocalDate.parse("1997-07-30"));
        addDog("L", pointer, LocalDate.parse("1989-10-01"));
        connectChildToFather("E", "F");
        connectChildToMother("F", "G");
        connectChildToFather("H", "I");
        connectChildToMother("H", "J");
        connectChildToFather("I", "L");
        // Boxer
        Node boxer = breed("Boxer", "3");
        addDog("M", boxer, LocalDate.parse("1999-02-14"));
        addDog("X", boxer, LocalDate.parse("2000-03-12"));
        connectChildToFather("M", "X");
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);


        // when
        Set<String> breedSet = new LinkedHashSet<>();
        breedSet.add("Rottweiler");
        breedSet.add("Pointer");
        PedigreeCompleteness completeness = graphQueryService.getPedigreeCompletenessOfGroup(4, breedSet, 1999, 2000);

        // then
        Assert.assertEquals(completeness.getGenerations(), 4);
        Assert.assertEquals(completeness.getCompletePedigreeSize(), 30); // 2^(4+1) - 2
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getN(), 3); // A, F, H
        double delta = 0.001;
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getMin(), 1, delta); // F
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getMax(), 3, delta); // A, H
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getSum(), 7, delta); // 3 + 1 + 3
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getMean(), 2.333, delta); // Sum/N = 7/3
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getPercentile50(), 3, delta); // median
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getPercentile90(), 3, delta);
        Assert.assertEquals(completeness.getPedigreeSizeStatistics().getStandardDeviation(), 1.155, delta);
    }
}
