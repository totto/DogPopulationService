package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import no.nkk.dogpopulation.UnittestModule;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.Neo4jModule;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
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
public class DogSearchPedigreeImporterTest {

    @Inject
    GraphDatabaseService graphDb;
    ExecutorService executorService;

    @BeforeMethod
    public void initGraph() {
        final Injector injector = Guice.createInjector(
                new UnittestModule(),
                new Neo4jModule()
        );
        injector.injectMembers(this);
        executorService = Executors.newFixedThreadPool(5);
    }

    @AfterMethod
    public void closeGraph() {
        graphDb.shutdown();
        executorService.shutdown();
    }

    @Test
    public void thatAllRelevantFieldsArePresentInGraph() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        DogSearchClient dogSearchClient = new DogSearchClient() {
            @Override
            public Future<Set<String>> listIdsForBreed(String breed) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Future<DogDetails> findDog(String id) {
                if (!"AB/12345/67".equals(id)) {
                    return null;
                }
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    return new ImmediateFuture<>(objectMapper.readValue(new File("src/test/resources/dog_with_offspring.json"), DogDetails.class));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Set<String> listIdsForLastWeek() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> listIdsForLastDay() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> listIdsForLastHour() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<String> listIdsForLastMinute() {
                throw new UnsupportedOperationException();
            }
        };

        BreedSynonymNodeCache breedSynonymNodeCache = new BreedSynonymNodeCache(graphDb);
        Dogs dogs = new Dogs(breedSynonymNodeCache);
        BulkWriteService bulkWriteService = new BulkWriteService(executorService, graphDb).start();

        DogSearchPedigreeImporter importer = new DogSearchPedigreeImporter(executorService, executorService, graphDb, dogSearchClient, dogs, breedSynonymNodeCache, bulkWriteService);
        Future<String> future = importer.importPedigree("AB/12345/67");
        String uuid = future.get(30, TimeUnit.SECONDS);
        importer.stop();

        GraphQueryService graphQueryService = new GraphQueryService(graphDb);
        TopLevelDog topLevelDog = graphQueryService.getPedigree(uuid);
        Assert.assertEquals(topLevelDog.getName(), "Awesomebitch");
        Assert.assertEquals(topLevelDog.getIds().get(DogGraphConstants.DOG_REGNO), "AB/12345/67");
        Assert.assertEquals(topLevelDog.getBreed().getName(), "Border Collie");
        Assert.assertEquals(topLevelDog.getBorn(), "1994-04-28");
        Assert.assertEquals(topLevelDog.getHealth().getHdDiag(), "A1");
        Assert.assertEquals(topLevelDog.getHealth().getHdYear(), 1996);
        Assert.assertEquals(topLevelDog.getOffspring().length, 6);
    }
}
