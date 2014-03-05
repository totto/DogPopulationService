package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.*;

import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CommonTraversals {

    private final GraphDatabaseService graphDb;

    public CommonTraversals(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    public Traverser traverseDogOfBreedBornBetween(Node breedNode, final int minYear, final int maxYear) {
        return graphDb.traversalDescription()
                .depthFirst()
                .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                .evaluator(Evaluators.atDepth(1))
                .evaluator(new Evaluator() {
                    @Override
                    public Evaluation evaluate(Path path) {
                        Node dogNode = path.endNode();
                        if (!dogNode.hasProperty(DogGraphConstants.DOG_BORN_YEAR)) {
                            return Evaluation.EXCLUDE_AND_CONTINUE;
                        }
                        int bornYear = (int) dogNode.getProperty(DogGraphConstants.DOG_BORN_YEAR);
                        if (minYear <= bornYear && bornYear <= maxYear) {
                            return Evaluation.INCLUDE_AND_CONTINUE;
                        }
                        return Evaluation.EXCLUDE_AND_CONTINUE;
                    }
                })
                .traverse(breedNode);
    }


    public  Traverser traverseBreedInSet(Node categoryBreedNode, final Set<String> breedList) {
        return graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NODE_GLOBAL)
                .relationships(DogGraphRelationshipType.MEMBER_OF, Direction.INCOMING)
                .evaluator(new PathEvaluator.Adapter() {
                    public Evaluation evaluate(Path path, BranchState state) {
                        if (path.length() != 1) {
                            return Evaluation.EXCLUDE_AND_CONTINUE; // not at depth 1
                        }
                        Node breedNode = path.endNode();
                        String breed = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
                        if (breedList.contains(breed)) {
                            return Evaluation.INCLUDE_AND_PRUNE;
                        }
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                })
                .traverse(categoryBreedNode);
    }


    public Traverser traverseDogsOfBreed(Node breedNode) {
        return graphDb.traversalDescription()
                .depthFirst()
                .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                .evaluator(Evaluators.atDepth(1))
                .traverse(breedNode);
    }

}
