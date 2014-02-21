package no.nkk.dogpopulation.graph.pedigree;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.ParentRole;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreePathExpander implements PathExpander<Dog> {

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Dog> state) {
        Iterable<Relationship> expansion = path.endNode().getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT, DogGraphRelationshipType.OWN_ANCESTOR);

        Dog child = state.getState();

        if (path.length() == 0) {
            PedigreeUtils.populateDog(path.endNode(), child);

            return expansion;
        }

        Ancestry ancestry = child.getAncestry();
        if (ancestry == null) {
            ancestry = new Ancestry();
            child.setAncestry(ancestry);
        }
        Dog parent = new Dog();
        PedigreeUtils.populateDog(path.endNode(), parent);
        Relationship incomingHasParent = path.reverseRelationships().iterator().next();
        ParentRole role = ParentRole.valueOf(((String) incomingHasParent.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
        if (role == ParentRole.FATHER) {
            ancestry.setFather(parent);
        } else if (role == ParentRole.MOTHER) {
            ancestry.setMother(parent);
        }
        state.setState(parent);

        return expansion;
    }

    @Override
    public PathExpander<Dog> reverse() {
        throw new UnsupportedOperationException();
    }

}
