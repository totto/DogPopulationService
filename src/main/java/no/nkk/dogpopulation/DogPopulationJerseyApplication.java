package no.nkk.dogpopulation;

import no.nkk.dogpopulation.dogsearch.DogSearchHttpClient;
import no.nkk.dogpopulation.pedigree.PedigreeResource;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@ApplicationPath("dogpopulation")
public class DogPopulationJerseyApplication extends ResourceConfig {
    public DogPopulationJerseyApplication() {
        registerInstances(new PedigreeResource(new DogSearchHttpClient()));
    }
}