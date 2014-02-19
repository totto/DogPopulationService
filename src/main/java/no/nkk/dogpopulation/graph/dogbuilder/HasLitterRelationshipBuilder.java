package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.ParentRole;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HasLitterRelationshipBuilder extends AbstractRelationshipBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HasLitterRelationshipBuilder.class);

    private ParentRole parentRole;
    private Node parent;
    private Node litter;
    private LitterNodeBuilder litterBuilder;

    HasLitterRelationshipBuilder() {
    }

    @Override
    protected Relationship doBuild(GraphDatabaseService graphDb) {
        if (litter == null) {
            litter = litterBuilder.build(graphDb);
        }
        for (Relationship existingHasLitter : parent.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
            Node existingLitter = existingHasLitter.getEndNode();
            if (existingLitter.equals(litter)) {
                // already connected to correct litter
                if (parentRole != null) {
                    if (existingHasLitter.hasProperty(DogGraphConstants.HASLITTER_ROLE)) {
                        ParentRole existingRole = ParentRole.valueOf(((String) existingHasLitter.getProperty(DogGraphConstants.HASLITTER_ROLE)).toUpperCase());
                        if (!existingRole.equals(parentRole)) {
                            String parentUuid = (String) parent.getProperty(DogGraphConstants.DOG_UUID);
                            LOGGER.warn("Inconsistent HAS_LITTER role. Dog {} has now changed role to {}", parentUuid, parentRole.name().toLowerCase());
                            existingHasLitter.setProperty(DogGraphConstants.HASLITTER_ROLE, parentRole.name().toLowerCase());
                        }
                    } else {
                        // role was missing before
                        existingHasLitter.setProperty(DogGraphConstants.HASLITTER_ROLE, parentRole.name().toLowerCase());
                    }
                }
                return existingHasLitter;
            }
        }
        Relationship hasLitter = parent.createRelationshipTo(litter, DogGraphRelationshipType.HAS_LITTER);
        if (parentRole != null) {
            hasLitter.setProperty(DogGraphConstants.HASLITTER_ROLE, parentRole.name().toLowerCase());
        }
        return hasLitter;
    }


    public HasLitterRelationshipBuilder parent(Node parent) {
        this.parent = parent;
        return this;
    }
    public HasLitterRelationshipBuilder role(ParentRole parentRole) {
        this.parentRole = parentRole;
        return this;
    }
    public HasLitterRelationshipBuilder litter(Node litter) {
        this.litter = litter;
        return this;
    }
    public HasLitterRelationshipBuilder litter(LitterNodeBuilder litterBuilder) {
        this.litterBuilder = litterBuilder;
        return this;
    }

}
