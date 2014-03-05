package no.nkk.dogpopulation.graph.inbreeding;

import com.google.common.collect.Sets;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.*;

/**
 * This class encapsulates the algorithm for computing a "Coefficient Of Inbreeding". The principles of this method
 * was first discovered by geneticist Sewall Wright.
 *
 * Thread-safety: Instances of this class are thread-safe.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class InbreedingAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(InbreedingAlgorithm.class);

    private final GraphDatabaseService graphDb;

    private final int PEDIGREE_GENERATIONS;


    /**
     * Create an algorithm that will work on a specific graph using a fixed number of generations when computing
     * inbreeding coefficient. The number of generations will be used recursively, i.e. if we use 6 generations, then
     * an ancestor's inbreeding contribution must also use 6 "new" generations.
     *
     * @param graphDb
     * @param pedigreeGenerations
     */
    public InbreedingAlgorithm(GraphDatabaseService graphDb, int pedigreeGenerations) {
        this.graphDb = graphDb;
        PEDIGREE_GENERATIONS = pedigreeGenerations;
    }


    /**
     * Compute the "Coefficient Of Inbreeding" using the method by geneticist Sewall Wright.
     *
     * @param dog the dog for which we want the inbreeding coefficient of.
     * @return the Coefficient Of Inbreeding.
     */
    public double computeSewallWrightCoefficientOfInbreeding(Node dog) {
        return computeCoefficientOfInbreeding(dog, PEDIGREE_GENERATIONS, 0);
    }


    /**
     * Compute the "Coefficient Of Inbreeding" of a not-yet-bred offspring of two parents using the method by geneticist Sewall Wright.
     *
     * @param inbredDogId a non-existent ID of the dog we want as a result of breeding the two parents
     * @param firstParent the first parent to use in the wanted breeding
     * @param secondParent the second parent to use in the wanted breeding
     * @return the Coefficient Of Inbreeding of the inbredDogId.
     */
    public double computeSewallWrightCoefficientOfInbreeding(String inbredDogId, Node firstParent, Node secondParent) {
        return computeCoefficientOfInbreeding(inbredDogId, PEDIGREE_GENERATIONS, 0, firstParent, secondParent);
    }


    private double computeCoefficientOfInbreeding(Node dog, int toDepth, int recursionLevel) {
        Iterable<Relationship> relationships = dog.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);

        Iterator<Relationship> parents = relationships.iterator();

        if (!parents.hasNext()) {
            return 0; // no known parents
        }

        Relationship firstHasParent = parents.next();

        if (!parents.hasNext()) {
            return 0; // missing one parent
        }

        Relationship secondHasParent = parents.next();
        Node secondParent = secondHasParent.getEndNode();

        // traverse pedigree of first parent, and collect all paths while traversing
        Node firstParent = firstHasParent.getEndNode();

        String inbredDogId = (String) dog.getProperty(DogGraphConstants.DOG_UUID);

        return computeCoefficientOfInbreeding(inbredDogId, toDepth, recursionLevel, firstParent, secondParent);
    }


    private double computeCoefficientOfInbreeding(String inbredDogId, int toDepth, int recursionLevel, Node firstParent, Node secondParent) {
        Map<String, List<Path>> firstParentPathsByUuid = mapAncestryPathsByAncestorUuid(firstParent, toDepth - 1, recursionLevel);

        double coi = computeInbreedingCoefficient(inbredDogId, secondParent, firstParentPathsByUuid, toDepth - 1, recursionLevel);

        return coi;
    }


    private double computeInbreedingCoefficient(String inbredDogId, Node secondParent, Map<String, List<Path>> firstParentPathsByUuid, int generations, int recursionLevel) {
        CommonAncestorEvaluator commonAncestorEvaluator = new CommonAncestorEvaluator(firstParentPathsByUuid, recursionLevel);

        double coi = 0;

        for (Path secondParentPath : graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NODE_PATH)
                .relationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING)
                .evaluator(Evaluators.toDepth(generations - 1))
                .evaluator(commonAncestorEvaluator)
                .traverse(secondParent)){

            Node commonAncestor = secondParentPath.endNode();

            String ancestorUuid = (String) commonAncestor.getProperty(DogGraphConstants.DOG_UUID);

            // determine path from one parent to the other through this common ancestor.

            List<Path> firstParentPaths = firstParentPathsByUuid.get(ancestorUuid);

            for (Path firstParentPath : firstParentPaths) {

                Sets.SetView<Node> intersection = Sets.intersection(Sets.newLinkedHashSet(secondParentPath.nodes()), Sets.newLinkedHashSet(firstParentPath.nodes()));
                if (intersection.size() > 1) {
                    continue; // there are more than one common ancestor in the two paths
                }

                int n = secondParentPath.length() + firstParentPath.length();

                double contribution = Math.pow(0.5, n + 1);

                double ancestorCoi = computeCoefficientOfInbreeding(commonAncestor, PEDIGREE_GENERATIONS, recursionLevel + 1);

                coi += contribution * (1 + ancestorCoi);

                tracePathBetweenParentsThroughCommonAncestor(inbredDogId, secondParentPath, firstParentPath, ancestorCoi, recursionLevel);
            }
        }
        return coi;
    }


    private Map<String, List<Path>> mapAncestryPathsByAncestorUuid(Node startNode, int generations, int recursionLevel) {
        Map<String, List<Path>> visited = new LinkedHashMap<>();
        for (Path path : graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NONE)
                .relationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING)
                .evaluator(Evaluators.toDepth(generations - 1))
                .traverse(startNode)) {
            Node endNode = path.endNode();
            String endNodeUuid = (String) endNode.getProperty(DogGraphConstants.DOG_UUID);
            List<Path> paths = visited.get(endNodeUuid);
            if (paths == null) {
                paths = new ArrayList<>();
                visited.put(endNodeUuid, paths);
            }
            paths.add(path);
            tracePath(1, path, false, recursionLevel);
        }
        return visited;
    }


    /**
     * Used for debugging only. This method does not produce any useful logic other tracing inbreeding coefficient contributions.
     *
     * This method assumes that the two supplied paths have the same end-node. The start-node of each path correlates to
     * each parent of the inbred dog. The end-node of both paths represents the common ancestor.
     *
     * @param inbredDogId
     * @param path
     * @param otherPath
     * @param ancestorCoi
     */
    private static void tracePathBetweenParentsThroughCommonAncestor(String inbredDogId, Path path, Path otherPath, double ancestorCoi, int recursionLevel) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Node node : otherPath.nodes()) {
            if (first) {
                first = false;
            } else {
                sb.append("->");
            }
            sb.append(node.getProperty(DogGraphConstants.DOG_UUID));
        }
        Iterator<Node> reverseNodeIterator = path.reverseNodes().iterator();
        reverseNodeIterator.next(); // avoid repeating common ancestor when combining paths
        while (reverseNodeIterator.hasNext()) {
            Node node = reverseNodeIterator.next();
            sb.append("<-").append(node.getProperty(DogGraphConstants.DOG_UUID));
        }
        int contributingRelations = path.length() + otherPath.length() + 1;
        Node ancestorDog = path.endNode();
        String ancestorDogId = (String) ancestorDog.getProperty(DogGraphConstants.DOG_UUID);
        String indentation = "";
        for (int i=0; i<recursionLevel; i++) {
            indentation += "    ";
        }
        if (ancestorCoi > 0) {
            String formattedAncestorCoi = new DecimalFormat("0.00").format(ancestorCoi);
            LOGGER.trace("{}F({},{}) = (0.5^{})(1+{})   {}", indentation, inbredDogId, ancestorDogId, contributingRelations, formattedAncestorCoi, sb.toString());
        } else {
            LOGGER.trace("{}F({},{}) = 0.5^{}           {}", indentation, inbredDogId, ancestorDogId, contributingRelations, sb.toString());
        }
    }


    /**
     * Used for debugging only. This method does not produce any useful logic other than path tracing information.
     *
     * @param parentIndex
     * @param path
     * @param commonAncestor
     */
    private static void tracePath(int parentIndex, Path path, boolean commonAncestor, int recursionLevel) {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<recursionLevel; i++) {
            sb.append("    ");
        }
        sb.append("P").append(parentIndex).append(": ");
        Iterator<Node> iterator = path.nodes().iterator();
        Node node = iterator.next();
        sb.append((String) node.getProperty(DogGraphConstants.DOG_UUID));
        while (iterator.hasNext()) {
            node = iterator.next();
            sb.append("-").append((String) node.getProperty(DogGraphConstants.DOG_UUID));
        }
        if (commonAncestor) {
            sb.append(" (*)");
        }
        LOGGER.trace(sb.toString());
    }


    /**
     * Evaluates whether or not a node is a common ancestor.
     */
    private static class CommonAncestorEvaluator implements Evaluator {

        private final Map<String, List<Path>> otherParentPathsByEndNodeUuid;

        private final int recursionLevel;

        CommonAncestorEvaluator(Map<String, List<Path>> otherParentPathsByEndNodeUuid, int recursionLevel) {
            this.otherParentPathsByEndNodeUuid = otherParentPathsByEndNodeUuid;
            this.recursionLevel = recursionLevel;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Node endNode = path.endNode();
            String endNodeUuid = (String) endNode.getProperty(DogGraphConstants.DOG_UUID);

            List<Path> otherPaths = otherParentPathsByEndNodeUuid.get(endNodeUuid);

            if (otherPaths == null) {
                tracePath(2, path, false, recursionLevel);
                return Evaluation.EXCLUDE_AND_CONTINUE; // node never before seen in pedigree
            }

            for (Path otherPath : otherPaths) {
                Sets.SetView<Node> intersection = Sets.intersection(Sets.newLinkedHashSet(path.nodes()), Sets.newLinkedHashSet(otherPath.nodes()));
                if (intersection.size() == 1) {
                    tracePath(2, path, true, recursionLevel);
                    return Evaluation.INCLUDE_AND_CONTINUE; // common ancestor on both mother and father side
                }
            }

            tracePath(2, path, false, recursionLevel);
            return Evaluation.EXCLUDE_AND_CONTINUE; // ancestor is not common on both mother and father side
        }
    }
}
