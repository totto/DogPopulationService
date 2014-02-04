package no.nkk.dogpopulation.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchHttpClient implements DogSearchClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String dogSearchUri;
    private final String dogSearchContextPath;
    private final Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();

    public DogSearchHttpClient() {
        this.dogSearchUri = "http://dogsearch.nkk.no";
        this.dogSearchContextPath = "/dogservice/dogs/select";
    }

    @Override
    public DogSearchResponse findDog(String query) {
        WebTarget configTarget = client.target(dogSearchUri).path(dogSearchContextPath).queryParam("q", query).queryParam("wt", "json");
        Response response = configTarget.request(MediaType.TEXT_PLAIN_TYPE).get();
        boolean success = handleResponseStatus(response);
        if (!success) {
            return null;
        }
        String json = response.readEntity(String.class);
        DogSearchResponse dogSearchResponse;
        try {
            dogSearchResponse = objectMapper.readValue(json, DogSearchResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dogSearchResponse;
    }

    private boolean handleResponseStatus(Response response) {
        switch(response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                return true;
            case CLIENT_ERROR:
            case INFORMATIONAL:
            case OTHER:
            case REDIRECTION:
            case SERVER_ERROR:
            default:
                LOGGER.warn("Request failed. status={} {}, location={}", response.getStatus(), response.getStatusInfo().getReasonPhrase(), response.getLocation());
        }
        return false;
    }
}
