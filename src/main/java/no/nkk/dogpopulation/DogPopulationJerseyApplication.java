package no.nkk.dogpopulation;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.importer.DogImporter;
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
    public DogPopulationJerseyApplication(GraphDatabaseService graphDb, DogImporter dogImporter) {
        GraphQueryService graphQueryService = new GraphQueryService(graphDb);
        PedigreeService pedigreeService = new PedigreeService(graphQueryService, dogImporter);
        registerInstances(new PedigreeResource(pedigreeService), new GraphResource(graphQueryService));
    }
}