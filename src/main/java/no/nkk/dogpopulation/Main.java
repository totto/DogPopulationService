package no.nkk.dogpopulation;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchImporter;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
            createIndexIfNeeded(schema, DogGraphLabel.BREED, DogGraphConstants.BREED_BREED);
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

        GraphDatabaseService db = createGraphDb("data/dogdb");
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(200);
            DogSearchClient dogSearchClient = new DogSearchSolrClient("http://dogsearch.nkk.no/dogservice/dogs");

            Main main = new Main(db, new DogPopulationResourceConfigFactory(db, new DogSearchImporter(executorService, db, dogSearchClient)));
            main.start();

            if (args.length > 0 && args[0].equalsIgnoreCase("--import")) {
                String[] suboptions = Arrays.copyOfRange(args, 1, args.length);
                int statusCode = main.mainImport(db, executorService, dogSearchClient, suboptions);
                main.stop();
                db.shutdown();
                System.exit(statusCode);
                return; // always exit after import
            }

            main.join();
        } finally {
            db.shutdown();
        }
    }


    public static int mainImport(GraphDatabaseService db, ExecutorService executorService, DogSearchClient dogSearchClient, String... args) {
        if (args.length < 2) {
            printUsage();
            return 1;
        }

        Set<String> breeds = new LinkedHashSet<>();
        Set<String> ids = new LinkedHashSet<>();

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--id") || arg.equalsIgnoreCase("--ids")) {
                if ((i+1) >= args.length) {
                    System.out.println("At least one argument must follow option: " + arg);
                    printUsage();
                    return 1;
                }
                String nextArg = args[i+1];
                if (nextArg.equals("--")) {
                    ids.addAll(readAndTrimLinesFromStandardInput());
                } else {
                    for (int j=i+1; j<args.length; j++) {
                        ids.add(args[j].trim());
                    }
                }
                break;
            }
            if (arg.equalsIgnoreCase("--breed") | arg.equalsIgnoreCase("--breeds")) {
                if ((i+1) >= args.length) {
                    System.out.println("At least one argument must follow option: " + arg);
                    printUsage();
                    return 1;
                }
                String nextArg = args[i+1];
                if (nextArg.equals("--")) {
                    breeds.addAll(readAndTrimLinesFromStandardInput());
                } else {
                    for (int j=i+1; j<args.length; j++) {
                        breeds.add(args[j].trim());
                    }
                }
                break;
            }
        }

        List<Future<?>> importCompleteFuture = new ArrayList<>();
        DogSearchImporter dogImporter = new DogSearchImporter(executorService, db, dogSearchClient);
        for (final String id : ids) {
            importCompleteFuture.add(dogImporter.importDog(id));
        }
        for (final String breed : breeds) {
            importCompleteFuture.add(dogImporter.importBreed(breed));
        }

        // wait for all imports to complete
        for (Future<?> f : importCompleteFuture) {
            try {
                f.get();
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            LOGGER.error("", e);
        }

        return 0;
    }

    private static Collection<? extends String> readAndTrimLinesFromStandardInput() {
        Set<String> result = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) {
                    continue; // ignore empty lines
                }
                result.add(trimmedLine);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static void printUsage() {
        System.out.println("DogPopulationService was invoked with --import. This will start the service and import the pedigree(s) of specific dog(s) or breed(s) from dogsearch into a graph-database.");
        System.out.println("Import SUB-OPTIONS:");
        System.out.println("    --id --                  Import a line separated list of dogs from standard input. Each line will be matched against UUID and RegNo in dogsearch.");
        System.out.println("    --id <UUID|RegNo>...     Import a list of dogs supplied as arguments. Each argument will be matched against UUID and RegNo in dogsearch.");
        System.out.println("    --breed --               Import all dogs known to dogsearch within a line separated list of breeds from standard input.");
        System.out.println("    --breed <breed>...       Import all dogs known to dogsearch within a list of breeds supplied as arguments");
    }
}
