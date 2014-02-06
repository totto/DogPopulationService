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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

    @Override
    public DogDetails findDog(String id) {
        QueryResponse queryResponse = runFindDogQuery(id);
        SolrDocumentList results = queryResponse.getResults();
        logCandidatesIfMoreThanOneResult(id, results);
        DogDetails chosenCandidate = getFirstCandidate(results);
        return chosenCandidate;
    }

    private QueryResponse runFindDogQuery(String id) {
        String eid = ClientUtils.escapeQueryChars(id);
        SolrQuery solrQuery = new SolrQuery(String.format("id:\"%s\" OR ids:\"%s\"", eid, eid));
        solrQuery.setRows(10);
        try {
            return solrServer.query(solrQuery);
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    private DogDetails getFirstCandidate(SolrDocumentList results) {
        for (SolrDocument solrDocument : results) {
            String json_detailed = (String) solrDocument.get("json_detailed");
            DogDetails dogDetails = null;
            try {
                dogDetails = objectMapper.readValue(json_detailed, DogDetails.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return dogDetails; // return first candidate
        }
        return null; // not found
    }

    private void logCandidatesIfMoreThanOneResult(String id, SolrDocumentList results) {
        if (LOGGER.isInfoEnabled() && results.getNumFound() > 1) {
            LOGGER.info("Found more than 1 dog with id {}, listing candidates (Candidate 1 was picked):", id);
            int i = 1;
            for (SolrDocument solrDocument : results) {
                String json_detailed = (String) solrDocument.get("json_detailed");
                LOGGER.info("CANDIDATE {}: {}", i++, json_detailed);
            }
        }
    }
}
