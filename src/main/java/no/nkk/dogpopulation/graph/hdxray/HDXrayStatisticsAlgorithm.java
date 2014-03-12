package no.nkk.dogpopulation.graph.hdxray;

import no.nkk.dogpopulation.graph.CommonTraversals;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HDXrayStatisticsAlgorithm {

    static interface BreedTraverserFactory {
        org.neo4j.graphdb.traversal.Traverser traverse(int minYear, int maxYear, Node breedNode);
    }

    private final GraphDatabaseService graphDb;
    private final CommonTraversals commonTraversals;

    public HDXrayStatisticsAlgorithm(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        this.commonTraversals = new CommonTraversals(graphDb);
    }

    public HDXrayStatistics hdXrayStatisticsForDogsOfBreedBornBetween(Set<String> breedSet, int minYear, int maxYear) {
        BreedTraverserFactory factory = new BreedTraverserFactory() {
            @Override
            public Traverser traverse(int minYear, int maxYear, Node breedNode) {
                return commonTraversals.traverseDogOfBreedBornBetween(breedNode, minYear, maxYear);
            }
        };
        return hdXrayStatisticsForDogsOfBreed(breedSet, minYear, maxYear, factory);
    }

    public HDXrayStatistics hdXrayStatisticsForDogsOfBreedXrayedBetween(Set<String> breedSet, int minYear, int maxYear) {
        BreedTraverserFactory factory = new BreedTraverserFactory() {
            @Override
            public Traverser traverse(int minYear, int maxYear, Node breedNode) {
                return traverseDogsOfBreedHdXrayedBetween(breedNode, minYear, maxYear);
            }
        };
        return hdXrayStatisticsForDogsOfBreed(breedSet, minYear, maxYear, factory);
    }

    private HDXrayStatistics hdXrayStatisticsForDogsOfBreed(Set<String> breedSet, int minYear, int maxYear, BreedTraverserFactory factory) {
        int dogCount = 0;
        Map<String, Integer> countByDiagnose = new LinkedHashMap<>();
        for (Path breedMemberPath : commonTraversals.traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(breedSet)) {
            for (Path dogPath : factory.traverse(minYear, maxYear, breedMemberPath.endNode())) {
                Node dogNode = dogPath.endNode();
                dogCount++;
                if (dogNode.hasProperty(DogGraphConstants.DOG_HDDIAG)) {
                    String diagnose = (String) dogNode.getProperty(DogGraphConstants.DOG_HDDIAG);
                    Integer diagCount = countByDiagnose.get(diagnose);
                    if (diagCount == null) {
                        countByDiagnose.put(diagnose, 1);
                    } else {
                        countByDiagnose.put(diagnose, diagCount + 1);
                    }
                }
            }
        }
        return new HDXrayStatistics(breedSet, minYear, maxYear, dogCount, countByDiagnose);
    }

    private Traverser traverseDogsOfBreedHdXrayedBetween(Node breedNode, final int minYear, final int maxYear) {
        return graphDb.traversalDescription()
                .depthFirst()
                .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                .evaluator(Evaluators.atDepth(1))
                .evaluator(new Evaluator() {
                    @Override
                    public Evaluation evaluate(Path path) {
                        Node dogNode = path.endNode();
                        if (!dogNode.hasProperty(DogGraphConstants.DOG_HDYEAR)) {
                            return Evaluation.EXCLUDE_AND_CONTINUE;
                        }
                        int hdXrayYear = (int) dogNode.getProperty(DogGraphConstants.DOG_HDYEAR);
                        if (minYear <= hdXrayYear && hdXrayYear <= maxYear) {
                            return Evaluation.INCLUDE_AND_CONTINUE;
                        }
                        return Evaluation.EXCLUDE_AND_CONTINUE;
                    }
                })
                .traverse(breedNode);
    }

}
