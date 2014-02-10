package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nkk.dogpopulation.graph.GraphQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/graph")
public class GraphResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphResource.class);

    private final ObjectMapper objectMapper;
    private final ObjectWriter prettyPrintingObjectWriter;

    private final GraphQueryService graphQueryService;

    public GraphResource(GraphQueryService graphQueryService) {
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        this.graphQueryService = graphQueryService;
    }

    @GET
    @Path("/breed/{breed}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDogsForBreed(@PathParam("breed") String breed) {
        LOGGER.trace("getDogsForBreed({})", breed);

        List<String> uuids = graphQueryService.getBreedList(breed);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(uuids);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
