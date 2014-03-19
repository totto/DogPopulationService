package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchPedigreeImporter implements PedigreeImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchPedigreeImporter.class);

    private final GraphDatabaseService graphDb;
    private final DogSearchClient dogSearchClient;

    private final Dogs dogs;
    private final BreedSynonymNodeCache breedSynonymNodeCache;

    private final ExecutorService graphQueryExectuor;
    private final ExecutorService traversingExectuor;

    private final BulkWriteService bulkWriteService;

    public DogSearchPedigreeImporter(ExecutorService graphQueryExectuor, ExecutorService traversingExectuor, GraphDatabaseService graphDb, DogSearchClient dogSearchClient, Dogs dogs, BreedSynonymNodeCache breedSynonymNodeCache, BulkWriteService bulkWriteService) {
        this.graphQueryExectuor = graphQueryExectuor;
        this.traversingExectuor = traversingExectuor;
        this.graphDb = graphDb;
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.breedSynonymNodeCache = breedSynonymNodeCache;
        this.bulkWriteService = bulkWriteService;
    }


    @Override
    public Future<String> importPedigree(final String id) {
        return traversingExectuor.submit(pedigreeImportTaskFor(id));
    }

    Callable<String> pedigreeImportTaskFor(final String id) {
        return new Callable<String>() {
            @Override
            public String call() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(id);
                try {
                    TraversalStatistics ts = importDogPedigree(id);
                    if (ts == null) {
                        return null;
                    }
                    return ts.id;
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        };
    }

    @Override
    public TraversalStatistics importDogPedigree(String id) {
        return importDogPedigree(id, new TraversalStatistics(id));
    }

    @Override
    public void stop() {
        bulkWriteService.stop();
    }

    public TraversalStatistics importDogPedigree(String id, TraversalStatistics ts) {
        long startTime = System.currentTimeMillis();
        LOGGER.trace("Importing Pedigree from DogSearch for dog {}", id);
        DogFuture dogFuture = startTraversingDogTask(id, ts);
        if (dogFuture == null) {
            LOGGER.trace("Could not resolve id {}", id);
            return ts;
        }
        dogFuture.waitForPedigreeImportToComplete();
        long durationMs = System.currentTimeMillis() - startTime;
        double duration = durationMs / 1000;
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        LOGGER.trace("Imported Pedigree of dog {} in {} seconds, {} Dogs/sec. -- {}", id, decimalFormat.format(duration), decimalFormat.format(1000.0 * (ts.dogsAdded.get() + ts.puppiesAdded.get()) / durationMs));
        return ts;
    }

    private DogFuture startTraversingDogTask(String id, TraversalStatistics ts) {
        TraversingDogTask traversingDogTask = new TraversingDogTask(graphQueryExectuor, traversingExectuor, graphDb, dogSearchClient, dogs, breedSynonymNodeCache, bulkWriteService, id, ts, 1);
        DogFuture dogFuture;
        try {
            dogFuture = traversingDogTask.call(); // execute in current thread
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return dogFuture;
    }

}
