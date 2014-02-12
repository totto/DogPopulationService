package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nkk.dogpopulation.graph.Dog;
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

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/dogpopulation/pedigree")
public class PedigreeResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedigreeResource.class);

    private final ObjectMapper objectMapper;
    private final ObjectWriter prettyPrintingObjectWriter;

    private final PedigreeService pedigreeService;

    public PedigreeResource(GraphQueryService graphQueryService) {
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        this.pedigreeService = new PedigreeService(graphQueryService);
    }

    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedigree(@PathParam("uuid") String uuid) {
        LOGGER.trace("getPedigree for dog with uuid " + uuid);

        Dog dog = pedigreeService.getPedigree(uuid);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(dog);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
