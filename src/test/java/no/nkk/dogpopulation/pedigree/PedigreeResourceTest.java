package no.nkk.dogpopulation.pedigree;

import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.DogPopulationResourceConfigFactory;
import no.nkk.dogpopulation.Main;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeResourceTest {

    private Main main;

    @BeforeClass
    public void startServer() {
        int httpPort = 10000 + Main.DEFAULT_HTTP_PORT;
        main = new Main(new DogPopulationResourceConfigFactory(), httpPort);
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
        RestAssured.expect().statusCode(200).when().get("/pedigree");
    }

}
