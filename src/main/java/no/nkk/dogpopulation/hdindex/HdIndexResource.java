package no.nkk.dogpopulation.hdindex;

import no.nkk.dogpopulation.graph.GraphQueryService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
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

    // all access must be synchronized on the map reference itself
    private final Map<String, CountDownLatch> lockedFiles = new LinkedHashMap<>();

    private final GraphQueryService graphQueryService;

    private final File hdIndexFolder = new File("hdindex");

    private enum HdIndexFileType {
        DATA, PEDIGREE, UUID_MAPPING
    }

    public HdIndexResource(GraphQueryService graphQueryService) {
        this.graphQueryService = graphQueryService;
        try {
            FileUtils.forceMkdir(hdIndexFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GET
    @Path("/{breed}/data")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getHdIndexDataFile(@PathParam("breed") String breed, @QueryParam("breed") Set<String> breedSet) {
        LOGGER.trace("getHdIndexDataFile()");

        Map<HdIndexFileType, File> map = mapFiles(breed);

        if (!map.get(HdIndexFileType.DATA).isFile()) {
            if (breedSet == null || breedSet.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            getHdIndexFiles(breedSet, map);
        }

        File file = map.get(HdIndexFileType.DATA);

        String attachment = String.format("attachment; filename=%s; size=%d", file.getName(), file.length());
        return Response.ok(file).header("Content-Disposition", attachment).build();
    }


    @GET
    @Path("/{breed}/pedigree")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getHdIndexPedigreeFile(@PathParam("breed") String breed, @QueryParam("breed") Set<String> breedSet) {
        LOGGER.trace("getHdIndexPedigreeFile()");

        Map<HdIndexFileType, File> map = mapFiles(breed);

        if (!map.get(HdIndexFileType.PEDIGREE).isFile()) {
            if (breedSet == null || breedSet.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            getHdIndexFiles(breedSet, map);
        }

        File file = map.get(HdIndexFileType.PEDIGREE);

        String attachment = String.format("attachment; filename=%s; size=%d", file.getName(), file.length());
        return Response.ok(file).header("Content-Disposition", attachment).build();
    }


    @GET
    @Path("/{breed}/uuidmapping")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getHdIndexUuidFile(@PathParam("breed") String breed, @QueryParam("breed") Set<String> breedSet) {
        LOGGER.trace("getHdIndexUuidFile()");

        Map<HdIndexFileType, File> map = mapFiles(breed);

        if (!map.get(HdIndexFileType.UUID_MAPPING).isFile()) {
            if (breedSet == null || breedSet.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            getHdIndexFiles(breedSet, map);
        }

        File file = map.get(HdIndexFileType.UUID_MAPPING);

        String attachment = String.format("attachment; filename=%s; size=%d", file.getName(), file.length());
        return Response.ok(file).header("Content-Disposition", attachment).build();
    }


    private Map<HdIndexFileType, File> getHdIndexFiles(Set<String> breed, Map<HdIndexFileType, File> map) {
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
            try(PrintWriter dataOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(map.get(HdIndexFileType.DATA)), Charset.forName("ISO-8859-1")))) {
                try(PrintWriter pedigreeOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(map.get(HdIndexFileType.PEDIGREE)), Charset.forName("ISO-8859-1")))) {
                    try(PrintWriter mappingOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(map.get(HdIndexFileType.UUID_MAPPING)), Charset.forName("ISO-8859-1")))) {
                        graphQueryService.writeDmuFiles(dataOut, pedigreeOut, mappingOut, breed);
                        mappingOut.flush();
                    }
                    pedigreeOut.flush();
                }
                dataOut.flush();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
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
        File folder = new File(hdIndexFolder, breed);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File dataFile = new File(folder, dataFilename);
        File pedigreeFile = new File(folder, pedigreeFilename);
        File uuidMappingFile = new File(folder, uuidMappingFilename);

        map.put(HdIndexFileType.DATA, dataFile);
        map.put(HdIndexFileType.PEDIGREE, pedigreeFile);
        map.put(HdIndexFileType.UUID_MAPPING, uuidMappingFile);
        return map;
    }
}
