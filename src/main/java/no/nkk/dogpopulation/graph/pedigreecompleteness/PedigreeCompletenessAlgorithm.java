package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.graph.BasicStatistics;
import no.nkk.dogpopulation.graph.CommonTraversals;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.UuidAndRegNo;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeCompletenessAlgorithm {

    private final GraphDatabaseService graphDb;
    private final int PEDIGREE_GENERATIONS;

    public PedigreeCompletenessAlgorithm(GraphDatabaseService graphDb, int generations) {
        this.graphDb = graphDb;
        this.PEDIGREE_GENERATIONS = generations;
    }


    public PedigreeCompleteness getPedigreeCompletenessOfGroup(Node categoryBreedNode, final Set<String> breedSet, final int minYear, final int maxYear) {
        DescriptiveStatistics pedigreeSizeStat = new DescriptiveStatistics();
        DescriptiveStatistics completenessStat = new DescriptiveStatistics();
        int N = PedigreeCompleteness.getSizeOfCompletePedigree(PEDIGREE_GENERATIONS);
        int[] pedigreeSizeHistogram = new int[N + 1];
        List<UuidAndRegNo> dogsWithEmptyPedigree = new ArrayList<>();
        List<UuidAndRegNo> dogsWithJustOneParent = new ArrayList<>();
        CommonTraversals commonTraversals = new CommonTraversals(graphDb);
        for (Path breedMemberPath : commonTraversals.traverseBreedInSet(categoryBreedNode, breedSet)) {
            for (Path dogPath : commonTraversals.traverseDogOfBreedBornBetween(breedMemberPath.endNode(), minYear, maxYear)) {
                Node dogNode = dogPath.endNode();
                int pedigreeSize = computePedigreeSize(dogNode);
                pedigreeSizeStat.addValue(pedigreeSize);
                completenessStat.addValue(100.0 * pedigreeSize / N);
                pedigreeSizeHistogram[pedigreeSize]++; // update histogram
                if (pedigreeSize == 0) {
                    dogsWithEmptyPedigree.add(new UuidAndRegNo(dogNode));
                }
                int parents = 0;
                for (Relationship hasParent : dogNode.getRelationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING)) {
                    parents++;
                }
                if (parents == 1) {
                    dogsWithJustOneParent.add(new UuidAndRegNo(dogNode));
                }
            }
        }
        return new PedigreeCompleteness(PEDIGREE_GENERATIONS, breedSet, minYear, maxYear, new BasicStatistics(pedigreeSizeStat), new BasicStatistics(completenessStat), pedigreeSizeHistogram, dogsWithEmptyPedigree, dogsWithJustOneParent);
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

}
