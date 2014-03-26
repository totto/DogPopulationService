package no.nkk.dogpopulation.graph.dogbuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class BreedSynonymNodeCache {

    private final GraphDatabaseService graphDb;

    private final Map<String, Node> breedByName = new LinkedHashMap<>();

    @Inject
    public BreedSynonymNodeCache(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public Node getBreed(String name) {
        if (name == null) {
            return null;
        }

        synchronized (breedByName) {
            Node node = breedByName.get(name);
            if (node != null) {
                return node;
            }
            try (Transaction tx = graphDb.beginTx()) {
                node = new BreedSynonymNodeBuilder().name(name).build(graphDb);
                tx.success();
            }
            breedByName.put(name, node);
            return node;
        }
    }
}
