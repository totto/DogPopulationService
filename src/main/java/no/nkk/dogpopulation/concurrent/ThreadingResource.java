package no.nkk.dogpopulation.concurrent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Path("/dogpopulation/concurrent")
@Singleton
public class ThreadingResource {

    private final ExecutorManager executorManager;
    private final ObjectMapper objectMapper;
    private final ObjectWriter prettyPrintingObjectWriter;

    @Inject
    public ThreadingResource(ExecutorManager executorManager) {
        this.executorManager = executorManager;
        objectMapper = new ObjectMapper();
        prettyPrintingObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }

    @GET
    @Path("/executor/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPedigree() {

        Map<String, ExecutorStatus> statusMaps = new LinkedHashMap<>();
        for (Map.Entry<String, ManageableExecutor> e : executorManager.executorSnapshot().entrySet()) {
            statusMaps.put(e.getKey(), e.getValue().getStatus());
        }

        try {
            String json = prettyPrintingObjectWriter.writeValueAsString(statusMaps);
            return Response.ok(json).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
