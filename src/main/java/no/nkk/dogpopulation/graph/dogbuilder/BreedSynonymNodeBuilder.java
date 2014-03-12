package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedSynonymNodeBuilder extends AbstractNodeBuilder {

    private String name;
    private Node breedNode;
    private BreedNodeBuilder breedBuilder;

    BreedSynonymNodeBuilder() {
    }

    @Override
    protected Node doBuild(GraphDatabaseService graphDb) {
        if (name == null) {
            throw new MissingFieldException("name");
        }

        Node breedSynonymNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED_SYNONYM, DogGraphConstants.BREEDSYNONYM_SYNONYM, name);
        doBuildRelationshipToBreed(graphDb, breedSynonymNode);
        return breedSynonymNode;
    }

    private void doBuildRelationshipToBreed(GraphDatabaseService graphDb, Node breedSynonymNode) {
        if (breedNode == null) {
            if (breedBuilder == null) {
                return;
            }
            breedNode = breedBuilder.build(graphDb);
        }

        if (!breedSynonymNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            // no existing connection to breed node
            breedNode.createRelationshipTo(breedNode, DogGraphRelationshipType.MEMBER_OF);
            return;
        }

        Relationship singleRelationship = breedSynonymNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
        Node existingBreedNode = singleRelationship.getEndNode();
        if (existingBreedNode.equals(breedNode)) {
            return; // already connected to correct breed node
        }

        // wrong existing connection, delete it and create correct one
        singleRelationship.delete();
        breedNode.createRelationshipTo(breedNode, DogGraphRelationshipType.MEMBER_OF);
    }


    public BreedSynonymNodeBuilder name(String name) {
        if (name != null) {
            this.name = name.trim();
        }
        return this;
    }
    public BreedSynonymNodeBuilder breed(Node breedNode) {
        this.breedNode = breedNode;
        return this;
    }
    public BreedSynonymNodeBuilder breed(BreedNodeBuilder breedBuilder) {
        this.breedBuilder = breedBuilder;
        return this;
    }

}
