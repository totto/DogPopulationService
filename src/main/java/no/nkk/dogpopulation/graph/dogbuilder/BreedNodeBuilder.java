package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedNodeBuilder extends AbstractNodeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreedNodeBuilder.class);

    private final CommonNodes commonNodes;

    private String id;
    private String name;

    BreedNodeBuilder(CommonNodes commonNodes) {
        this.commonNodes = commonNodes;
    }

    @Override
    protected Node doBuild(GraphDatabaseService graphDb) {
        if (name == null) {
            throw new MissingFieldException("name");
        }

        Node breedNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, name);
        if (id != null && id.trim().length() > 0) {
            if (breedNode.hasProperty(DogGraphConstants.BREED_ID)) {
                String existingBreedId = (String) breedNode.getProperty(DogGraphConstants.BREED_ID);
                if (!existingBreedId.equals(id)) {
                    LOGGER.warn("Breed-ID conflict: ID \"{}\" (in graph) and \"{}\" are both assigned to breed \"{}\"", existingBreedId, id, name);
                }
            } else {
                LOGGER.trace("Added breed.id={} to \"{}\"", id, name);
                breedNode.setProperty(DogGraphConstants.BREED_ID, id);
            }
        }
        if (!breedNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breedNode.createRelationshipTo(commonNodes.getBreedCategory(), DogGraphRelationshipType.MEMBER_OF);
        }
        return breedNode;
    }


    public BreedNodeBuilder id(String id) {
        if (id != null) {
            this.id = id.trim();
        }
        return this;
    }
    public BreedNodeBuilder name(String name) {
        if (name != null) {
            this.name = name.trim();
        }
        return this;
    }

}
