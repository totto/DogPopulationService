package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class InLitterRelationshipBuilder extends AbstractRelationshipBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(InLitterRelationshipBuilder.class);

    private LitterNodeBuilder litterBuilder;
    private DogNodeBuilder puppyBuilder;
    private Node litter;
    private Node puppy;

    InLitterRelationshipBuilder() {
    }

    @Override
    protected Relationship doBuild(GraphDatabaseService graphDb) {
        if (litter == null) {
            litter = litterBuilder.build(graphDb);
        }
        if (puppy == null) {
            puppy = puppyBuilder.build(graphDb);
        }
        for (Relationship existingInLitter : puppy.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.IN_LITTER)) {
            Node existingLitter = existingInLitter.getEndNode();
            if (existingLitter.equals(this.litter)) {
                return existingInLitter; // already connected to correct litter
            }
            String puppyUuid = (String) puppy.getProperty(DogGraphConstants.DOG_UUID);
            String existingLitterId = (String) existingLitter.getProperty(DogGraphConstants.LITTER_ID);
            String litterId = (String) this.litter.getProperty(DogGraphConstants.LITTER_ID);
            LOGGER.warn("LITTER CONFLICT: Dog {} is already in litter {} but will now be moved to litter {}.", puppyUuid, existingLitterId, litterId);
            existingInLitter.delete();
        }
        return puppy.createRelationshipTo(this.litter, DogGraphRelationshipType.IN_LITTER);
    }


    public InLitterRelationshipBuilder litter(Node litter) {
        this.litter = litter;
        return this;
    }
    public InLitterRelationshipBuilder puppy(Node puppy) {
        this.puppy = puppy;
        return this;
    }
    public InLitterRelationshipBuilder litter(LitterNodeBuilder litterBuilder) {
        this.litterBuilder = litterBuilder;
        return this;
    }
    public InLitterRelationshipBuilder puppy(DogNodeBuilder puppyBuilder) {
        this.puppyBuilder = puppyBuilder;
        return this;
    }
}