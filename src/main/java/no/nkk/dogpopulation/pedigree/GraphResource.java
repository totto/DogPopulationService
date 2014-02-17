package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/dogpopulation/graph")
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
    @Path("/breed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKnownBreeds() {
        LOGGER.trace("getKnownBreeds()");

        List<String> breeds = graphQueryService.getBreeds();

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(breeds);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/pedigreecompleteness")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedigreeCompletenessOfDogGroup(@QueryParam("generations") Integer generations, @QueryParam("breed") List<String> breed, @QueryParam("minYear") Integer minYear, @QueryParam("maxYear") Integer maxYear) {
        LOGGER.trace("getPedigreeCompletenessOfDogGroup({})", breed);

        if (generations == null) {
            generations = 6;
        }
        if (breed == null) {
            breed = new ArrayList<>();
        }
        if (minYear == null) {
            minYear = 0;
        }
        if (maxYear == null) {
            maxYear = Integer.MAX_VALUE;
        }
        PedigreeCompleteness breedOverview = graphQueryService.getPedigreeCompletenessOfGroup(generations, new LinkedHashSet<>(breed), minYear, maxYear);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(breedOverview);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/breed/{breed}/uuids")
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
