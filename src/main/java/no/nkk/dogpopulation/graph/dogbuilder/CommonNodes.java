package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CommonNodes {

    private final GraphDatabaseService graphDb;

    private final Node root;
    private final Node breedCategory;

    private final Map<String, Node> breedByName = new LinkedHashMap<>();


    public CommonNodes(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        try (Transaction tx = graphDb.beginTx()) {
            root = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_ROOT);
            breedCategory = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            if (!breedCategory.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
                breedCategory.createRelationshipTo(root, DogGraphRelationshipType.MEMBER_OF);
            }
            tx.success();
        }
    }


    public Node getRoot() {
        return root;
    }

    public Node getBreedCategory() {
        return breedCategory;
    }

    public Node getBreed(String name, String id) {
        synchronized (breedByName) {

            Node breedNode = breedByName.get(name);
            if (breedNode != null) {
                return breedNode;
            }

            try (Transaction tx = graphDb.beginTx()) {
                breedNode = new BreedNodeBuilder(this).name(name).id(id).build(graphDb);
                tx.success();
            }

            breedByName.put(name, breedNode);

            return breedNode;
        }
    }
}
