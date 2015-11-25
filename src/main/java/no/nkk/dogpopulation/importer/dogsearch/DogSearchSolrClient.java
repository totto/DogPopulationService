package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class DogSearchSolrClient implements DogSearchClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchSolrClient.class);
    private static final Logger DOGSEARCH = LoggerFactory.getLogger("dogsearch");

    private final SolrClient solrServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Random rnd = new Random();

    private final ExecutorService executorService;

    private final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Inject
    public DogSearchSolrClient(@Named(ExecutorManager.SOLR_MAP_KEY) ExecutorService executorService, @Named("dogServiceUrl") String dogServiceUrl) {
        this.executorService = executorService;
        solrServer = new HttpSolrClient(dogServiceUrl);
    }

    public Set<String> listIdsForBreed(final String breed, final LocalDateTime from, final LocalDateTime to) {
        Future<Set<String>> future = executorService.submit(new Callable<Set<String>>() {
            @Override
            public Set<String> call() throws Exception {
                Set<String> ids = new LinkedHashSet<>();
                QueryResponse queryResponse = runFindDogIdsQuery(breed, from, to);
                SolrDocumentList solrDocuments = queryResponse.getResults();
                long numFound = solrDocuments.getNumFound();
                long n = solrDocuments.size();
                for (SolrDocument solrDocument : solrDocuments) {
                    String id = (String) solrDocument.getFieldValue("id");
                    ids.add(id);
                }
                LOGGER.trace("n={}, numFound={}", n, numFound);
                return ids;
            }

            @Override
            public String toString() {
                return String.format("SOLR listing of uuids of breed %s between %s and %s", breed, from.toString(pattern), to.toString(pattern));
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TooManyNumFoundException) {
                throw (TooManyNumFoundException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    @Override
    public Future<DogDetails> findDog(final String id) {
        return executorService.submit(new Callable<DogDetails>() {
            @Override
            public DogDetails call() throws Exception {
                try {
                    QueryResponse queryResponse = runFindDogQuery(id);
                    SolrDocumentList results = queryResponse.getResults();
                    DogDetails chosenCandidate = getFirstValidCandidate(id, results);
                    return chosenCandidate;
                } catch (RuntimeException e) {
                    LOGGER.warn("", e);
                    return null; // not found
                }
            }

            @Override
            public String toString() {
                return "SOLR lookup of dog " + id;
            }
        });
    }

    @Override
    public Set<String> listIdsForLastWeek() {
        return runListIdsForLastTimeQuery(7 * 24 * 60 * 60);
    }

    @Override
    public Set<String> listIdsForLastDay() {
        return runListIdsForLastTimeQuery(24*60*60);
    }

    @Override
    public Set<String> listIdsForLastHour() {
        return runListIdsForLastTimeQuery(60*60);
    }

    @Override
    public Set<String> listIdsForLastMinute() {
        return runListIdsForLastTimeQuery(60);
    }

    public Set<String> runListIdsForLastTimeQuery(int second) {
        SolrQuery solrQuery = new SolrQuery(String.format("timestamp:[NOW-%dSECOND TO NOW]", second));
        solrQuery.setSort("timestamp", SolrQuery.ORDER.desc);
        solrQuery.setFields("id");
        solrQuery.setRows(1000000);
        solrQuery.setStart(0);
        QueryResponse queryResponse = runSolrQueryWithRetries(solrQuery);
        SolrDocumentList results = queryResponse.getResults();
        Set<String> result = new LinkedHashSet<String>();
        for (SolrDocument solrDocument : results) {
            String uuid = (String) solrDocument.get("id");
            result.add(uuid);
        }
        return result;
    }

    private QueryResponse runFindDogQuery(String id) {
        String eid = ClientUtils.escapeQueryChars(id);
        SolrQuery solrQuery;
        try {
            UUID.fromString(id);
            solrQuery = new SolrQuery(String.format("id:\"%s\"", eid));
        } catch (IllegalArgumentException notValidUuid) {
            solrQuery = new SolrQuery(String.format("ids:\"%s\"", eid));
        }
        solrQuery.setRows(10);
        return runSolrQueryWithRetries(solrQuery);
    }

    private QueryResponse runSolrQueryWithRetries(SolrQuery solrQuery) {
        final int maxRetries = 100;
        int waitTimeMs = 1000 + rnd.nextInt(9001); // initial random wait-time between 1 and 10 seconds.
        final int maxWaitTimeMs = 300 * 1000; // at most 5 minutes between retries
        for (int i=1; true; i++) {
            try {
                try {
                    return solrServer.query(solrQuery);
                } catch (IOException e) {
                    throw new RuntimeException("Error solr query", e);
                }
            } catch (SolrServerException e) {
                if (i >= maxRetries) {
                    throw new RuntimeException("Failed again after retrying " + i + " times.", e);
                }
                LOGGER.debug("SolrServerException: attempt={}, secondsBeforeRetry={}, msg={}", i, waitTimeMs / 1000, e.getMessage());
                waitTimeMs = exponentialWait(maxWaitTimeMs, waitTimeMs);
            } catch (SolrException e) {
                if (i >= maxRetries) {
                    throw new RuntimeException("Failed again after retrying " + i + " times.", e);
                }
                int code = e.code();
                LOGGER.debug("SolrException: code={}. attempt={}, secondsBeforeRetry={}, msg={}", code, i, waitTimeMs / 1000, e.getMessage());
                waitTimeMs = exponentialWait(maxWaitTimeMs, waitTimeMs);
            }
        }
    }

    private int exponentialWait(int maxWaitTimeMs, int waitTimeMs) {
        if (waitTimeMs > maxWaitTimeMs) {
            waitTimeMs = maxWaitTimeMs;
        }

        try {
            Thread.sleep((int) waitTimeMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return 2 * waitTimeMs;
    }

    private QueryResponse runFindDogIdsQuery(String breed, LocalDateTime from, LocalDateTime to) {
        String ebreed = ClientUtils.escapeQueryChars(breed);
        SolrQuery solrQuery = new SolrQuery(String.format("breed:\"%s\" AND timestamp:[%s TO %s]", ebreed, from.toString(pattern), to.toString(pattern)));
        solrQuery.setSort("timestamp", SolrQuery.ORDER.asc);
        solrQuery.setFields("id", "timestamp");
        int MAX_RESULTS = 1000000;
        solrQuery.setRows(MAX_RESULTS);
        QueryResponse queryResponse = runSolrQueryWithRetries(solrQuery);
        long numFound = queryResponse.getResults().getNumFound();
        if (numFound > MAX_RESULTS) {
            throw new RuntimeException("Too many responses found, please decrease time-window");
        }
        return queryResponse;
    }

    private DogDetails getFirstValidCandidate(String id, SolrDocumentList results) {
        if (results.isEmpty()) {
            return null;
        }
        List<DogDetails> candidates = new ArrayList<>();
        for (SolrDocument solrDocument : results) {
            String json_detailed = (String) solrDocument.get("json_detailed");
            try {
                preProcess(id, json_detailed);
                DogDetails dogDetails = objectMapper.readValue(json_detailed, DogDetails.class);
                dogDetails.setJson(json_detailed);
                candidates.add(dogDetails); // found candidate
            } catch (IOException e) {
                if (json_detailed.startsWith("{{")) {
                    String intended_json_detailed = json_detailed.substring(1, json_detailed.length() - 1);
                    try {
                        DogDetails dogDetails = objectMapper.readValue(intended_json_detailed, DogDetails.class);
                        dogDetails.setJson(intended_json_detailed);
                        candidates.add(dogDetails); // found candidate
                    } catch (IOException e1) {
                        DOGSEARCH.warn("BAD JSON: {}", json_detailed);
                    }
                } else {
                    DOGSEARCH.warn("BAD JSON: {}", json_detailed);
                }
            }
        }
        if (candidates.isEmpty()) {
            DOGSEARCH.info("No valid candidates for dog ", id);
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0); // only one valid candidate
        }

        // candidates.size() > 1

        DogDetails chosenCandidate = candidates.get(0);
        DOGSEARCH.info("Found more than 1 dog with id {}, listing candidates (Elected candidate {}):", id, chosenCandidate.getId());
        int i = 1;
        for (SolrDocument solrDocument : results) {
            String json_detailed = (String) solrDocument.get("json_detailed");
            DOGSEARCH.info("CANDIDATE {}: {}", i++, json_detailed);
        }
        return chosenCandidate; // return first candidate
    }

    protected void preProcess(String id, String json_detailed) {
    }
}
