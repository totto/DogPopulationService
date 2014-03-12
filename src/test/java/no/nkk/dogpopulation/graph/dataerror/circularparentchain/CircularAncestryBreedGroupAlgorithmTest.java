package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CircularAncestryBreedGroupAlgorithmTest extends AbstractGraphTest {

    @Test
    public void thatCircularAncestryBreedGroupAlgorithmDetectsTheCircle() {
        Node breedNode = breed("Unit-test Breed");
        Set<String> breedSet = new LinkedHashSet<>();
        breedSet.add("Unit-test Breed");
        addDog("A", breedNode, new LocalDate(2007, 6, 17));
        addDog("B", breedNode, new LocalDate(2004, 2, 2));
        addDog("C", breedNode, new LocalDate(2002, 10, 21));
        addDog("D", breedNode, new LocalDate(1998, 4, 13));
        connectChildToFather("A", "B");
        connectChildToFather("B", "C");
        connectChildToFather("C", "D");
        connectChildToFather("D", "B");

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);

        List<String> uuids = graphQueryService.getCircluarParentChainInAncestryOf(breedSet);
        Assert.assertNotNull(uuids);
        Assert.assertEquals(uuids.size(), 1);
    }

}
