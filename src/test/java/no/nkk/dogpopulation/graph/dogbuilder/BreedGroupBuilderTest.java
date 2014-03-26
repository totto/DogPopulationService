package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.AbstractGraphTest;
import org.neo4j.graphdb.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedGroupBuilderTest extends AbstractGraphTest {

    @Test
    public void thatAddingDogsCreatesBreedSynonymsOnly() {
        // when
        Node breedSynonym1 = breed("australian cattledog");
        Node breedSynonym2 = breed("Australian Cattledog");
        addDog("uuid-1", breedSynonym1);
        addDog("uuid-2", breedSynonym1);
        addDog("uuid-3", breedSynonym1);
        addDog("uuid-4", breedSynonym2);
        addDog("uuid-5", breedSynonym2);
        addDog("uuid-6", breedSynonym2);

        // then
        for (Map<String, Object> result : executionEngine.execute("MATCH (n:BREED) RETURN count(n)")) {
            long n = (Long) result.get("count(n)");
            Assert.assertEquals(n, 0);
        }
        for (Map<String, Object> result : executionEngine.execute("MATCH (n:BREED_SYNONYM) RETURN count(n)")) {
            long n = (Long) result.get("count(n)");
            Assert.assertEquals(n, 2);
        }
        for (Map<String, Object> result : executionEngine.execute("MATCH (n:BREED_GROUP) RETURN count(n)")) {
            long n = (Long) result.get("count(n)");
            Assert.assertEquals(n, 0);
        }
        for (Map<String, Object> result : executionEngine.execute("MATCH (n:CATEGORY) RETURN count(n)")) {
            long n = (Long) result.get("count(n)");
            Assert.assertEquals(n, 0);
        }
        for (Map<String, Object> result : executionEngine.execute("MATCH (n:DOG) RETURN count(n)")) {
            long n = (Long) result.get("count(n)");
            Assert.assertEquals(n, 6);
        }
    }

    @Test
    public void thatBreedGroupsCanBeCreatedInEmptyGraph() {
    }

    @Test
    public void thatBreedGroupsCanBeAddedToExistingSynonyms() {
    }

}
