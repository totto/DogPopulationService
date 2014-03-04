package no.nkk.dogpopulation;

import no.nkk.dogpopulation.graph.dogbuilder.CommonNodes;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractGraphTest {

    protected GraphDatabaseService graphDb;
    private CommonNodes commonNodes;
    protected Dogs dogs;

    @BeforeMethod
    public void initGraph() {
        String dbPath = "target/unittestdogdb";
        File dbFolder = new File(dbPath);
        FileUtils.deleteQuietly(dbFolder);
        graphDb = Main.createGraphDb(dbPath);
        commonNodes = new CommonNodes(graphDb);
        dogs = new Dogs(commonNodes);
    }

    @AfterMethod
    public void closeGraph() {
        graphDb.shutdown();
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

    protected void setProperty(Node node, String key, Object value) {
        try (Transaction tx = graphDb.beginTx()) {
            node.setProperty(key, value);
            tx.success();
        }
    }
}
