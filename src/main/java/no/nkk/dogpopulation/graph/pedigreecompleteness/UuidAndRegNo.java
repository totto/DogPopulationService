package no.nkk.dogpopulation.graph.pedigreecompleteness;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class UuidAndRegNo {
    private final String uuid;
    private final String regNo;

    public UuidAndRegNo(Node dogNode) {
        uuid = (String) dogNode.getProperty(DogGraphConstants.DOG_UUID);
        if (dogNode.hasProperty(DogGraphConstants.DOG_REGNO)) {
            regNo = (String) dogNode.getProperty(DogGraphConstants.DOG_REGNO);
        } else {
            regNo = null;
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getRegNo() {
        return regNo;
    }
}
