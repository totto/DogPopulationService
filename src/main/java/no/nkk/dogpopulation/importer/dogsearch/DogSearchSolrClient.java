package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchSolrClient implements DogSearchClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchSolrClient.class);
    private static final Logger DOGSEARCH = LoggerFactory.getLogger("dogsearch");

    private final SolrServer solrServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Random rnd = new Random();

    public DogSearchSolrClient(String dogServiceUrl) {
        solrServer = new HttpSolrServer(dogServiceUrl);
    }

    public Set<String> listIdsForBreed(String breed) {
        int i=0;
        long n = 0;
        Set<String> ids = new LinkedHashSet<>();
        do {
            QueryResponse queryResponse = runFindDogIdsQuery(breed, 5000, i);
            SolrDocumentList solrDocuments = queryResponse.getResults();
            long numFound = solrDocuments.getNumFound();
            n = solrDocuments.size();
            for (SolrDocument solrDocument : solrDocuments) {
                String id = (String) solrDocument.getFieldValue("id");
                ids.add(id);
            }
            LOGGER.trace("i={}, n={}, numFound={}", i, n, numFound);
            i += n;
        } while (n > 0);
        return ids;
    }

    @Override
    public DogDetails findDog(String id) {
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

    private QueryResponse runFindDogQuery(String id) {
        String eid = ClientUtils.escapeQueryChars(id);
        SolrQuery solrQuery = new SolrQuery(String.format("id:\"%s\" OR ids:\"%s\"", eid, eid));
        solrQuery.setRows(10);
        return runSolrQueryWithRetries(solrQuery);
    }

    private QueryResponse runSolrQueryWithRetries(SolrQuery solrQuery) {
        final int maxRetries = 100;
        int waitTimeMs = 5000 + rnd.nextInt(5001); // initial random wait-time between 5 and 10 seconds.
        final int maxWaitTimeMs = 300 * 1000; // at most 5 minutes between retries
        for (int i=1; true; i++) {
            try {
                return solrServer.query(solrQuery);
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

    private QueryResponse runFindDogIdsQuery(String breed, int n, int start) {
        String ebreed = ClientUtils.escapeQueryChars(breed);
        SolrQuery solrQuery = new SolrQuery(String.format("breed:\"%s\"", ebreed));
        solrQuery.setSort("id", SolrQuery.ORDER.asc);
        solrQuery.setFields("id");
        solrQuery.setRows(n);
        solrQuery.setStart(start);
        return runSolrQueryWithRetries(solrQuery);
    }

    private DogDetails getFirstValidCandidate(String id, SolrDocumentList results) {
        if (results.isEmpty()) {
            return null;
        }
        List<DogDetails> candidates = new ArrayList<>();
        for (SolrDocument solrDocument : results) {
            String json_detailed = (String) solrDocument.get("json_detailed");
            try {
                DogDetails dogDetails = objectMapper.readValue(json_detailed, DogDetails.class);
                candidates.add(dogDetails); // found candidate
            } catch (IOException e) {
                if (json_detailed.startsWith("{{")) {
                    String intended_json_detailed = json_detailed.substring(1, json_detailed.length() - 1);
                    try {
                        DogDetails dogDetails = objectMapper.readValue(intended_json_detailed, DogDetails.class);
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

}
