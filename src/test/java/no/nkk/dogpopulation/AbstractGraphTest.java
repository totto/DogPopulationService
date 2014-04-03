package no.nkk.dogpopulation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import no.nkk.dogpopulation.graph.*;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import org.joda.time.LocalDate;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public abstract class AbstractGraphTest {

    @Inject
    protected GraphDatabaseService graphDb;
    @Inject
    protected BreedSynonymNodeCache breedSynonymNodeCache;
    @Inject
    protected Dogs dogs;
    @Inject
    protected ExecutionEngine executionEngine;
    @Inject
    protected GraphQueryService graphQueryService;


    @BeforeMethod
    public void initGraph() {
        final Injector injector = Guice.createInjector(
                new UnittestModule(),
                new Neo4jModule()
        );
        injector.injectMembers(this);
    }

    @AfterMethod
    public void closeGraph() {
        graphDb.shutdown();
    }

    protected Node addBreed(String synonym, String nkkBreedId) {
        Node breedSynonymNode = breedSynonymNodeCache.getBreed(synonym);
        try (Transaction tx = graphDb.beginTx()) {
            Node breedNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED, DogGraphConstants.BREED_NKK_BREED_ID, nkkBreedId);
            breedNode.setProperty(DogGraphConstants.BREED_BREED_NAME, synonym);
            breedSynonymNode.createRelationshipTo(breedNode, DogGraphRelationshipType.MEMBER_OF);
            tx.success();
        }
        return breedSynonymNode;
    }

    protected Node breed(String breedName) {
        return breedSynonymNodeCache.getBreed(breedName);
    }

    protected Node addDog(String uuid, Node breedSynonymNode) {
        return addDog(uuid, uuid, breedSynonymNode);
    }

    protected Node addDog(String uuid, String name, Node breedSynonymNode) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = dogs.dog(uuid).name(name).breed(breedSynonymNode).build(graphDb);
            tx.success();
            return dog;
        }
    }

    protected Node addDog(String uuid, Node breedSynonymNode, LocalDate born) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = dogs.dog(uuid).name(uuid).breed(breedSynonymNode).born(born).build(graphDb);
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

    protected Relationship connectChildToMother(String childUuid, String motherUuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Relationship hasMother = dogs.hasParent().child(childUuid).mother(motherUuid).build(graphDb);
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
