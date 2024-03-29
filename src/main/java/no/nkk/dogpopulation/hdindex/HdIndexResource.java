package no.nkk.dogpopulation.hdindex;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.hdindex.UnknownBreedCodeException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/dogpopulation/hdindex")
public class HdIndexResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdIndexResource.class);

    private static final boolean DEFAULT_REGENERATE_LITTER_ID = true;

    // all access must be synchronized on the map reference itself
    private final Map<String, CountDownLatch> lockedFiles = new LinkedHashMap<>();

    private final GraphQueryService graphQueryService;

    private final File hdIndexFolder;

    private enum HdIndexFileType {
        DATA, PEDIGREE, UUID_MAPPING, BREED_CODE_MAPPING, DATA_ERROR_FILE
    }

    @Inject
    public HdIndexResource(GraphQueryService graphQueryService, @Named("hdindex-folder") String hdIndexFolderPath) {
        this.graphQueryService = graphQueryService;
        hdIndexFolder = new File(hdIndexFolderPath);
        try {
            FileUtils.forceMkdir(hdIndexFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/{breed}/data")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getHdIndexDataFile(@PathParam("breed") String breed, @QueryParam("breed") Set<String> breedSet, @QueryParam("regenerateLitterId") Boolean regenerateLitterId) {
        LOGGER.trace("getHdIndexDataFile()");

        Map<HdIndexFileType, File> map = mapFiles(breed);

        if (!map.get(HdIndexFileType.DATA).isFile()) {
            if (breedSet == null || breedSet.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            try {
                getHdIndexFiles(breedSet, map, regenerateLitterId != null ? regenerateLitterId : DEFAULT_REGENERATE_LITTER_ID);
            } catch (UnknownBreedCodeException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown breed code of race \"" + e.getMessage() + "\"").build();
            } catch (RuntimeException e) {
                LOGGER.error("", e);
                throw e;
            }
        }

        File file = map.get(HdIndexFileType.DATA);

        String attachment = String.format("attachment; filename=%s; size=%d", file.getName(), file.length());
        return Response.ok(file).header("Content-Disposition", attachment).build();
    }


    @GET
    @Path("/{breed}/pedigree")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getHdIndexPedigreeFile(@PathParam("breed") String breed, @QueryParam("breed") Set<String> breedSet, @QueryParam("regenerateLitterId") Boolean regenerateLitterId) {
        LOGGER.trace("getHdIndexPedigreeFile()");

        Map<HdIndexFileType, File> map = mapFiles(breed);

        if (!map.get(HdIndexFileType.PEDIGREE).isFile()) {
            if (breedSet == null || breedSet.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            try {
                getHdIndexFiles(breedSet, map, regenerateLitterId != null ? regenerateLitterId : DEFAULT_REGENERATE_LITTER_ID);
            } catch (UnknownBreedCodeException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown breed code of race \"" + e.getMessage() + "\"").build();
            } catch (RuntimeException e) {
                LOGGER.error("", e);
                throw e;
            }
        }

        File file = map.get(HdIndexFileType.PEDIGREE);

        String attachment = String.format("attachment; filename=%s; size=%d", file.getName(), file.length());
        return Response.ok(file).header("Content-Disposition", attachment).build();
    }


    @GET
    @Path("/{breed}/uuidmapping")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getHdIndexUuidFile(@PathParam("breed") String breed, @QueryParam("breed") Set<String> breedSet, @QueryParam("regenerateLitterId") Boolean regenerateLitterId) {
        LOGGER.trace("getHdIndexUuidFile()");
        Map<HdIndexFileType, File> map = mapFiles(breed);

        if (!map.get(HdIndexFileType.UUID_MAPPING).isFile()) {
            if (breedSet == null || breedSet.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            try {
                getHdIndexFiles(breedSet, map, regenerateLitterId != null ? regenerateLitterId : DEFAULT_REGENERATE_LITTER_ID);
            } catch (UnknownBreedCodeException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Unknown breed code of race \"" + e.getMessage() + "\"").build();
            } catch (RuntimeException e) {
                LOGGER.error("", e);
                throw e;
            }
        }

        File file = map.get(HdIndexFileType.UUID_MAPPING);

        String attachment = String.format("attachment; filename=%s; size=%d", file.getName(), file.length());
        return Response.ok(file).header("Content-Disposition", attachment).build();
    }


    private Map<HdIndexFileType, File> getHdIndexFiles(Set<String> breed, Map<HdIndexFileType, File> map, boolean regenerateLitterId) {
        CountDownLatch lock = null;
        CountDownLatch existingLock;
        synchronized (lockedFiles) {
            existingLock = lockedFiles.get(map.get(HdIndexFileType.DATA).getName());
            if (existingLock == null) {
                lock = new CountDownLatch(1);
                lockedFiles.put(map.get(HdIndexFileType.DATA).getName(), lock);
            }
        }

        if (existingLock != null) {
            // already locked, wait on completion
            try {
                existingLock.await(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            // generate file
            graphQueryService.writeDmuFiles(map.get(HdIndexFileType.DATA), map.get(HdIndexFileType.PEDIGREE), map.get(HdIndexFileType.UUID_MAPPING), map.get(HdIndexFileType.BREED_CODE_MAPPING), map.get(HdIndexFileType.DATA_ERROR_FILE), breed, regenerateLitterId);
            lock.countDown(); // signal file generation completion
            synchronized (lockedFiles) {
                lockedFiles.remove(map.get(HdIndexFileType.DATA).getName());
            }
        }
        return map;
    }


    private Map<HdIndexFileType, File> mapFiles(String breed) {
        Map<HdIndexFileType, File> map = new LinkedHashMap<>();

        String dataFilename = String.format("hdindex-data-%s.txt", breed);
        String pedigreeFilename = String.format("hdindex-pedigree-%s.txt", breed);
        String uuidMappingFilename = String.format("hdindex-uuid-map-%s.txt", breed);
        String breedCodeMappingFilename = String.format("hdindex-breed-map-%s.txt", breed);
        String dataErrorFilename = String.format("hdindex-errors-%s.txt", breed);
        File folder = new File(hdIndexFolder, breed);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File dataFile = new File(folder, dataFilename);
        File pedigreeFile = new File(folder, pedigreeFilename);
        File uuidMappingFile = new File(folder, uuidMappingFilename);
        File breedCodeMappingFile = new File(folder, breedCodeMappingFilename);
        File dataErrorFile = new File(folder, dataErrorFilename);

        map.put(HdIndexFileType.DATA, dataFile);
        map.put(HdIndexFileType.PEDIGREE, pedigreeFile);
        map.put(HdIndexFileType.UUID_MAPPING, uuidMappingFile);
        map.put(HdIndexFileType.BREED_CODE_MAPPING, breedCodeMappingFile);
        map.put(HdIndexFileType.DATA_ERROR_FILE, dataErrorFile);
        return map;
    }
}
