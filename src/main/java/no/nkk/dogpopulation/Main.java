package no.nkk.dogpopulation;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Main {

    public static final int DEFAULT_HTTP_PORT = 8051;
    public static final int DEFAULT_MAX_THREADS = 50;
    public static final int DEFAULT_MIN_THREADS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final ResourceConfigFactory resourceConfigFactory;

    private final int httpPort;
    private final int maxThreads;
    private final int minThreads;

    private Server server;

    private final GraphDatabaseService graphDb;

    public Main(GraphDatabaseService graphDb, ResourceConfigFactory resourceConfigFactory) {
        this(graphDb, resourceConfigFactory, DEFAULT_HTTP_PORT);
    }

    public Main(GraphDatabaseService graphDb, ResourceConfigFactory resourceConfigFactory, int httpPort) {
        this(graphDb, resourceConfigFactory, httpPort, DEFAULT_MAX_THREADS, DEFAULT_MIN_THREADS);
    }

    public Main(GraphDatabaseService graphDb, ResourceConfigFactory resourceConfigFactory, int httpPort, int maxThreads, int minThreads) {
        this.resourceConfigFactory = resourceConfigFactory;
        this.httpPort = httpPort;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.graphDb = graphDb;
    }

    public void start() {
        try {
            URI baseUri = UriBuilder.fromUri("http://localhost/").port(httpPort).build();
            // TODO: This way of initializing does _not_ honor the ApplicationPath annotation
            ResourceConfig resourceConfig = resourceConfigFactory.createResourceConfig();
            ResourceConfig config = ResourceConfig.forApplication(resourceConfig);
            server = JettyHttpContainerFactory.createServer(baseUri, config);
            QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
            threadPool.setMaxThreads(maxThreads);
            threadPool.setMinThreads(minThreads);
            LOGGER.info("DogPopulationService started. Jetty is listening on port " + httpPort);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void join() {
        try {
            server.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    public static GraphDatabaseService createGraphDb(String dogDbFolder) {
        // initialize embedded neo4j graph database
        URL neo4jPropertiesUrl = ClassLoader.getSystemClassLoader().getResource("neo4j.properties");
        return new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(dogDbFolder)
                .loadPropertiesFromURL(neo4jPropertiesUrl)
                .newGraphDatabase();
    }

    public static void main(String... args) {
        // Bridge Jersey from java.util.logging to slf4
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        GraphDatabaseService db = createGraphDb("data/dogdb");
        Main main = new Main(db, new DogPopulationResourceConfigFactory(db));
        main.start();
        main.join();
    }

}
