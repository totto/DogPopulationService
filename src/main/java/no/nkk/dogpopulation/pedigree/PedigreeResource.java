package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
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

    public PedigreeResource(PedigreeService pedigreeService) {
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        this.pedigreeService = pedigreeService;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedigree(@PathParam("id") String id) {
        LOGGER.trace("getPedigree for dog with id " + id);

        TopLevelDog dog = pedigreeService.getPedigree(id);

        if (dog == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(dog);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/fictitious")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFictitiousPedigree(@QueryParam("father") String fatherUuid, @QueryParam("mother") String motherUuid) {
        LOGGER.trace("getFictitiousPedigree({}, {})", fatherUuid, motherUuid);

        TopLevelDog dog = pedigreeService.getFicticiousPedigree("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", "Fictitious", fatherUuid, motherUuid);

        if (dog == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(dog);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
