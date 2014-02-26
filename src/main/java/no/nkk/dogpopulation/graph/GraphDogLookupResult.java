package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphDogLookupResult {
    private final Node dogNode;
    private final boolean atLeastOneParent;

    public GraphDogLookupResult(Node dogNode, boolean atLeastOneParent) {
        this.dogNode = dogNode;
        this.atLeastOneParent = atLeastOneParent;
    }

    public Node getDogNode() {
        return dogNode;
    }

    public boolean isAtLeastOneParent() {
        return atLeastOneParent;
    }
}
