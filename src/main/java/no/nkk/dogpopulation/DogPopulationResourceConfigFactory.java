package no.nkk.dogpopulation;

import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final PedigreeImporterFactory pedigreeImporterFactory;
    private final ExecutorService executorService;
    private final DogSearchClient dogSearchClient;

    DogPopulationResourceConfigFactory(GraphDatabaseService graphDatabaseService, PedigreeImporterFactory pedigreeImporterFactory, ExecutorService executorService, DogSearchClient dogSearchClient) {
        this.graphDatabaseService = graphDatabaseService;
        this.pedigreeImporterFactory = pedigreeImporterFactory;
        this.executorService = executorService;
        this.dogSearchClient = dogSearchClient;
    }

    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication(graphDatabaseService, executorService, dogSearchClient, pedigreeImporterFactory);
    }
}
