package no.nkk.dogpopulation;

import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.concurrent.ThreadingResource;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.hdindex.HdIndexResource;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.pedigree.GraphResource;
import no.nkk.dogpopulation.pedigree.PedigreeResource;
import no.nkk.dogpopulation.pedigree.PedigreeService;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.ApplicationPath;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@ApplicationPath("dogpopulation")
public class DogPopulationJerseyApplication extends ResourceConfig {

    public DogPopulationJerseyApplication(GraphDatabaseService graphDb, ExecutorManager executorManager, DogSearchClient dogSearchClient, PedigreeImporterFactory pedigreeImporterFactory) {
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);
        PedigreeImporter pedigreeImporter = pedigreeImporterFactory.createInstance(new BulkWriteService(executorManager.getExecutor(ExecutorManager.BULK_WRITER_MAP_KEY), graphDb).start());
        PedigreeService pedigreeService = new PedigreeService(graphDb, graphQueryService, pedigreeImporter);

        registerInstances(
                new PedigreeResource(pedigreeService),
                new GraphResource(graphDb, graphQueryService, executorManager, pedigreeImporterFactory, dogSearchClient, pedigreeImporter),
                new HdIndexResource(graphQueryService, "hdindex"),
                new ThreadingResource(executorManager));
    }
}