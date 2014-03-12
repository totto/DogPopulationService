package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.Main;
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
    public void thatImportOfSchaferWorks() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        String dogUuid = "93683d2b-3ad9-4531-bb3d-d8c43f9d99f0";

        // DogSearchClient dogSearchClient = createExternalDogSearchClient(dogUuid); // run once to extract test-data

        DogSearchClient dogSearchClient = createFileReadingDogSearchClient(dogUuid);

        BreedSynonymNodeCache breedSynonymNodeCache = new BreedSynonymNodeCache(graphDb);
        Dogs dogs = new Dogs(breedSynonymNodeCache);
        BulkWriteService bulkWriteService = new BulkWriteService(graphDb);

        DogSearchPedigreeImporter importer = new DogSearchPedigreeImporter(executorService, graphDb, dogSearchClient, dogs, breedSynonymNodeCache, bulkWriteService);
        Future<String> future = importer.importPedigree(dogUuid);
        String uuid = future.get(300, TimeUnit.SECONDS);
        importer.stop();
    }

    private DogSearchClient createFileReadingDogSearchClient(final String folderName) {
        return new DogSearchClient() {
                @Override
                public Set<String> listIdsForBreed(String breed) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public DogDetails findDog(String id) {
                    File directory = new File("src/test/resources/dogsearch/" + folderName);
                    File file = new File(directory, id + ".json");
                    if (!file.isFile()) {
                        return null;
                    }
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        return objectMapper.readValue(file, DogDetails.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
        return new DogSearchSolrClient("http://dogsearch.nkk.no/dogservice/dogs") {
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
