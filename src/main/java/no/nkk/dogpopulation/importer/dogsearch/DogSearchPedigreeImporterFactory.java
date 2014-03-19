package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchPedigreeImporterFactory implements PedigreeImporterFactory {
    private final ExecutorManager executorManager;
    private final GraphDatabaseService graphDb;
    private final DogSearchClient dogSearchClient;
    private final Dogs dogs;
    private final BreedSynonymNodeCache breedSynonymNodeCache;

    public DogSearchPedigreeImporterFactory(ExecutorManager executorManager, GraphDatabaseService graphDb, DogSearchClient dogSearchClient, Dogs dogs, BreedSynonymNodeCache breedSynonymNodeCache) {
        this.executorManager = executorManager;
        this.graphDb = graphDb;
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.breedSynonymNodeCache = breedSynonymNodeCache;
    }

    @Override
    public PedigreeImporter createInstance(BulkWriteService bulkWriteService) {
        return new DogSearchPedigreeImporter(executorManager.getExecutor(ExecutorManager.GRAPH_QUERY_MAP_KEY), executorManager.getExecutor(ExecutorManager.TRAVERSER_MAP_KEY), graphDb, dogSearchClient, dogs, breedSynonymNodeCache, bulkWriteService);
    }
}
