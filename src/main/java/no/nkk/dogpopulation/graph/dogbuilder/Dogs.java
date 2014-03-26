package no.nkk.dogpopulation.graph.dogbuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class Dogs {

    private final BreedSynonymNodeCache breedSynonymNodeCache;

    @Inject
    public Dogs(BreedSynonymNodeCache breedSynonymNodeCache) {
        this.breedSynonymNodeCache = breedSynonymNodeCache;
    }

    public DogNodeBuilder dog(String uuid) {
        return new DogNodeBuilder(breedSynonymNodeCache, uuid);
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

    public HasParentRelationshipDeleteBuilder deleteParent() {
        return new HasParentRelationshipDeleteBuilder();
    }

    public InLitterRelationshipBuilder inLitter() {
        return new InLitterRelationshipBuilder();
    }
}
