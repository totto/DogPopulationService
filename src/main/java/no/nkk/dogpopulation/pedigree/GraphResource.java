package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.hdindex.DmuFiles;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.BreedImportStatus;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchBreedImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/dogpopulation/graph")
public class GraphResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphResource.class);

    private final ObjectMapper objectMapper;
    private final ObjectWriter prettyPrintingObjectWriter;

    private final GraphQueryService graphQueryService;

    private final Map<String, BreedImportStatus> breedImportStatus = new LinkedHashMap<>(); // keep references forever

    private final DogSearchBreedImporter dogSearchBreedImporter;

    public GraphResource(GraphQueryService graphQueryService, ExecutorService executorService, PedigreeImporter pedigreeImporter, DogSearchClient dogSearchClient) {
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        this.graphQueryService = graphQueryService;
        dogSearchBreedImporter = new DogSearchBreedImporter(executorService, pedigreeImporter, dogSearchClient);
    }

    @GET
    @Path("/breed/import")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportStatus() {
        LOGGER.trace("getImportStatus()");

        List<BreedImportStatus> statusList;
        synchronized (breedImportStatus) {
            statusList = new ArrayList<>(breedImportStatus.values());
        }

        BreedImportStatusAggregate statusAggregate = new BreedImportStatusAggregate(statusList);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(statusAggregate);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/breed/import/{breed}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response importBreedFromDogSearch(@PathParam("breed") String breed) {
        LOGGER.trace("importBreedFromDogSearch()");

        BreedImportStatus progress;
        boolean shouldImport = false;
        synchronized (breedImportStatus) {
            progress = breedImportStatus.get(breed);
            if (progress == null) {
                shouldImport = true;
                progress = new BreedImportStatus(breed);
                breedImportStatus.put(breed, progress);
            }
        }

        if (shouldImport) {
            dogSearchBreedImporter.importBreed(breed, progress);
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(progress);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    @GET
    @Path("/breed/{breed}/hddata")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDmuHdDataForBreed(@PathParam("breed") String breed) {
        LOGGER.trace("getDmuHdDataForBreed({})", breed);

        DmuFiles dmuFiles = graphQueryService.getDmuFiles(breed);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(dmuFiles);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
