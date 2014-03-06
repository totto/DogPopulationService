package no.nkk.dogpopulation.graph.inbreeding;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class InbreedingAlgorithmTest extends AbstractGraphTest {

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

        Node breedNode = breed("Unit-test Breed", "1");
        addDog("A", breedNode);
        addDog("B", breedNode);
        addDog("C", breedNode);
        addDog("D", breedNode);
        addDog("E", breedNode);
        addDog("F", breedNode);
        addDog("G", breedNode);
        addDog("H", breedNode);
        addDog("I", breedNode);
        addDog("J", breedNode);
        addDog("X", breedNode);
        addDog("Y", breedNode);

        // 1st gen
        connectChildToFather("A", "B");

        connectChildToMother("A", "C");
        // 2nd gen
        connectChildToFather("B", "X");
        connectChildToMother("B", "Y");
        connectChildToFather("C", "B");
        connectChildToMother("C", "D");
        // 3rd gen
        connectChildToFather("X", "G");
        connectChildToMother("X", "H");
        connectChildToFather("Y", "I");
        connectChildToMother("Y", "J");
        // no need to repeat inbreed X father of B
        // no need to repeat inbreed Y mother of B
        connectChildToFather("D", "E");
        connectChildToMother("D", "F");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        double coi = graphQueryService.computeCoefficientOfInbreeding("A", 3).getCoi();

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

        Node breedNode = breed("Unit-test Breed", "1");
        addDog("A", breedNode);
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
        connectChildToMother("M", "K");
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
        connectChildToFather("K", "I");
        connectChildToMother("K", "H");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        // when
        double coi = graphQueryService.computeCoefficientOfInbreeding("A", 5).getCoi();

        // then
        Assert.assertEquals(coi, 0.3125, 0.000001);
    }

}
