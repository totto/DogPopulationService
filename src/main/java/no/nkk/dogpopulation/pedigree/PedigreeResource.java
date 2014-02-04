package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/pedigree")
public class PedigreeResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PedigreeResource.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PedigreeResource() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedigree(@QueryParam("dogid") String dogid) {
        LOGGER.trace("getPedigree for dogid " + dogid);
        return Response.ok("PONG " + PedigreeResource.class.getSimpleName()).build();
    }

}
