package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nkk.dogpopulation.dogsearch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/pedigree")
public class PedigreeResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedigreeResource.class);

    private final DogSearchClient dogSearchClient;

    private final ObjectMapper objectMapper;
    private final ObjectWriter prettyPrintingObjectWriter;

    public PedigreeResource(DogSearchClient dogSearchClient) {
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        this.dogSearchClient = dogSearchClient;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedigree(@QueryParam("q") String query) {
        LOGGER.trace("getPedigree for query " + query);

        DogSearchResponse dogSearchResponse = dogSearchClient.findDog(query);

        if (dogSearchResponse == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        DogSearchResponseBody dogSearchResponseBody = dogSearchResponse.getResponse();
        DogDocument[] dogDocuments = dogSearchResponseBody.getDocs();
        DogDocument dogDocument = dogDocuments[0];
        DogDetails dogDetails = dogDocument.toDogDetails(objectMapper);

        Dog dog = new Dog(dogDetails);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(dog);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
