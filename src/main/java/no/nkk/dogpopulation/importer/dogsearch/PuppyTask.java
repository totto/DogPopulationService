package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
* @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
*/
class PuppyTask implements Callable<Future<Relationship>> {


    private static final Logger LOGGER = LoggerFactory.getLogger(PuppyTask.class);


    /*
     * Services
     */

    private final GraphQueryService graphQueryService;
    private final Dogs dogs;
    private final BreedSynonymNodeCache breedSynonymNodeCache;
    private final DogSearchClient dogSearchClient;
    private final BulkWriteService bulkWriteService;


    /*
     * Required context
     */

    private final TraversalStatistics ts;
    private final DogDetails parentDogDetails;
    private final LitterNodeBuilder litterBuilder;
    private final DogPuppy dogPuppy;


    PuppyTask(GraphQueryService graphQueryService, Dogs dogs, BreedSynonymNodeCache breedSynonymNodeCache, DogSearchClient dogSearchClient, BulkWriteService bulkWriteService, TraversalStatistics ts, DogDetails parentDogDetails, LitterNodeBuilder litterBuilder, DogPuppy dogPuppy) {
        this.graphQueryService = graphQueryService;
        this.dogs = dogs;
        this.breedSynonymNodeCache = breedSynonymNodeCache;
        this.dogSearchClient = dogSearchClient;
        this.bulkWriteService = bulkWriteService;
        this.ts = ts;
        this.parentDogDetails = parentDogDetails;
        this.litterBuilder = litterBuilder;
        this.dogPuppy = dogPuppy;
    }

    @Override
    public Future<Relationship> call() throws Exception {
        Node dog = graphQueryService.getDog(dogPuppy.getId());
        InLitterRelationshipBuilder inLitterBuilder = configureInLitterBuilder(dog);
        if (inLitterBuilder == null) {
            return null;
        }
        Future<Relationship> future = bulkWriteService.build(inLitterBuilder);
        ts.graphBuildCount.incrementAndGet();
        ts.puppiesAdded.incrementAndGet();
        return future;
    }

    private InLitterRelationshipBuilder configureInLitterBuilder(Node dog) {
        InLitterRelationshipBuilder inLitterBuilder = dogs.inLitter().litter(litterBuilder);

        if (dog != null) {
            // dog node already present in graph
            ts.graphPuppyHit.incrementAndGet();
            inLitterBuilder.puppy(dog);
            return inLitterBuilder;
        }

        // dog node not present in graph
        ts.graphPuppyMiss.incrementAndGet();

        if (validUuid(dogPuppy.getId())) {
            // Valid UUID - create dog-node without consulting dogsearch
            inLitterBuilder.puppy(configurePuppyBuilder(dogPuppy.getId()));
            return inLitterBuilder;
        }

        // Not a valid UUID

        Future<DogDetails> puppyDetailsfuture = dogSearchClient.findDog(dogPuppy.getId());

        DogDetails puppyDetails = null;
        try {
            puppyDetails = puppyDetailsfuture.get();
        } catch (InterruptedException e) {
            LOGGER.error("", e);
        } catch (ExecutionException e) {
            LOGGER.error("", e);
        }

        if (puppyDetails != null) {
            // found dog on dogsearch
            ts.dogsearchPuppyHit.incrementAndGet();
            inLitterBuilder.puppy(configurePuppyBuilder(puppyDetails));
            return inLitterBuilder;
        }

        // id not known by dogsearch

        ts.dogsearchPuppyMiss.incrementAndGet();
        return null;
    }

    private DogNodeBuilder configurePuppyBuilder(DogDetails puppyDetails) {
        return dogs.dog(puppyDetails.getId()).all(puppyDetails);
    }

    private DogNodeBuilder configurePuppyBuilder(String uuid) {
        DogNodeBuilder puppyBuilder = dogs.dog(uuid).name(dogPuppy.getName());
        if (dogPuppy.getBreed() != null) {
            puppyBuilder.breed(breedSynonymNodeCache.getBreed(dogPuppy.getBreed().getName()));
        }
        return puppyBuilder;
    }

    private boolean validUuid(String id) {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }


    @Override
    public String toString() {
        return String.format("PuppyTask of dog %s", dogPuppy.getId());
    }

}
