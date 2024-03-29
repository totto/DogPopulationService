package no.nkk.dogpopulation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.concurrent.ThreadingModule;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.Neo4jModule;
import no.nkk.dogpopulation.graph.dogbuilder.BreedSynonymNodeCache;
import no.nkk.dogpopulation.graph.dogbuilder.Dogs;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class AbstractResourceTest {

    @Inject
    protected Main main;
    @Inject
    protected GraphDatabaseService graphDb;
    @Inject
    protected BreedSynonymNodeCache breedSynonymNodeCache;
    @Inject
    protected Dogs dogs;
    @Inject
    protected ExecutorManager executorManager;

    @BeforeMethod
    public void startServer() {
        final Injector injector = Guice.createInjector(
                new UnittestModule(),
                new ThreadingModule(20),
                new Neo4jModule(),
                new WebModule()
        );
        injector.injectMembers(this);
        main.start();
    }

    @AfterMethod
    public void stopServer() throws Exception {
        if (main != null) {
            main.stop();
        }
        if (executorManager != null) {
            executorManager.shutdown();
        }
    }

    protected Node addBreed(String breed, String nkkBreedId) {
        Node breedSynonymNode = breedSynonymNodeCache.getBreed(breed);
        try (Transaction tx = graphDb.beginTx()) {
            Node breedNode = graphDb.createNode(DogGraphLabel.BREED);
            breedNode.setProperty(DogGraphConstants.BREED_BREED_NAME, breed);
            breedNode.setProperty(DogGraphConstants.BREED_NKK_BREED_ID, nkkBreedId);
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

    protected Relationship connectChildToMother(String childUuid, String fatherUuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Relationship hasMother = dogs.hasParent().child(childUuid).mother(fatherUuid).build(graphDb);
            tx.success();
            return hasMother;
        }
    }
}
