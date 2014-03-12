package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;

import java.util.LinkedHashSet;
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


    public  Traverser traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(Node breedGroupsCategoryNode, final Set<String> breedSet) {
        return traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(breedSet);
    }

    public  Traverser traverseAllBreedSynonymNodesThatAreMembersOfTheSameBreedGroupAsSynonymsInSet(Set<String> breedSet) {
        Set<Node> synonymNodes = new LinkedHashSet<>();
        Set<Node> breedNodes = new LinkedHashSet<>();
        for (String synonym : breedSet) {
            Node synonymNode = GraphUtils.getSingleNode(graphDb, DogGraphLabel.BREED_SYNONYM, DogGraphConstants.BREEDSYNONYM_SYNONYM, synonym);
            if (synonymNode == null) {
                    continue;
            }
            synonymNodes.add(synonymNode);
            Relationship memberOf = synonymNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            if (memberOf == null) {
                continue;
            }
            Node breedNode = memberOf.getEndNode();
            breedNodes.add(breedNode);
        }
        for (Path path : graphDb.traversalDescription()
                .relationships(DogGraphRelationshipType.MEMBER_OF, Direction.INCOMING)
                .evaluator(Evaluators.atDepth(1))
                .traverse(breedNodes.toArray(new Node[breedNodes.size()]))) {
            Node synonymNode = path.endNode();
            synonymNodes.add(synonymNode);
        }
        return graphDb.traversalDescription()
                .evaluator(Evaluators.atDepth(0))
                .traverse(synonymNodes.toArray(new Node[synonymNodes.size()]));
    }


    public Traverser traverseDogsOfBreed(Node breedNode) {
        return graphDb.traversalDescription()
                .depthFirst()
                .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                .evaluator(Evaluators.atDepth(1))
                .traverse(breedNode);
    }

}
