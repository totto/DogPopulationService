package no.nkk.dogpopulation;

import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final PedigreeImporterFactory pedigreeImporterFactory;
    private final ExecutorManager executorManager;
    private final DogSearchClient dogSearchClient;

    DogPopulationResourceConfigFactory(GraphDatabaseService graphDatabaseService, PedigreeImporterFactory pedigreeImporterFactory, ExecutorManager executorManager, DogSearchClient dogSearchClient) {
        this.graphDatabaseService = graphDatabaseService;
        this.pedigreeImporterFactory = pedigreeImporterFactory;
        this.executorManager = executorManager;
        this.dogSearchClient = dogSearchClient;
    }

    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication(graphDatabaseService, executorManager, dogSearchClient, pedigreeImporterFactory);
    }
}
