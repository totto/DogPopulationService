package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.*;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class HasParentRelationshipBuilder extends AbstractRelationshipBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HasParentRelationshipBuilder.class);

    private DogNodeBuilder childBuilder;
    private DogNodeBuilder parentBuilder;
    private Node child;
    private Node parent;
    private String childUuid;
    private String parentUuid;
    private ParentRole parentRole;
    private RelationshipType relationshipType = DogGraphRelationshipType.HAS_PARENT;

    HasParentRelationshipBuilder() {
    }

    @Override
    protected Relationship doBuild(GraphDatabaseService graphDb) {
        if (child == null) {
            if (childBuilder != null) {
                child = childBuilder.build(graphDb);
            } else {
                if (childUuid == null) {
                    throw new MissingFieldException("child/uuid");
                }
                child = GraphUtils.getSingleNode(graphDb, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, childUuid);
            }
        }
        if (parent == null) {
            if (parentBuilder != null) {
                parent = parentBuilder.build(graphDb);
            } else {
                if (parentUuid == null) {
                    throw new MissingFieldException("parent/uuid");
                }
                parent = GraphUtils.getSingleNode(graphDb, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, parentUuid);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            if (childUuid == null) {
                childUuid = (String) child.getProperty(DogGraphConstants.DOG_UUID);
            }
            if (parentUuid == null) {
                parentUuid = (String) parent.getProperty(DogGraphConstants.DOG_UUID);
            }
            LOGGER.trace("Connected DOG with {}: child:{}, parent:{}", parentRole, childUuid, parentUuid);
        }

        Iterable<Relationship> parentRelationshipIterator = child.getRelationships(relationshipType, Direction.OUTGOING);
        for (Relationship relationship : parentRelationshipIterator) {
            // iterate through parents already known by graph
            ParentRole existingParentRole = ParentRole.valueOf(((String) relationship.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            if (parentRole.equals(existingParentRole)) {
                Node existingParent = relationship.getEndNode();
                if (existingParent.equals(parent)) {
                    return relationship; // the child and parent is already connected
                }
                // child-parent relationship in graph is wrong
                relationship.delete();
                break;
            }
        }
        Relationship relationship = child.createRelationshipTo(parent, relationshipType);
        relationship.setProperty(DogGraphConstants.HASPARENT_ROLE, parentRole.name().toLowerCase());
        return relationship;
    }


    public HasParentRelationshipBuilder child(Node child) {
        this.child = child;
        return this;
    }
    public HasParentRelationshipBuilder parent(Node parent) {
        this.parent = parent;
        return this;
    }
    public HasParentRelationshipBuilder child(String childUuid) {
        this.childUuid = childUuid;
        return this;
    }
    public HasParentRelationshipBuilder parent(String parentUuid) {
        this.parentUuid = parentUuid;
        return this;
    }
    public HasParentRelationshipBuilder father(String fatherUuid) {
        this.parentUuid = fatherUuid;
        this.parentRole = ParentRole.FATHER;
        return this;
    }
    public HasParentRelationshipBuilder mother(String motherUuid) {
        this.parentUuid = motherUuid;
        this.parentRole = ParentRole.MOTHER;
        return this;
    }
    public HasParentRelationshipBuilder role(ParentRole parentRole) {
        this.parentRole = parentRole;
        return this;
    }
    public HasParentRelationshipBuilder ownAncestor() {
        relationshipType = DogGraphRelationshipType.OWN_ANCESTOR;
        return this;
    }
    public HasParentRelationshipBuilder child(DogNodeBuilder childBuilder) {
        this.childBuilder = childBuilder;
        return this;
    }
    public HasParentRelationshipBuilder parent(DogNodeBuilder parentBuilder) {
        this.parentBuilder = parentBuilder;
        return this;
    }
}
