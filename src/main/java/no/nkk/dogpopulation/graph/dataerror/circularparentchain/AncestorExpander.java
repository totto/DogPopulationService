package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class AncestorExpander implements PathExpander<Object> {

    private final Set<Relationship> alreadyVisited;
    private final Set<Relationship> visited = new LinkedHashSet<>();

    public AncestorExpander(Set<Relationship> alreadyVisited) {
        this.alreadyVisited = alreadyVisited;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Object> state) {
        Iterable<Relationship> parents = path.endNode().getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);

        List<Relationship> expansion = new ArrayList<>(2);
        for (Relationship r : parents) {
            if (alreadyVisited.contains(r)) {
                continue;
            }
            visited.add(r);
            expansion.add(r);
        }

        return expansion;
    }

    @Override
    public PathExpander<Object> reverse() {
        throw new UnsupportedOperationException();
    }

    Set<Relationship> getVisited() {
        return visited;
    }
}
