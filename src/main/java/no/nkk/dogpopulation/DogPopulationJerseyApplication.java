package no.nkk.dogpopulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import no.nkk.dogpopulation.concurrent.ThreadingResource;
import no.nkk.dogpopulation.hdindex.HdIndexResource;
import no.nkk.dogpopulation.pedigree.GraphResource;
import no.nkk.dogpopulation.pedigree.PedigreeResource;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@ApplicationPath("dogpopulation")
@Singleton
public class DogPopulationJerseyApplication extends ResourceConfig {

    @Inject
    public DogPopulationJerseyApplication(
            PedigreeResource pedigreeResource,
            GraphResource graphResource,
            HdIndexResource hdIndexResource,
            ThreadingResource threadingResource) {

        registerInstances(
                pedigreeResource,
                graphResource,
                hdIndexResource,
                threadingResource);
    }
}