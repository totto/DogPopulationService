package no.nkk.dogpopulation;

import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final PedigreeImporter pedigreeImporter;

    DogPopulationResourceConfigFactory(GraphDatabaseService graphDatabaseService, PedigreeImporter pedigreeImporter) {
        this.graphDatabaseService = graphDatabaseService;
        this.pedigreeImporter = pedigreeImporter;
    }

    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication(graphDatabaseService, pedigreeImporter);
    }
}
