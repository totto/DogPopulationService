package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.Builder;
import no.nkk.dogpopulation.graph.GraphDogLookupResult;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import no.nkk.dogpopulation.graph.bulkwrite.BuilderProgress;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.*;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchPedigreeImporter implements PedigreeImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchPedigreeImporter.class);

    private final GraphDatabaseService graphDb;
    private final GraphQueryService graphQueryService;
    private final DogSearchClient dogSearchClient;

    private final Dogs dogs;
    private final CommonNodes commonNodes;

    private final ExecutorService executorService;

    private final BulkWriteService bulkWriteService;

    public DogSearchPedigreeImporter(ExecutorService executorService, GraphDatabaseService graphDb, DogSearchClient dogSearchClient, Dogs dogs, CommonNodes commonNodes, BulkWriteService bulkWriteService) {
        this.executorService = executorService;
        this.graphDb = graphDb;
        this.graphQueryService = new GraphQueryService(graphDb);
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.commonNodes = commonNodes;
        this.bulkWriteService = bulkWriteService;
    }


    @Override
    public Future<String> importPedigree(final String id) {
        return executorService.submit(pedigreeImportTaskFor(id));
    }

    Callable<String> pedigreeImportTaskFor(final String id) {
        return new Callable<String>() {
            @Override
            public String call() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(id);
                try {
                    TraversalStatistics ts = importDogPedigree(id);
                    if (ts == null) {
                        return null;
                    }
                    return ts.id;
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        };
    }

    @Override
    public TraversalStatistics importDogPedigree(String id) {
        return importDogPedigree(id, new TraversalStatistics(id));
    }

    @Override
    public void stop() {
        bulkWriteService.stop();
    }

    public TraversalStatistics importDogPedigree(String id, TraversalStatistics ts) {
        long startTime = System.currentTimeMillis();
        LOGGER.trace("Importing Pedigree from DogSearch for dog {}", id);
        DogFuture dogFuture = depthFirstDogImport(id, ts, 1);
        dogFuture.waitForPedigreeImportToComplete();
        long durationMs = System.currentTimeMillis() - startTime;
        double duration = durationMs / 1000;
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
        LOGGER.trace("Imported Pedigree of dog {} in {} seconds, {} Dogs/sec. -- {}", id, decimalFormat.format(duration), decimalFormat.format(1000.0 * (ts.dogsAdded.get() + ts.puppiesAdded.get()) / durationMs));
        return ts;
    }

    public DogFuture depthFirstDogImport(String id, TraversalStatistics ts, int depth) {
        DogLookupResult lookupResult = lookupDog(id, ts);

        GraphDogLookupResult graphResult = lookupResult.getGraphResult();
        DogDetails dogDetails = lookupResult.getDogsearchResult();

        if (graphResult == null && dogDetails == null ) {
            // Dog does not exists in graph, nor on dogsearch. Assume that this is the end of the pedigree.
            recordMinimumDepth(ts, depth);
            return null;
        }

        recordMaximumDepth(ts, depth);

        if (dogDetails == null) {
            // dog already exists in graph
            DogNodeBuilder builder = dogs.dog().build(graphResult.getDogNode());
            return new DogFuture(builder, new ImmediateFuture<>(graphResult.getDogNode()));
        }

        // dog should be added or updated with details from dogsearch

        BuilderProgress progress = bulkWriteService.build(id, dogs.dog().all(dogDetails)); // add dog to graph

        Builder<Node> dogBuilder = progress.getBuilder();

        if (progress.isAlreadyInProgress()) {
            // dog is right now being imported by another thread
            return new DogFuture(dogBuilder, progress.getFuture());
        }

        if (graphResult != null && !graphResult.isAtLeastOneParent()) {
            ts.dogsUpdated.incrementAndGet(); // count that dog was updated in graph
        } else {
            ts.dogsAdded.incrementAndGet(); // count that dog was added to graph
        }

        DogAncestry ancestry = dogDetails.getAncestry();
        if (ancestry == null) {
            LOGGER.trace("DOG is missing ancestry {}", id);
            return new DogFuture(dogBuilder, progress.getFuture());
        }

        /*
         * Recursively add parents
         */

        Future<DogFuture> fatherFuture = importParentAsync(ts, depth + 1, ancestry.getFather(), ParentRole.FATHER, dogBuilder);

        Future<DogFuture> motherFuture = importParentAsync(ts, depth + 1, ancestry.getMother(), ParentRole.MOTHER, dogBuilder);

        DogLitter litter = ancestry.getLitter();
        addDogInLitter(dogBuilder, litter);

        // add offspring, but queue up import requests on each puppy rather than adding them recursively.
        Future<Future<Relationship>>[] puppyFutureArr = addOffspring(ts, dogBuilder, dogDetails);

        DogFuture dogFuture = new DogFuture(dogBuilder, progress.getFuture(), fatherFuture, motherFuture, puppyFutureArr);

        return dogFuture;
    }

    private Future<DogFuture> importParentAsync(TraversalStatistics ts, int depth, DogParent parent, ParentRole parentRole, Builder<Node> childBuilder) {
        if (parent == null) {
            return null;
        }
        return executorService.submit(new ParentTask(ts, childBuilder, parent, parentRole, depth));
    }

    private class ParentTask implements Callable<DogFuture> {
        private final TraversalStatistics ts;
        private final Builder<Node> childBuilder;
        private final DogParent parent;
        private final ParentRole parentRole;
        private final int depth;

        ParentTask(TraversalStatistics ts, Builder<Node> childBuilder, DogParent parent, ParentRole parentRole, int depth) {
            this.ts = ts;
            this.childBuilder = childBuilder;
            this.parent = parent;
            this.parentRole = parentRole;
            this.depth = depth;
        }

        @Override
        public DogFuture call() throws Exception {
            DogFuture dogFuture = depthFirstDogImport(parent.getId(), ts, depth);

            if (dogFuture == null) {
                return null;
            }

            HasParentRelationshipBuilder hasParentBuilder = dogs.hasParent().child(childBuilder).parent(dogFuture.getDogBuilder()).role(parentRole);

            bulkWriteService.build(hasParentBuilder);

            if (parentRole == ParentRole.FATHER) {
                ts.fathersAdded.incrementAndGet();
            } else {
                ts.mothersAdded.incrementAndGet();
            }

            return dogFuture;
        }
    }

    private DogLookupResult lookupDog(String id, TraversalStatistics ts) {
        /*
         * Check whether dos is already in graph first in order to avoid more expensive dogsearch lookup
         */
        GraphDogLookupResult graphDogLookupResult = graphQueryService.lookupDog(id);
        if (graphDogLookupResult != null) {
            ts.graphHit.incrementAndGet();
            if (graphDogLookupResult.isAtLeastOneParent()) {
                return new DogLookupResult(null, graphDogLookupResult); // parents already being/been traversed
            }
        } else {
            ts.graphMiss.incrementAndGet();
        }

        // dog is not present in graph, lookup externally on dogsearch

        DogDetails searchResult = dogSearchClient.findDog(id);  // external search

        if (searchResult == null) {
            LOGGER.trace("LOOKUP - Not found on dogsearch: {}", id);
            ts.dogsearchMiss.incrementAndGet();
            return new DogLookupResult(null, graphDogLookupResult);
        }
        ts.dogsearchHit.incrementAndGet();

        return new DogLookupResult(searchResult, graphDogLookupResult);
    }

    private void addDogInLitter(Builder<Node> dogBuilder, DogLitter litter) {
        if (litter == null) {
            return;
        }
        String litterId = litter.getId();
        if (litterId == null) {
            return;
        }
        bulkWriteService.build(dogs.inLitter().litter(dogs.litter().id(litterId)).puppy(dogBuilder));
    }

    private void recordMinimumDepth(TraversalStatistics ts, int depth) {
        if ((depth - 1) < ts.minDepth.get()) {
            ts.minDepth.set(depth - 1); // TODO perform this check-then-add operation in atomically
        }
    }

    private void recordMaximumDepth(TraversalStatistics ts, int depth) {
        if (depth > ts.maxDepth.get()) {
            ts.maxDepth.set(depth); // TODO perform this check-then-add operation in atomically
        }
    }

    private Future<Future<Relationship>>[] addOffspring(TraversalStatistics ts, Builder<Node> parentBuilder, DogDetails dogDetails) {
        ParentRole parentRole = null;
        String gender = dogDetails.getGender();
        if (gender != null) {
            if (gender.equalsIgnoreCase("male")) {
                parentRole = ParentRole.FATHER;
            } else if (gender.equalsIgnoreCase("female")) {
                parentRole = ParentRole.MOTHER;
            }
        }

        List<Future<Future<Relationship>>> puppyFutureList = new ArrayList<>();

        DogOffspring[] offspringArr = dogDetails.getOffspring();
        if (offspringArr == null || offspringArr.length <= 0) {
            return new Future[0]; // no offspring
        }

        for (DogOffspring offspring : offspringArr) {
            String litterId = offspring.getId();
            LocalDate litterBorn = LocalDate.parse(offspring.getBorn());
            int count = offspring.getCount();
            LitterNodeBuilder litterBuilder = dogs.litter().id(litterId).born(litterBorn).count(count);
            Future<Relationship> hasLitterFuture = bulkWriteService.build(dogs.hasLitter().litter(litterBuilder).parent(parentBuilder).role(parentRole));
            ts.litterCount.incrementAndGet();
            puppyFutureList.add(new ImmediateFuture<>(hasLitterFuture));
            for (DogPuppy dogPuppy : offspring.getPuppies()) {
                Callable<Future<Relationship>> puppyTask = new PuppyTask(ts, dogDetails, litterBuilder, dogPuppy);
                puppyFutureList.add(executorService.submit(puppyTask));
            }
        }

        Future<Future<Relationship>>[] puppyFutures = puppyFutureList.toArray(new Future[puppyFutureList.size()]);
        return puppyFutures;
    }

    private class PuppyTask implements Callable<Future<Relationship>> {
        private final TraversalStatistics ts;
        private final DogDetails dogDetails;
        private final LitterNodeBuilder litterBuilder;
        private final DogPuppy dogPuppy;

        private PuppyTask(TraversalStatistics ts, DogDetails dogDetails, LitterNodeBuilder litterBuilder, DogPuppy dogPuppy) {
            this.ts = ts;
            this.dogDetails = dogDetails;
            this.litterBuilder = litterBuilder;
            this.dogPuppy = dogPuppy;
        }

        @Override
        public Future<Relationship> call() throws Exception {
            Node dog = graphQueryService.getDog(dogPuppy.getId());
            InLitterRelationshipBuilder inLitterBuilder = dogs.inLitter().litter(litterBuilder);
            if (dog != null) {
                ts.graphPuppyHit.incrementAndGet();
                inLitterBuilder.puppy(dog);
            } else {
                ts.graphPuppyMiss.incrementAndGet();
                boolean validUuid = false;
                try {
                    UUID.fromString(dogPuppy.getId());
                    validUuid = true;
                } catch (IllegalArgumentException ignore) {
                }
                if (validUuid) {
                    DogNodeBuilder puppyBuilder = dogs.dog().uuid(dogPuppy.getId()).name(dogPuppy.getName());
                    if (dogPuppy.getBreed() != null) {
                        puppyBuilder.breed(commonNodes.getBreed(dogPuppy.getBreed().getName(), dogPuppy.getBreed().getId()));
                    }
                    inLitterBuilder.puppy(puppyBuilder);
                } else {
                    // Not a valid UUID
                    DogDetails puppyDetails = dogSearchClient.findDog(dogPuppy.getId());
                    if (puppyDetails == null) {
                        ts.dogsearchPuppyMiss.incrementAndGet();
                        return null;
                    }
                    ts.dogsearchPuppyHit.incrementAndGet();
                    inLitterBuilder.puppy(dogs.dog().all(puppyDetails));
                }
            }
            Future<Relationship> future = bulkWriteService.build(inLitterBuilder);
            ts.puppiesAdded.incrementAndGet();
            return future;
        }
    }

}
