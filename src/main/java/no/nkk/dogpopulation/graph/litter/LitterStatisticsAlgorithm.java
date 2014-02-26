package no.nkk.dogpopulation.graph.litter;

import no.nkk.dogpopulation.graph.BasicStatistics;
import no.nkk.dogpopulation.graph.CommonTraversals;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class LitterStatisticsAlgorithm {

    private final GraphDatabaseService graphDb;
    private final Set<String> breedSet;
    private final int minYear;
    private final int maxYear;
    private final Node categoryBreedNode;

    public LitterStatisticsAlgorithm(GraphDatabaseService graphDb, Set<String> breedSet, int minYear, int maxYear, Node categoryBreedNode) {
        this.graphDb = graphDb;
        this.breedSet = breedSet;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.categoryBreedNode = categoryBreedNode;
    }

    public LitterStatistics execute() {
        int dogCount = 0;
        int dogsWithAtLeastOneLitter = 0;
        DescriptiveStatistics litterSizeStatistics = new DescriptiveStatistics();
        CommonTraversals commonTraversals = new CommonTraversals(graphDb);
        Set<Node> dogNodes = new HashSet<>();
        Set<Node> litterNodes = new HashSet<>();
        for (Path breedMemberPath : commonTraversals.traverseBreedInSet(categoryBreedNode, breedSet)) {
            for (Path dogPath : commonTraversals.traverseDogOfBreedBornBetween(breedMemberPath.endNode(), minYear, maxYear)) {
                Node dogNode = dogPath.endNode();
                if (dogNodes.contains(dogNode)) {
                    continue;
                }
                dogNodes.add(dogNode);
                dogCount++;
                boolean atLeastOneLitter = false;
                for (Path litterPath : traverseLitters(dogNode)) {
                    atLeastOneLitter = true;
                    Node litterNode = litterPath.endNode();
                    if (litterNodes.contains(litterNode)) {
                        continue;
                    }
                    litterNodes.add(litterNode);
                    if (litterNode.hasProperty(DogGraphConstants.LITTER_COUNT)) {
                        litterSizeStatistics.addValue((Integer) litterNode.getProperty(DogGraphConstants.LITTER_COUNT));
                    }
                }
                if (atLeastOneLitter) {
                    dogsWithAtLeastOneLitter++;
                }
            }
        }
        return new LitterStatistics(breedSet, minYear, maxYear, dogCount, dogsWithAtLeastOneLitter, new BasicStatistics(litterSizeStatistics));
    }

    private Traverser traverseLitters(Node dogNode) {
        return graphDb.traversalDescription()
                .uniqueness(Uniqueness.NODE_GLOBAL)
                .evaluator(Evaluators.atDepth(1))
                .relationships(DogGraphRelationshipType.HAS_LITTER, Direction.OUTGOING)
                .traverse(dogNode);
    }

}
