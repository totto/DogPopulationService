package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.CommonNodes;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchPedigreeImporterFactory implements PedigreeImporterFactory {
    private final ExecutorService executorService;
    private final GraphDatabaseService graphDb;
    private final DogSearchClient dogSearchClient;
    private final Dogs dogs;
    private final CommonNodes commonNodes;

    public DogSearchPedigreeImporterFactory(ExecutorService executorService, GraphDatabaseService graphDb, DogSearchClient dogSearchClient, Dogs dogs, CommonNodes commonNodes) {
        this.executorService = executorService;
        this.graphDb = graphDb;
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.commonNodes= commonNodes;
    }

    @Override
    public PedigreeImporter createInstance(BulkWriteService bulkWriteService) {
        return new DogSearchPedigreeImporter(executorService, graphDb, dogSearchClient, dogs, commonNodes, bulkWriteService);
    }
}
