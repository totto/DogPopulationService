package no.nkk.dogpopulation;

import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final PedigreeImporter pedigreeImporter;
    private final ExecutorService executorService;
    private final DogSearchClient dogSearchClient;

    DogPopulationResourceConfigFactory(GraphDatabaseService graphDatabaseService, PedigreeImporter pedigreeImporter, ExecutorService executorService, DogSearchClient dogSearchClient) {
        this.graphDatabaseService = graphDatabaseService;
        this.pedigreeImporter = pedigreeImporter;
        this.executorService = executorService;
        this.dogSearchClient = dogSearchClient;
    }

    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication(graphDatabaseService, pedigreeImporter, executorService, dogSearchClient);
    }
}
