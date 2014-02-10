package no.nkk.dogpopulation.pedigree;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.ResourceConfigFactory;
import no.nkk.dogpopulation.graph.Dog;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.neo4j.graphdb.GraphDatabaseService;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeResourceTest {

    private Main main;

    @BeforeClass
    public void startServer() {
        int httpPort = 10000 + Main.DEFAULT_HTTP_PORT;
        String dbPath = "target/unittestdogdb";
        File dbFolder = new File(dbPath);
        FileUtils.deleteQuietly(dbFolder);
        final GraphDatabaseService graphDb = Main.createGraphDb(dbPath);
        ResourceConfigFactory resourceConfigFactory = new ResourceConfigFactory() {
            @Override
            public ResourceConfig createResourceConfig() {
                ResourceConfig resourceConfig = new ResourceConfig();
                resourceConfig.registerInstances(new PedigreeResource(new GraphQueryService(graphDb)));
                return resourceConfig;
            }
        };
        main = new Main(graphDb, resourceConfigFactory, httpPort);
        main.start();
        RestAssured.port = httpPort;

        String breed = "Rottweiler";
        String childUuid = "uuid-1234567890";
        String childName = "Wicked teeth Jr. III";
        String fatherUuid = "uuid-1234567891";
        String fatherName = "Wicked teeth Sr. II";
        String fathersFatherUuid = "uuid-1234567892";
        String fathersFatherName = "Wicked teeth I";
        String fathersMotherUuid = "uuid-1234567893";
        String fathersMotherName = "Daisy";
        String motherUuid = "uuid-1234567894";
        String motherName = "Tigerclaws";

        GraphAdminService graphAdminService = new GraphAdminService(graphDb);
        graphAdminService.addDog(childUuid, childName, breed);
        graphAdminService.addDog(fatherUuid, fatherName, breed);
        graphAdminService.connectChildToParent(childUuid, fatherUuid, ParentRole.FATHER);
        graphAdminService.addDog(fathersFatherUuid, fathersFatherName, breed);
        graphAdminService.connectChildToParent(fatherUuid, fathersFatherUuid, ParentRole.FATHER);
        graphAdminService.addDog(fathersMotherUuid, fathersMotherName, breed);
        graphAdminService.connectChildToParent(fatherUuid, fathersMotherUuid, ParentRole.MOTHER);
        graphAdminService.addDog(motherUuid, motherName, breed);
        graphAdminService.connectChildToParent(childUuid, motherUuid, ParentRole.MOTHER);
    }

    @AfterClass
    public void stopServer() throws Exception {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void thatPedigreeIsWellFormed() throws Exception {
        Dog dog = RestAssured.expect().statusCode(200).given().when().get("/pedigree/uuid-1234567891").as(Dog.class);
        String dogName = dog.getName();
        Assert.assertEquals(dogName, "Wicked teeth Sr. II");
    }

}
