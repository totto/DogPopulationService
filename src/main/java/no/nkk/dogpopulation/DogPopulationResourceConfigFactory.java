package no.nkk.dogpopulation;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogPopulationResourceConfigFactory implements ResourceConfigFactory {
    @Override
    public ResourceConfig createResourceConfig() {
        return new DogPopulationJerseyApplication();
    }
}
