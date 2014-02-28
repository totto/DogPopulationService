package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CommonNodes {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonNodes.class);

    private final GraphDatabaseService graphDb;

    private final Node root;
    private final Node breedCategory;

    private final Map<String, BreedNodeAndId> breedByName = new LinkedHashMap<>();

    private static class BreedNodeAndId {
        private final Node node;
        private final String id;
        private BreedNodeAndId(Node node, String id) {
            this.node = node;
            this.id = id;
        }
    }

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
        if (name == null) {
            return null;
        }

        synchronized (breedByName) {

            BreedNodeAndId nodeAndId = breedByName.get(name);
            if (nodeAndId != null) {
                if (id == null) {
                    return nodeAndId.node;
                }
                if (id.trim().isEmpty()) {
                    return nodeAndId.node;
                }
                if (id.trim().equals(nodeAndId.id)) {
                    return nodeAndId.node;
                }
                try (Transaction tx = graphDb.beginTx()) {
                    if (nodeAndId.node.hasProperty(DogGraphConstants.BREED_ID)) {
                        LOGGER.warn("Breed.id conflict for {}, id was {} now set to {}", name, nodeAndId.node.getProperty(DogGraphConstants.BREED_ID), id);
                    }
                    nodeAndId.node.setProperty(DogGraphConstants.BREED_ID, id);
                    tx.success();
                }
                breedByName.put(name, new BreedNodeAndId(nodeAndId.node, id));
                return nodeAndId.node;
            }

            try (Transaction tx = graphDb.beginTx()) {
                nodeAndId = new BreedNodeAndId(new BreedNodeBuilder(this).name(name).id(id).build(graphDb), id);
                tx.success();
            }

            breedByName.put(name, nodeAndId);

            return nodeAndId.node;
        }
    }
}
