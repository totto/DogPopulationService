package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphDogLookupResult {
    private final Node dogNode;
    private final boolean upToDate;

    public GraphDogLookupResult(Node dogNode, boolean upToDate) {
        this.dogNode = dogNode;
        this.upToDate = upToDate;
    }

    public Node getDogNode() {
        return dogNode;
    }

    public boolean isUpToDate() {
        return upToDate;
    }
}
