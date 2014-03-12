package no.nkk.dogpopulation;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import no.nkk.dogpopulation.hdindex.HdIndexResource;
import no.nkk.dogpopulation.importer.dogsearch.DogTestImporterFactory;
import no.nkk.dogpopulation.pedigree.PedigreeResource;
import no.nkk.dogpopulation.pedigree.PedigreeService;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class AbstractResourceTest {
    protected Main main;
    protected GraphDatabaseService graphDb;
    protected BreedSynonymNodeCache breedSynonymNodeCache;
    protected Dogs dogs;

    @BeforeClass
    public void startServer() {
        int httpPort = 10000 + Main.DEFAULT_HTTP_PORT;
        String dbPath = "target/unittestdogdb";
        File dbFolder = new File(dbPath);
        FileUtils.deleteQuietly(dbFolder);
        graphDb = Main.createGraphDb(dbPath);
        ResourceConfigFactory resourceConfigFactory = new ResourceConfigFactory() {
            @Override
            public ResourceConfig createResourceConfig() {
                ResourceConfig resourceConfig = new ResourceConfig();
                GraphQueryService graphQueryService = new GraphQueryService(graphDb);
                resourceConfig.registerInstances(new PedigreeResource(new PedigreeService(graphDb, graphQueryService, new DogTestImporterFactory())));
                resourceConfig.registerInstances(new HdIndexResource(graphQueryService, "target/hdindex-test"));
                return resourceConfig;
            }
        };
        breedSynonymNodeCache = new BreedSynonymNodeCache(graphDb);
        dogs = new Dogs(breedSynonymNodeCache);
        main = new Main(graphDb, resourceConfigFactory, httpPort);
        main.start();
        RestAssured.port = httpPort;
    }

    @AfterClass
    public void stopServer() throws Exception {
        if (main != null) {
            main.stop();
        }
    }


    protected Node breed(String breedName) {
        return breedSynonymNodeCache.getBreed(breedName);
    }

    protected Node addDog(String uuid, Node breedSynonymNode) {
        return addDog(uuid, uuid, breedSynonymNode);
    }

    protected Node addDog(String uuid, String name, Node breedSynonymNode) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = dogs.dog().uuid(uuid).name(name).breed(breedSynonymNode).build(graphDb);
            tx.success();
            return dog;
        }
    }

    protected Node addDog(String uuid, Node breedSynonymNode, LocalDate born) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = dogs.dog().uuid(uuid).name(uuid).breed(breedSynonymNode).born(born).build(graphDb);
            tx.success();
            return dog;
        }
    }

    protected Relationship connectChildToFather(String childUuid, String fatherUuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Relationship hasFather = dogs.hasParent().child(childUuid).father(fatherUuid).build(graphDb);
            tx.success();
            return hasFather;
        }
    }

    protected Relationship connectChildToMother(String childUuid, String fatherUuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Relationship hasMother = dogs.hasParent().child(childUuid).mother(fatherUuid).build(graphDb);
            tx.success();
            return hasMother;
        }
    }
}
