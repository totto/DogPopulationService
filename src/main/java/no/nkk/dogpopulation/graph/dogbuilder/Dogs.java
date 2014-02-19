package no.nkk.dogpopulation.graph.dogbuilder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Dogs {

    private final CommonNodes commonNodes;

    public Dogs(CommonNodes commonNodes) {
        this.commonNodes = commonNodes;
    }

    public DogNodeBuilder dog() {
        return new DogNodeBuilder(commonNodes);
    }

    public LitterNodeBuilder litter() {
        return new LitterNodeBuilder();
    }

    public HasLitterRelationshipBuilder hasLitter() {
        return new HasLitterRelationshipBuilder();
    }

    public HasParentRelationshipBuilder hasParent() {
        return new HasParentRelationshipBuilder();
    }

    public InLitterRelationshipBuilder inLitter() {
        return new InLitterRelationshipBuilder();
    }

    public IsBreedRelationshipBuilder isBreed() {
        return new IsBreedRelationshipBuilder();
    }
}
