package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import no.nkk.dogpopulation.graph.CommonTraversals;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CircularAncestryBreedGroupAlgorithm {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircularParentChainAlgorithm.class);

    private final GraphDatabaseService graphDb;
    private final ExecutionEngine engine;
    private final CircularParentChainAlgorithm algorithm;

    public CircularAncestryBreedGroupAlgorithm(GraphDatabaseService graphDb, ExecutionEngine engine) {
        this.graphDb = graphDb;
        this.engine = engine;
        this.algorithm = new CircularParentChainAlgorithm(graphDb, engine);
    }

    public List<String> run(Set<String> breedSet) {
        Set<String> result = new LinkedHashSet<>();
        CommonTraversals commonTraversals = new CommonTraversals(graphDb);
        Set<Relationship> alreadyVisited = new HashSet<>(30000);
        for (Path breedPath : commonTraversals.traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(breedSet)) {
            Node breedNode = breedPath.endNode();
            for (Path path : commonTraversals.traverseDogsOfBreed(breedNode)) {
                Node dogNode = path.endNode();
                String uuid = (String) dogNode.getProperty(DogGraphConstants.DOG_UUID);
                List<Relationship> circle = findCircle(dogNode, alreadyVisited);
                if (circle != null && circle.size() > 0) {
                    // there is at least one circle in ancestry, eliminate any descendants from further search
                    for (Relationship r : circle) {
                        Node node = r.getEndNode();
                        addDescendantsToSet(node, alreadyVisited);
                    }
                    result.add((String) circle.get(0).getStartNode().getProperty(DogGraphConstants.DOG_UUID));
                }
            }
        }
        return new ArrayList<>(result);
    }

    private List<Relationship> findCircle(Node dogNode, Set<Relationship> alreadyVisited) {
        AncestorExpander ancestorExpander = new AncestorExpander(alreadyVisited);
        for (Path path : traverseAncestorsWithExpander(dogNode, ancestorExpander)) {
            if (containsSameNodeTwice(path)) {
                return buildCircle(path);
            }
        }
        alreadyVisited.addAll(ancestorExpander.getVisited());
        return null;
    }

    private Traverser traverseAncestorsWithExpander(Node dogNode, AncestorExpander ancestorExpander) {
        return graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.RELATIONSHIP_PATH) // do not use NODE_PATH, because we need to see the node twice in path to detect circle
                .expand(ancestorExpander)
                .traverse(dogNode);
    }

    private List<Relationship> buildCircle(Path path) {
        List<Relationship> circle = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        for (Relationship r : path.reverseRelationships()) {
            Node node = r.getEndNode();
            if (stack.contains(node)) {
                break;
            }
            stack.push(node);
            circle.add(r);
        }
        Collections.reverse(circle);
        return circle;
    }

    private boolean containsSameNodeTwice(Path path) {
        Set<Node> nodes = new HashSet<>(3 * path.length());
        for (Node node : path.nodes()) {
            if (nodes.contains(node)) {
                return true;
            }
            nodes.add(node);
        }
        return false;
    }

    private void addDescendantsToSet(Node dogNode, Set<Relationship> alreadyVisited) {
        for (Path path : traverseDescendants(dogNode)) {
            Relationship relationship = path.lastRelationship();
            if (relationship != null) {
                alreadyVisited.add(relationship);
            }
        }
    }

    private Traverser traverseDescendants(Node dogNode) {
        return graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NODE_PATH)
                .relationships(DogGraphRelationshipType.HAS_PARENT, Direction.INCOMING)
                .traverse(dogNode);
    }

}
