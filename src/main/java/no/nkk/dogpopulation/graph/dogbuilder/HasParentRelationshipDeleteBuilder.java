package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.Builder;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.ParentRole;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HasParentRelationshipDeleteBuilder extends AbstractRelationshipBuilder {

    private Builder<Node> childBuilder;
    private ParentRole parentRole;

    HasParentRelationshipDeleteBuilder() {
    }

    @Override
    protected Relationship doBuild(GraphDatabaseService graphDb) {
        if (childBuilder == null) {
            throw new MissingFieldException("child");
        }
        if (parentRole == null) {
            throw new MissingFieldException("parentRole");
        }

        Node child = childBuilder.build(graphDb);
        Iterable<Relationship> parentRelationshipIterator = child.getRelationships(DogGraphRelationshipType.HAS_PARENT, Direction.OUTGOING);
        for (Relationship relationship : parentRelationshipIterator) {
            // iterate through parents already known by graph
            ParentRole existingParentRole = ParentRole.valueOf(((String) relationship.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            if (parentRole.equals(existingParentRole)) {
                relationship.delete();
                break;
            }
        }
        return null;
    }

    public HasParentRelationshipDeleteBuilder child(Builder<Node> childBuilder) {
        this.childBuilder = childBuilder;
        return this;
    }
    public HasParentRelationshipDeleteBuilder role(ParentRole parentRole) {
        this.parentRole = parentRole;
        return this;
    }

}
