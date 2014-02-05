package no.nkk.dogpopulation.graph;

import org.neo4j.graphdb.RelationshipType;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public enum DogGraphRelationshipType implements RelationshipType {
    IS_BREED,
    HAS_PARENT,
    MEMBER_OF,
    REGISTERED_IN
}
