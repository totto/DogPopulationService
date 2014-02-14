package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.TopLevelDog;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchImporterTest {

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
    public void thatAllRelevantFieldsArePresentInGraph() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        DogSearchClient dogSearchClient = new DogSearchClient() {
            @Override
            public Set<String> listIdsForBreed(String breed) {
                throw new UnsupportedOperationException();
            }

            @Override
            public DogDetails findDog(String id) {
                if (!"AB/12345/67".equals(id)) {
                    return null;
                }
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    return objectMapper.readValue(new File("src/test/resources/dog_with_offspring.json"), DogDetails.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        DogSearchImporter importer = new DogSearchImporter(executorService, graphDb, dogSearchClient);
        Future<String> future = importer.importDog("AB/12345/67");
        String uuid = future.get(30, TimeUnit.SECONDS);

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);
        TopLevelDog topLevelDog = graphQueryService.getPedigree(uuid);
        Assert.assertEquals(topLevelDog.getName(), "Awesomebitch");
        Assert.assertEquals(topLevelDog.getIds().get(DogGraphConstants.DOG_REGNO), "AB/12345/67");
        Assert.assertEquals(topLevelDog.getBreed().getName(), "Border Collie");
        Assert.assertEquals(topLevelDog.getBreed().getId(), "401");
        Assert.assertEquals(topLevelDog.getBorn(), "1994-04-28");
        Assert.assertEquals(topLevelDog.getHealth().getHdDiag(), "A1");
        Assert.assertEquals(topLevelDog.getHealth().getHdYear(), 1996);
        // Assert.assertEquals(topLevelDog.getOffspring().length, 6);
        Assert.assertNull(topLevelDog.getOffspring());
    }
}
