package no.nkk.dogpopulation;

import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {

    private final GraphDatabaseService graphDatabaseService;

    DogPopulationResourceConfigFactory(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication(graphDatabaseService);
    }
}
