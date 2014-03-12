package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedGroupBuilder extends AbstractNodeBuilder {

    private String fciBreedGroup;
    private Node categoryNode;

    @Override
    protected Node doBuild(GraphDatabaseService graphDb) {
        if (fciBreedGroup == null) {
            throw new MissingFieldException("fciBreedGroup");
        }
        Node breedGroupNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED_GROUP, DogGraphConstants.BREEDGROUP_FCIBREEDGROUP, fciBreedGroup);
        return breedGroupNode;
    }

    public BreedGroupBuilder fciBreedGroup(String fciBreedGroup) {
        if (fciBreedGroup == null) {
            throw new IllegalArgumentException("fciBreedGroup cannot be null");
        }
        String trimmedFciBreedGroup = fciBreedGroup.trim();
        if (trimmedFciBreedGroup.isEmpty()) {
            throw new IllegalArgumentException("fciBreedGroup cannot be an empty string");
        }
        this.fciBreedGroup = trimmedFciBreedGroup;
        return this;
    }
    public BreedGroupBuilder category(Node categoryNode) {
        this.categoryNode = categoryNode;
        return this;
    }
}
