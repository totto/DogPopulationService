package no.nkk.dogpopulation.importer.breedupdater;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import no.nkk.dogpopulation.breedgroupimport.BreedGroupJsonImporter;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.concurrent.ThreadingModule;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.Neo4jModule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.fail;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedImportIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreedImportIntegrationTest.class);

    @Test(groups = "oome")
    public void thatImportOfAllBreedsCompletesWithoutExceptions() {
        try {
            int maxConcurrentBreedImports = 500;
            int maxConcurrentPedigreePerBreedImports = 500;
            final Injector injector = Guice.createInjector(
                    new IntegrationtestModule(),
                    new ThreadingModule(maxConcurrentBreedImports, maxConcurrentPedigreePerBreedImports),
                    new Neo4jModule()
            );

            final GraphDatabaseService db = injector.getInstance(GraphDatabaseService.class);
            try {
                BreedGroupJsonImporter breedGroupJsonImporter = injector.getInstance(BreedGroupJsonImporter.class);
                BreedUpdateService breedUpdateService = injector.getInstance(BreedUpdateService.class);
                ExecutorService breedImporterExecutor = injector.getInstance(Key.get(ExecutorService.class, Names.named(ExecutorManager.BREED_IMPORTER_MAP_KEY)));
                GraphQueryService graphQueryService = injector.getInstance(GraphQueryService.class);

            /*
             * Test initialization
             */

                breedGroupJsonImporter.importBreedGroup();

                markAllBreedSynonymNodesAsUpdatedToBeginningOfTime(graphQueryService); // will cause all breeds to be part of the import test

                Runnable theTaskToTest = breedUpdateService.createBreedUpdaterTask();

            /*
             * Start the test asynchronously
             */

                theTaskToTest.run(); // will queue tasks to update all breeds known to Raser.json


            /*
             * Wait at most 24 hours for test to complete
             */
                shutdownExecutorAndAwaitTermination(breedImporterExecutor, 24 * 60);
            } catch (OutOfMemoryError e) {

                Runtime runtime = Runtime.getRuntime();

                NumberFormat format = NumberFormat.getInstance();

                StringBuilder sb = new StringBuilder();
                long maxMemory = runtime.maxMemory();
                long allocatedMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();

                sb.append("free memory: " + format.format(freeMemory / 1024) + "  ");
                sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "  ");
                sb.append("max memory: " + format.format(maxMemory / 1024) + "  ");
                sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "  ");


                fail("Unstable version, out of memory. before shotdown\n"+sb);

            } finally {
                db.shutdown();
            }
        } catch (OutOfMemoryError e) {

            Runtime runtime = Runtime.getRuntime();

            NumberFormat format = NumberFormat.getInstance();

            StringBuilder sb = new StringBuilder();
            long maxMemory = runtime.maxMemory();
            long allocatedMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();

            sb.append("free memory: " + format.format(freeMemory / 1024) + "  ");
            sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "  ");
            sb.append("max memory: " + format.format(maxMemory / 1024) + "  ");
            sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "  ");


            fail("Unstable version, out of memory.\n"+sb);

        }
    }

    private void markAllBreedSynonymNodesAsUpdatedToBeginningOfTime(GraphQueryService graphQueryService) {
        List<String> breedSynonyms = graphQueryService.listAllBreedSynonyms();
        for (String breedSynonym : breedSynonyms) {
            graphQueryService.setUpdatedTo(breedSynonym, DogGraphConstants.BEGINNING_OF_TIME);
        }
    }

    private void shutdownExecutorAndAwaitTermination(ExecutorService pool, int minutesToWait) {
        pool.shutdown(); // Safe, since all tasks are already submitted
        try {
            if (!pool.awaitTermination(minutesToWait, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOGGER.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
