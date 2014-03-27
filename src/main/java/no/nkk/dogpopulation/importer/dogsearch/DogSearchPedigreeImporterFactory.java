package no.nkk.dogpopulation.importer.dogsearch;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class DogSearchPedigreeImporterFactory implements PedigreeImporterFactory {


    private final ExecutorService traversingExecutor;
    private final ExecutorService bulkWriterExecutor;
    private final GraphDatabaseService graphDb;
    private final DogSearchClient dogSearchClient;
    private final Dogs dogs;
    private final BreedSynonymNodeCache breedSynonymNodeCache;
    private final GraphQueryService graphQueryService;

    @Inject
    public DogSearchPedigreeImporterFactory(
            @Named(ExecutorManager.TRAVERSER_MAP_KEY) ExecutorService traversingExecutor,
            @Named(ExecutorManager.BULK_WRITER_MAP_KEY) ExecutorService bulkWriterExecutor,
            GraphDatabaseService graphDb,
            DogSearchClient dogSearchClient,
            Dogs dogs,
            BreedSynonymNodeCache breedSynonymNodeCache, GraphQueryService graphQueryService) {
        this.traversingExecutor = traversingExecutor;
        this.bulkWriterExecutor = bulkWriterExecutor;
        this.graphDb = graphDb;
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.breedSynonymNodeCache = breedSynonymNodeCache;
        this.graphQueryService = graphQueryService;
    }

    @Override
    public DogSearchPedigreeImporter createInstance(String context) {
        BulkWriteService bulkWriteService = new BulkWriteService(graphDb, context).start(bulkWriterExecutor);
        return new DogSearchPedigreeImporter(traversingExecutor, graphDb, dogSearchClient, dogs, breedSynonymNodeCache, bulkWriteService, graphQueryService);
    }
}
