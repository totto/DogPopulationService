package no.nkk.dogpopulation;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class WebModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebModule.class);

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public Server createJettyServer(DogPopulationJerseyApplication dogPopulationJerseyApplication, @Named("httpPort") int httpPort, @Named("maxThreads") int maxThreads, @Named("minThreads") int minThreads) {
        try {
            URI baseUri = UriBuilder.fromUri("http://localhost/").port(httpPort).build();
            // TODO: This way of initializing does _not_ honor the ApplicationPath annotation
            ResourceConfig config = ResourceConfig.forApplication(dogPopulationJerseyApplication);
            Server server = JettyHttpContainerFactory.createServer(baseUri, config);
            QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
            threadPool.setMaxThreads(maxThreads);
            threadPool.setMinThreads(minThreads);
            LOGGER.info("DogPopulationService started. Jetty is listening on port " + httpPort);
            return server;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}
