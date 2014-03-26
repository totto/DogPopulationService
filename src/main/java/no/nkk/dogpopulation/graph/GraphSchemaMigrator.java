package no.nkk.dogpopulation.graph;

import com.google.inject.Inject;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphSchemaMigrator {

    private final GraphDatabaseService graphDb;

    @Inject
    public GraphSchemaMigrator(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public void migrateSchema(GraphDatabaseService graphDb) {
        migrateBreedNodesToBreedSynonymNodes(graphDb);
    }

    public void migrateBreedNodesToBreedSynonymNodes(GraphDatabaseService graphDb) {
        try (Transaction tx = graphDb.beginTx()) {
            Node breedCategoryNode = GraphUtils.getSingleNode(graphDb, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, "Breed");
            if (breedCategoryNode == null) {
                return; // already migrated
            }
            for (Path path : graphDb.traversalDescription()
                    .relationships(DogGraphRelationshipType.MEMBER_OF, Direction.INCOMING)
                    .evaluator(Evaluators.atDepth(1))
                    .traverse(breedCategoryNode)) {
                Node breedSynonym = path.endNode();
                breedSynonym.removeLabel(DogGraphLabel.BREED);
                breedSynonym.addLabel(DogGraphLabel.BREED_SYNONYM);
                for (Relationship relationship : breedSynonym.getRelationships(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
                    relationship.delete();
                }
                String synonym = (String) breedSynonym.getProperty("breed");
                breedSynonym.setProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM, synonym);
                breedSynonym.removeProperty("breed");
                if (breedSynonym.hasProperty("id")) {
                    breedSynonym.removeProperty("id");
                }
            }
            Relationship memberOfRoot = breedCategoryNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            memberOfRoot.delete();
            breedCategoryNode.delete();
            tx.success();
        }
    }

}
