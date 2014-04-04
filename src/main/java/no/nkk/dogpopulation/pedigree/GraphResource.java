package no.nkk.dogpopulation.pedigree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.dataerror.breed.IncorrectBreedRecord;
import no.nkk.dogpopulation.graph.dataerror.circularparentchain.CircularRecord;
import no.nkk.dogpopulation.graph.dataerror.gender.IncorrectGenderRecord;
import no.nkk.dogpopulation.graph.hdxray.HDXrayStatistics;
import no.nkk.dogpopulation.graph.inbreeding.InbreedingOfGroup;
import no.nkk.dogpopulation.graph.litter.LitterStatistics;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.breedupdater.BreedImportStatus;
import no.nkk.dogpopulation.importer.breedupdater.BreedImportStatusAggregate;
import no.nkk.dogpopulation.importer.breedupdater.BreedUpdateService;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.UpdatesImporterTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/dogpopulation/graph")
@Singleton
public class GraphResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphResource.class);

    private final ObjectMapper objectMapper;
    private final ObjectWriter prettyPrintingObjectWriter;

    private final GraphQueryService graphQueryService;
    private final BreedUpdateService breedUpdateService;

    private final DogSearchClient dogSearchClient;

    private final PedigreeImporter pedigreeImporter;

    private final ExecutorService updatesImporterExecutorService;

    @Inject
    public GraphResource(
            @Named(ExecutorManager.UPDATES_IMPORTER_MAP_KEY) ExecutorService updatesImporterExecutorService,
            GraphQueryService graphQueryService, BreedUpdateService breedUpdateService, DogSearchClient dogSearchClient,
            PedigreeImporter pedigreeImporter) {
        this.breedUpdateService = breedUpdateService;
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        this.updatesImporterExecutorService = updatesImporterExecutorService;
        this.graphQueryService = graphQueryService;
        this.dogSearchClient = dogSearchClient;
        this.pedigreeImporter = pedigreeImporter;
    }


    @GET
    @Path("/import/dog/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reimportDog(@PathParam("id") String id) {
        LOGGER.trace("reimportDog for dog with id " + id);

        pedigreeImporter.importDogPedigree(id);

        TopLevelDog dog = graphQueryService.getPedigree(id);

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
    @Path("/import/latest/week")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLastWeek() {
        LOGGER.trace("updateLastWeek");

        Set<String> uuids = dogSearchClient.listIdsForLastWeek();

        UpdatesImporterTask updatesImporterTask = new UpdatesImporterTask(updatesImporterExecutorService, pedigreeImporter, dogSearchClient, uuids);
        int n = updatesImporterTask.call();

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(n);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/import/latest/day")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLastDay() {
        LOGGER.trace("updateLastDay");

        Set<String> uuids = dogSearchClient.listIdsForLastDay();

        UpdatesImporterTask updatesImporterTask = new UpdatesImporterTask(updatesImporterExecutorService, pedigreeImporter, dogSearchClient, uuids);
        int n = updatesImporterTask.call();

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(n);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/import/latest/hour")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLastHour() {
        LOGGER.trace("updateLastHour");

        Set<String> uuids = dogSearchClient.listIdsForLastHour();

        UpdatesImporterTask updatesImporterTask = new UpdatesImporterTask(updatesImporterExecutorService, pedigreeImporter, dogSearchClient, uuids);
        int n = updatesImporterTask.call();

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(n);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/import/latest/minute")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLastMinute() {
        LOGGER.trace("updateLastMinute");

        Set<String> uuids = dogSearchClient.listIdsForLastMinute();

        UpdatesImporterTask updatesImporterTask = new UpdatesImporterTask(updatesImporterExecutorService, pedigreeImporter, dogSearchClient, uuids);
        int n = updatesImporterTask.call();

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(n);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/breed/import")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImportStatus() {
        LOGGER.trace("getImportStatus()");

        BreedImportStatusAggregate statusAggregate = breedUpdateService.statusAggregate();

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

        BreedImportStatus progress = breedUpdateService.importBreed(breed, 24 * 60 * 60);

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
    @Path("/hdstatistics/bornyear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHdXrayStatisticsOfDogGroupBornBetween(@QueryParam("minYear") Integer minYear, @QueryParam("maxYear") Integer maxYear, @QueryParam("breed") List<String> breed) {
        LOGGER.trace("getHdXrayStatisticsOfDogGroupBornBetween({}, {}, {})", minYear, maxYear, breed);

        if (breed == null) {
            breed = new ArrayList<>();
        }
        if (minYear == null) {
            minYear = 0;
        }
        if (maxYear == null) {
            maxYear = Integer.MAX_VALUE;
        }
        HDXrayStatistics statistics = graphQueryService.getHDXrayStatisticsOfGroupBornBetween(new LinkedHashSet<>(breed), minYear, maxYear);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(statistics);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/hdstatistics/xrayyear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHdXrayStatisticsOfDogGroupHdXrayedBetween(@QueryParam("minYear") Integer minYear, @QueryParam("maxYear") Integer maxYear, @QueryParam("breed") List<String> breed) {
        LOGGER.trace("getHdXrayStatisticsOfDogGroupHdXrayedBetween({}, {}, {})", minYear, maxYear, breed);

        if (breed == null) {
            breed = new ArrayList<>();
        }
        if (minYear == null) {
            minYear = 0;
        }
        if (maxYear == null) {
            maxYear = Integer.MAX_VALUE;
        }
        HDXrayStatistics statistics = graphQueryService.getHDXrayStatisticsOfGroupHdXRayedBetween(new LinkedHashSet<>(breed), minYear, maxYear);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(statistics);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/inbreeding")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInbreedingOfDogGroup(@QueryParam("generations") Integer generations, @QueryParam("breed") List<String> breed, @QueryParam("minYear") Integer minYear, @QueryParam("maxYear") Integer maxYear) {
        LOGGER.trace("getInbreedingOfDogGroup({})", breed);

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
        InbreedingOfGroup inbreedingOfGroup = graphQueryService.getInbreedingOfGroup(generations, new LinkedHashSet<>(breed), minYear, maxYear);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(inbreedingOfGroup);
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
    @Path("/litter")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLitterOfDogGroup(@QueryParam("breed") List<String> breed, @QueryParam("minYear") Integer minYear, @QueryParam("maxYear") Integer maxYear) {
        LOGGER.trace("getInbreedingOfDogGroup({})", breed);

        if (breed == null || breed.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (minYear == null) {
            minYear = 0;
        }
        if (maxYear == null) {
            maxYear = 9999;
        }
        LitterStatistics litterStatistics = graphQueryService.getLitterStatisticsOfGroup(new LinkedHashSet<>(breed), minYear, maxYear);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(litterStatistics);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/inconsistencies/gender/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIncorrectOrMissingGender(@QueryParam("breed") String breedSynonym, @QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
        LOGGER.trace("getIncorrectOrMissingGender()");
        if (skip == null || skip < 0) {
            skip = 0;
        }
        if (limit == null || limit < 0) {
            limit = 10;
        }

        List<String> result = graphQueryService.getAllDogsWithInconsistentGender(skip, limit, breedSynonym);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(result);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Path("/inconsistencies/gender/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIncorrectOrMissingGender(@PathParam("uuid") String uuid) {
        LOGGER.trace("getIncorrectOrMissingGender({})", uuid);
        IncorrectGenderRecord result = graphQueryService.getDogWithInconsistentGender(uuid);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(result);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/inconsistencies/breed/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIncorrectBreed(@QueryParam("breed") String breedSynonym, @QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
        LOGGER.trace("getIncorrectOrMissingGender()");
        if (skip == null || skip < 0) {
            skip = 0;
        }
        if (limit == null || limit < 0) {
            limit = 10;
        }

        List<String> result = graphQueryService.getAllDogsWithInconsistentBreed(skip, limit, breedSynonym);

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(result);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/inconsistencies/breed/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIncorrectBreed(@PathParam("uuid") String uuid) {
        LOGGER.trace("getIncorrectOrMissingGender({})", uuid);
        IncorrectBreedRecord result = graphQueryService.getDogWithInconsistentBreed(uuid);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(result);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/inconsistencies/circularancestry/breed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCircularAncestry(@QueryParam("breed") List<String> breed) {
        LOGGER.trace("getCircularAncestry()");
        if (breed == null || breed.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<String> result = graphQueryService.getCircluarParentChainInAncestryOf(new LinkedHashSet<>(breed));
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(result);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/inconsistencies/circularancestry/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCircularAncestry(@PathParam("uuid") String uuid) {
        LOGGER.trace("getCircularAncestry({})", uuid);
        List<CircularRecord> result = graphQueryService.getCircluarParentChainInAncestryOf(uuid);
        if (result == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(result);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
