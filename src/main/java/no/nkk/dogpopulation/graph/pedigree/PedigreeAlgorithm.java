package no.nkk.dogpopulation.graph.pedigree;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.InitialBranchState;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeAlgorithm {

    private final GraphDatabaseService graphDb;

    public PedigreeAlgorithm(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    public TopLevelDog getPedigree(Node node) {
        TopLevelDog dog = new TopLevelDog();
        for (Path path : traversePedigree(node, dog)) {
        }
        dog.setOffspring(PedigreeUtils.getOffspring(node));
        return dog;
    }


    private Traverser traversePedigree(Node node, Dog initialState) {
        return graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.RELATIONSHIP_PATH)
                .evaluator(Evaluators.toDepth(9))
                .expand(new PedigreePathExpander(), new InitialBranchState.State<>(initialState, null))
                .traverse(node);
    }

}
