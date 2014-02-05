package no.nkk.dogpopulation.pedigree;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.ResourceConfigFactory;
import no.nkk.dogpopulation.graph.Dog;
import no.nkk.dogpopulation.graph.GraphQueryService;
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
    }

    @AfterClass
    public void stopServer() throws Exception {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void thatPedigreeIsWellFormed() throws Exception {
        Dog dog = RestAssured.expect().statusCode(200).given().parameter("q", "S38454/2006").when().get("/pedigree").as(Dog.class);
        String dogName = dog.getName();
        Assert.assertEquals(dogName, "A Heaven Sent Habib Ul-Lah");
    }

}
