package no.nkk.dogpopulation;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.pedigree.GraphResource;
import no.nkk.dogpopulation.pedigree.PedigreeResource;
import no.nkk.dogpopulation.pedigree.PedigreeService;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.ApplicationPath;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@ApplicationPath("dogpopulation")
public class DogPopulationJerseyApplication extends ResourceConfig {
    public DogPopulationJerseyApplication(GraphDatabaseService graphDb, PedigreeImporter pedigreeImporter, ExecutorService executorService, DogSearchClient dogSearchClient) {
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);
        PedigreeService pedigreeService = new PedigreeService(graphQueryService, pedigreeImporter);

        registerInstances(new PedigreeResource(pedigreeService), new GraphResource(graphQueryService, executorService, pedigreeImporter, dogSearchClient));
    }
}