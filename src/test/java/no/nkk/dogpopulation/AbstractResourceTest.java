package no.nkk.dogpopulation;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.dogbuilder.CommonNodes;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
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
    protected CommonNodes commonNodes;
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
                resourceConfig.registerInstances(new PedigreeResource(new PedigreeService(graphDb, new GraphQueryService(graphDb), new DogTestImporterFactory())));
                return resourceConfig;
            }
        };
        commonNodes = new CommonNodes(graphDb);
        dogs = new Dogs(commonNodes);
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
        return commonNodes.getBreed(breedName, null);
    }

    protected Node breed(String breedName, String breedId) {
        return commonNodes.getBreed(breedName, breedId);
    }

    protected Node addDog(String uuid, Node breedNode) {
        return addDog(uuid, uuid, breedNode);
    }

    protected Node addDog(String uuid, String name, Node breedNode) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = dogs.dog().uuid(uuid).name(name).breed(breedNode).build(graphDb);
            tx.success();
            return dog;
        }
    }

    protected Node addDog(String uuid, Node breedNode, LocalDate born) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = dogs.dog().uuid(uuid).name(uuid).breed(breedNode).born(born).build(graphDb);
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
