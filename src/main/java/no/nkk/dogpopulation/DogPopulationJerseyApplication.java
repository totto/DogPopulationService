package no.nkk.dogpopulation;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.hdindex.HdIndexResource;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
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
    public DogPopulationJerseyApplication(GraphDatabaseService graphDb, ExecutorService executorService, DogSearchClient dogSearchClient, PedigreeImporterFactory pedigreeImporterFactory) {
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);
        PedigreeService pedigreeService = new PedigreeService(graphDb, graphQueryService, pedigreeImporterFactory);

        registerInstances(
                new PedigreeResource(pedigreeService),
                new GraphResource(graphDb, graphQueryService, executorService, pedigreeImporterFactory, dogSearchClient),
                new HdIndexResource(graphQueryService));
    }
}