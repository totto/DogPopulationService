package no.nkk.dogpopulation;

import no.nkk.dogpopulation.importer.DogImporter;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final DogImporter dogImporter;

    DogPopulationResourceConfigFactory(GraphDatabaseService graphDatabaseService, DogImporter dogImporter) {
        this.graphDatabaseService = graphDatabaseService;
        this.dogImporter = dogImporter;
    }

    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication(graphDatabaseService, dogImporter);
    }
}
