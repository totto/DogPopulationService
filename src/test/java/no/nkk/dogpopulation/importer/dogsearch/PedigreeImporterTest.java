package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.concurrent.ManageableExecutor;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeImporterTest {
    GraphDatabaseService graphDb;
    ExecutorService executorService;

    @BeforeMethod
    public void initGraph() {
        String dbPath = "target/unittestdogdb";
        File dbFolder = new File(dbPath);
        FileUtils.deleteQuietly(dbFolder);
        graphDb = Main.createGraphDb(dbPath);
        executorService = Executors.newFixedThreadPool(5);
    }

    @AfterMethod
    public void closeGraph() {
        graphDb.shutdown();
        executorService.shutdown();
    }

    @Test
    public void thatImportOfSchaferWorks() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = new ManageableExecutor(5, 30);

        String dogUuid = "93683d2b-3ad9-4531-bb3d-d8c43f9d99f0";

        // DogSearchClient dogSearchClient = createExternalDogSearchClient(dogUuid); // run once to extract test-data

        DogSearchClient dogSearchClient = createFileReadingDogSearchClient(dogUuid);

        BreedSynonymNodeCache breedSynonymNodeCache = new BreedSynonymNodeCache(graphDb);
        Dogs dogs = new Dogs(breedSynonymNodeCache);
        BulkWriteService bulkWriteService = new BulkWriteService(executorService, graphDb).start();

        DogSearchPedigreeImporter importer = new DogSearchPedigreeImporter(executorService, executorService, graphDb, dogSearchClient, dogs, breedSynonymNodeCache, bulkWriteService);
        Future<String> future = importer.importPedigree(dogUuid);
        String uuid = future.get(300, TimeUnit.SECONDS);
        importer.stop();
    }

    private DogSearchClient createFileReadingDogSearchClient(final String folderName) {
        return new DogSearchClient() {
                @Override
                public Future<Set<String>> listIdsForBreed(String breed) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Future<DogDetails> findDog(String id) {
                    File directory = new File("src/test/resources/dogsearch/" + folderName);
                    File file = new File(directory, id + ".json");
                    if (!file.isFile()) {
                        return null;
                    }
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        return new ImmediateFuture<>(objectMapper.readValue(file, DogDetails.class));
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
    }

    /**
     * Used to extract test-data from dogsearch.
     *
     * @return
     */
    private DogSearchClient createExternalDogSearchClient(String folderName) {
        final File directory = new File("src/test/resources/dogsearch/" + folderName);
        directory.mkdirs();
        return new DogSearchSolrClient(executorService, "http://dogsearch.nkk.no/dogservice/dogs") {
            protected void preProcess(String id, String json_detailed) {
                File file = new File(directory, id + ".json");
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")))) {
                    bw.write(json_detailed);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}
