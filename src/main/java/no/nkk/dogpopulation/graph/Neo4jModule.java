package no.nkk.dogpopulation.graph;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Neo4jModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jModule.class);

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public ExecutionEngine createExecutionEngine(GraphDatabaseService graphDb) {
        return new ExecutionEngine(graphDb);
    }

    @Provides
    @Singleton
    public GraphDatabaseService createGraphDb(@Named("neo4jFolder") String dbFolder) {
        // initialize embedded neo4j graph database
        URL neo4jPropertiesUrl = ClassLoader.getSystemClassLoader().getResource("neo4j.properties");
        long startTime = System.currentTimeMillis();
        LOGGER.debug("Neo4j Initializing...");
        GraphDatabaseBuilder graphDatabaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFolder);
        graphDatabaseBuilder.loadPropertiesFromURL(neo4jPropertiesUrl); // load default configuration
        File neo4jConfigOverrideFile = new File("neo4j.properties");
        if (neo4jConfigOverrideFile.isFile()) {
            // load neo4j override settings
            try {
                graphDatabaseBuilder.loadPropertiesFromURL(neo4jConfigOverrideFile.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        final GraphDatabaseService graphDatabaseService = graphDatabaseBuilder.newGraphDatabase();
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

    private void createUniqueConstraintIfNeeded(Schema schema, Label label, String property) {
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

    private void createIndexIfNeeded(Schema schema, Label label, String property) {
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
}
