package no.nkk.dogpopulation.graph.inbreeding;

import no.nkk.dogpopulation.graph.BasicStatistics;
import no.nkk.dogpopulation.graph.CommonTraversals;
import no.nkk.dogpopulation.graph.UuidAndRegNo;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class InbreedingOfGroupAlgorithm {

    private final GraphDatabaseService graphDb;
    private final int PEDIGREE_GENERATIONS;

    public InbreedingOfGroupAlgorithm(GraphDatabaseService graphDb, int generations) {
        this.graphDb = graphDb;
        this.PEDIGREE_GENERATIONS = generations;
    }


    public InbreedingOfGroup getInbreedingOfGroup(Node categoryBreedNode, final Set<String> breedSet, final int minYear, final int maxYear) {
        DescriptiveStatistics coefficientStatistics = new DescriptiveStatistics();
        int[] frequency = new int[60];
        List<UuidAndRegNo> dogsWithCoefficientAbove1250 = new ArrayList<>();
        List<UuidAndRegNo> dogsWithCoefficientAbove2500 = new ArrayList<>();
        List<UuidAndRegNo> dogsWithCoefficientAbove3000 = new ArrayList<>();
        InbreedingAlgorithm algorithm = new InbreedingAlgorithm(graphDb, PEDIGREE_GENERATIONS);
        CommonTraversals commonTraversals = new CommonTraversals(graphDb);
        for (Path breedMemberPath : commonTraversals.traverseBreedInSet(categoryBreedNode, breedSet)) {
            for (Path dogPath : commonTraversals.traverseDogOfBreedBornBetween(breedMemberPath.endNode(), minYear, maxYear)) {
                Node dogNode = dogPath.endNode();
                double coi = algorithm.computeSewallWrightCoefficientOfInbreeding(dogNode);
                double percentage = 100 * coi;
                int index = 1 + (int) percentage;
                if (percentage == 0) {
                    index = 0;
                }
                frequency[index]++;
                double percentageTimes100 = 100 * percentage;
                if (percentageTimes100 >= 3000) {
                    dogsWithCoefficientAbove3000.add(new UuidAndRegNo(dogNode));
                } else if (percentageTimes100 >= 2500) {
                    dogsWithCoefficientAbove2500.add(new UuidAndRegNo(dogNode));
                } else if (percentageTimes100 >= 1250) {
                    dogsWithCoefficientAbove1250.add(new UuidAndRegNo(dogNode));
                }
                coefficientStatistics.addValue(percentage);
            }
        }
        return new InbreedingOfGroup(breedSet, minYear, maxYear, PEDIGREE_GENERATIONS, new BasicStatistics(coefficientStatistics), frequency, dogsWithCoefficientAbove1250, dogsWithCoefficientAbove2500, dogsWithCoefficientAbove3000);
    }

}
