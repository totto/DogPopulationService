package no.nkk.dogpopulation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import no.nkk.dogpopulation.breedgroupimport.BreedGroupJsonImporter;
import no.nkk.dogpopulation.concurrent.ThreadingModule;
import no.nkk.dogpopulation.graph.GraphSchemaMigrator;
import no.nkk.dogpopulation.graph.Neo4jModule;
import no.nkk.dogpopulation.importer.breedupdater.BreedUpdateService;
import org.eclipse.jetty.server.Server;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final Server server;
    private final GraphDatabaseService graphDb;

    @Inject
    public Main(Server server, GraphDatabaseService graphDb) {
        this.server = server;
        this.graphDb = graphDb;
    }

    public void start() {
        try {
            server.start();
        } catch (RuntimeException e) {
            throw e;
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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        long startTime = System.currentTimeMillis();

        // Bridge Jersey from java.util.logging to slf4
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final Injector injector = Guice.createInjector(
                new ConfigurationModule(),
                new ThreadingModule(),
                new Neo4jModule(),
                new WebModule()
        );

        final GraphDatabaseService db = injector.getInstance(GraphDatabaseService.class);
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    db.shutdown();
                }
            }));

            GraphSchemaMigrator graphSchemaMigrator = injector.getInstance(GraphSchemaMigrator.class);
            graphSchemaMigrator.migrateSchema(db);

            BreedGroupJsonImporter breedGroupJsonImporter = injector.getInstance(BreedGroupJsonImporter.class);
            breedGroupJsonImporter.importBreedGroup();

            Main main = injector.getInstance(Main.class);
            main.start();

            BreedUpdateService breedUpdateService = injector.getInstance(BreedUpdateService.class);
            breedUpdateService.initializeRecurringUpdates();

            long durationMs = System.currentTimeMillis() - startTime;
            LOGGER.info("DogPopulation application started in {} seconds", Math.round(durationMs / 1000.0));

            main.join();
        } finally {
            db.shutdown();
        }
    }

}
