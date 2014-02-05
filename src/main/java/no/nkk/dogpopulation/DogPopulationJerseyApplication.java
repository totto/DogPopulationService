package no.nkk.dogpopulation;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.pedigree.PedigreeResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;

import javax.ws.rs.ApplicationPath;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@ApplicationPath("dogpopulation")
public class DogPopulationJerseyApplication extends ResourceConfig {
    public DogPopulationJerseyApplication(GraphDatabaseService graphDb) {
        registerInstances(new PedigreeResource(new GraphQueryService(graphDb)));
    }
}