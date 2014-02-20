package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.graph.BasicStatistics;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeCompletenessAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(PedigreeCompletenessAlgorithm.class);

    private final GraphDatabaseService graphDb;
    private final int PEDIGREE_GENERATIONS;

    public PedigreeCompletenessAlgorithm(GraphDatabaseService graphDb, int generations) {
        this.graphDb = graphDb;
        this.PEDIGREE_GENERATIONS = generations;
    }

    public PedigreeCompleteness getPedigreeCompletenessOfGroup(Node categoryBreedNode, final Set<String> breedSet, final int minYear, final int maxYear) {
        DescriptiveStatistics pedigreeSizeStat = new DescriptiveStatistics();
        DescriptiveStatistics completenessStat = new DescriptiveStatistics();
        int N = PedigreeCompleteness.getCompletePedigreeSize(PEDIGREE_GENERATIONS);
        for (Path breedMemberPath : traverseBreedInSet(categoryBreedNode, breedSet)) {
            for (Path dogPath : traverseDogOfBreedBornBetween(breedMemberPath.endNode(), minYear, maxYear)) {
                Node dogNode = dogPath.endNode();
                int pedigreeSize = computePedigreeSize(dogNode);
                pedigreeSizeStat.addValue(pedigreeSize);
                completenessStat.addValue(100.0 * pedigreeSize / N);
            }
        }
        return new PedigreeCompleteness(PEDIGREE_GENERATIONS, breedSet, minYear, maxYear, new BasicStatistics(pedigreeSizeStat), new BasicStatistics(completenessStat));
    }

    public int computePedigreeSize(Node dogNode) {
        int n = 0;
        for (Path ignored : traversePedigreeToPredefinedDepth(dogNode)) {
            n++;
        }
        return n;
    }

    private Traverser traversePedigreeToPredefinedDepth(Node dogNode) {
        return graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NONE)
                .relationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING)
                .evaluator(Evaluators.includingDepths(1, PEDIGREE_GENERATIONS - 1))
                .traverse(dogNode);
    }

    private Traverser traverseDogOfBreedBornBetween(Node breedNode, final int minYear, final int maxYear) {
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

    private Traverser traverseBreedInSet(Node categoryBreedNode, final Set<String> breedList) {
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

}
