package no.nkk.dogpopulation.graph.dogbuilder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Dogs {

    private final BreedSynonymNodeCache breedSynonymNodeCache;

    public Dogs(BreedSynonymNodeCache breedSynonymNodeCache) {
        this.breedSynonymNodeCache = breedSynonymNodeCache;
    }

    public DogNodeBuilder dog() {
        return new DogNodeBuilder(breedSynonymNodeCache);
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
}
