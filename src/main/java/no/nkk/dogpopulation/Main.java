package no.nkk.dogpopulation;

import no.nkk.dogpopulation.breedgroupimport.BreedGroupJsonImporter;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.GraphSchemaMigration;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchPedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchSolrClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            graphDb.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static GraphDatabaseService createGraphDb(String dogDbFolder) {
        // initialize embedded neo4j graph database
        URL neo4jPropertiesUrl = ClassLoader.getSystemClassLoader().getResource("neo4j.properties");
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Neo4j Initializing...");
        GraphDatabaseService graphDatabaseService = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(dogDbFolder)
                .loadPropertiesFromURL(neo4jPropertiesUrl)
                .newGraphDatabase();
        long graphDbDelay = System.currentTimeMillis() - startTime;
        LOGGER.debug("Neo4j core engine up in {} seconds, initializing indexes...", graphDbDelay / 1000);
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Schema schema = graphDatabaseService.schema();
            startTime = System.currentTimeMillis();
            createIndexIfNeeded(schema, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY);
            createIndexIfNeeded(schema, DogGraphLabel.BREED, DogGraphConstants.BREEDSYNONYM_SYNONYM);
            createIndexIfNeeded(schema, DogGraphLabel.DOG, DogGraphConstants.DOG_NAME);
            createIndexIfNeeded(schema, DogGraphLabel.DOG, DogGraphConstants.DOG_REGNO);
            createUniqueConstraintIfNeeded(schema, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID);
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Schema schema = graphDatabaseService.schema();
            schema.awaitIndexesOnline(30, TimeUnit.MINUTES);
            tx.success();
        }
        long indexDelay = System.currentTimeMillis() - startTime;
        LOGGER.debug("Neo4j indexes initialized in {} seconds", indexDelay / 1000);
        return graphDatabaseService;
    }

    private static void createUniqueConstraintIfNeeded(Schema schema, Label label, String property) {
        Set<String> indexedConstraints = new LinkedHashSet<>();
        for (ConstraintDefinition cdef : schema.getConstraints(label)) {
            for (String propertyKey : cdef.getPropertyKeys()) {
                indexedConstraints.add(propertyKey);
            }
        }
        if (!indexedConstraints.contains(property)) {
            schema.constraintFor(label).assertPropertyIsUnique(property).create();
        }
    }

    private static void createIndexIfNeeded(Schema schema, Label label, String property) {
        Set<String> indexedProperties = new LinkedHashSet<>();
        for (IndexDefinition idef : schema.getIndexes(label)) {
            for (String propertyKey : idef.getPropertyKeys()) {
                indexedProperties.add(propertyKey);
            }
        }
        if (!indexedProperties.contains(property)) {
            schema.indexFor(label).on(property).create();
        }
    }

    public static void main(String... args) {
        // Bridge Jersey from java.util.logging to slf4
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final GraphDatabaseService db = createGraphDb("data/dogdb");
        try {
            GraphSchemaMigration.migrateSchema(db);
            updateBreedGroups(db);

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    db.shutdown();
                }
            }));
            ExecutorService executorService = Executors.newFixedThreadPool(300);
            DogSearchClient dogSearchClient = new DogSearchSolrClient("http://dogsearch.nkk.no/dogservice/dogs");

            BreedSynonymNodeCache breedSynonymNodeCache = new BreedSynonymNodeCache(db);
            Dogs dogs = new Dogs(breedSynonymNodeCache);
            DogSearchPedigreeImporterFactory dogImporterFactory = new DogSearchPedigreeImporterFactory(executorService, db, dogSearchClient, dogs, breedSynonymNodeCache);

            Main main = new Main(db, new DogPopulationResourceConfigFactory(db, dogImporterFactory, executorService, dogSearchClient));
            main.start();
            main.join();
        } finally {
            db.shutdown();
        }
    }

    private static void updateBreedGroups(GraphDatabaseService db) {
        URL url = null;
        try {
            url = new URL("http://dogid.nkk.no/ras/Raser.json");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        BreedGroupJsonImporter importer = new BreedGroupJsonImporter(url, db);
        importer.importBreedGroup();
    }

}
