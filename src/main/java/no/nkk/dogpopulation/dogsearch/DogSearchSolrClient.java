package no.nkk.dogpopulation.dogsearch;

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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchSolrClient implements DogSearchClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchSolrClient.class);

    private final SolrServer solrServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            DogDetails chosenCandidate = getFirstCandidate(id, results);
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
        float waitTimeMs = 1000;
        for (int i=1; true; i++) {
            try {
                return solrServer.query(solrQuery);
            } catch (SolrServerException e) {
                LOGGER.error(solrQuery.toString(), e);
                throw new RuntimeException(e);
            } catch (SolrException e) {
                if (i >= maxRetries) {
                    throw e;
                }
                int code = e.code();
                LOGGER.trace("SolrException: code={}. attempt={}, secondsBeforeRetry={}, msg={}", code, i, waitTimeMs/1000, e.getMessage());
                try {
                    Thread.sleep((int) waitTimeMs);
                    waitTimeMs *= 1.1;
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }
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

    private DogDetails getFirstCandidate(String id, SolrDocumentList results) {
        int candidate = 1;
        DogDetails dogDetails = null;
        for (SolrDocument solrDocument : results) {
            String json_detailed = (String) solrDocument.get("json_detailed");
            try {
                dogDetails = objectMapper.readValue(json_detailed, DogDetails.class);
                break; // found candidate
            } catch (IOException e) {
                LOGGER.warn("BAD JSON: {}", json_detailed);
            }
            candidate++;
        }
        if (LOGGER.isInfoEnabled() && results.getNumFound() > 1 && dogDetails != null) {
            LOGGER.info("Found more than 1 dog with id {}, listing candidates (Candidate {} was picked):", id, candidate);
            int i = 1;
            for (SolrDocument solrDocument : results) {
                String json_detailed = (String) solrDocument.get("json_detailed");
                LOGGER.info("CANDIDATE {}: {}", i++, json_detailed);
            }
        }
        return dogDetails; // return candidate
    }

}
