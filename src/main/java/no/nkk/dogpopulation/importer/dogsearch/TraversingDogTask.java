package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.concurrent.ExecutorServiceHelper;
import no.nkk.dogpopulation.graph.Builder;
import no.nkk.dogpopulation.graph.GraphDogLookupResult;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import no.nkk.dogpopulation.graph.bulkwrite.BuilderProgress;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.dogbuilder.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Responsible for importing a dog from dogsearch to graph. This task will also queue new tasks to import this dog's
 * mother and father and offspring.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
class TraversingDogTask implements Callable<DogFuture> {


    private static final Logger LOGGER = LoggerFactory.getLogger(TraversingDogTask.class);


    /*
     * Services
     */

    private final GraphDatabaseService graphDb;
    private final GraphQueryService graphQueryService;
    private final DogSearchClient dogSearchClient;
    private final Dogs dogs;
    private final BreedSynonymNodeCache breedSynonymNodeCache;
    private final ExecutorService traverserExecutor;
    private final ExecutorServiceHelper traverserExecutorHelper;
    private final ExecutorService graphQueryExecutor;
    private final BulkWriteService bulkWriteService;


    /*
     * Required context
     */
    private final String id; // UUID or RegNo of dog
    private final TraversalStatistics ts; // running statistics, usually shared by many other tasks and threads
    private final int depth; // how far back into a pedigree we have traversed

    /*
     * Optional context, used in tasks queued while traversing ancestry
     */
    private final Builder<Node> childBuilder; // the builder of the child-dog of this dog
    private final ParentRole parentRole; // the parent-role this dog has in relation to the child-dog


    TraversingDogTask(
            ExecutorService graphQueryExecutor,
            ExecutorService traverserExecutor,
            GraphDatabaseService graphDb,
            DogSearchClient dogSearchClient,
            Dogs dogs,
            BreedSynonymNodeCache breedSynonymNodeCache,
            BulkWriteService bulkWriteService,
            String id,
            TraversalStatistics ts,
            int depth) {
        this.graphQueryExecutor = graphQueryExecutor;
        this.traverserExecutor = traverserExecutor;
        this.graphDb = graphDb;
        this.graphQueryService = new GraphQueryService(graphDb);
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.breedSynonymNodeCache = breedSynonymNodeCache;
        this.bulkWriteService = bulkWriteService;
        this.id = id;
        this.ts = ts;
        this.depth = depth;
        this.traverserExecutorHelper = new ExecutorServiceHelper(traverserExecutor);
        this.childBuilder = null;
        this.parentRole = null;
    }


    /**
     * Used to spawn parent-tasks from an existing task.
     *
     * @param child
     * @param id
     * @param childBuilder
     * @param parentRole
     */
    private TraversingDogTask(TraversingDogTask child, String id, Builder<Node> childBuilder, ParentRole parentRole) {
        this.graphQueryExecutor = child.graphQueryExecutor;
        this.traverserExecutor = child.traverserExecutor;
        this.graphDb = child.graphDb;
        this.graphQueryService = child.graphQueryService;
        this.dogSearchClient = child.dogSearchClient;
        this.dogs = child.dogs;
        this.breedSynonymNodeCache = child.breedSynonymNodeCache;
        this.bulkWriteService = child.bulkWriteService;
        this.id = id;
        this.ts = child.ts;
        this.depth = child.depth + 1;
        this.traverserExecutorHelper = child.traverserExecutorHelper;
        this.childBuilder = childBuilder;
        this.parentRole = parentRole;
    }


    @Override
    public DogFuture call() throws Exception {
        DogFuture dogFuture = processDog();

        if (dogFuture == null) {
            return null;
        }

        if (childBuilder == null) {
            return dogFuture; // this task was not spawned as part of the ancestry of another task.
        }

        // task has optional context, connect child with parent

        buildHasParentRelationship(dogFuture.getDogBuilder());

        return dogFuture;

    }

    private DogFuture processDog() {
        DogLookupResult lookupResult = lookupDog();

        GraphDogLookupResult graphResult = lookupResult.getGraphResult();
        DogDetails dogDetails = lookupResult.getDogsearchResult();

        if (graphResult == null && dogDetails == null ) {
            // Dog does not exists in graph, nor on dogsearch. Assume that this is the end of the pedigree.
            ts.recordMinimumDepth(depth);
            return null;
        }

        ts.recordMaximumDepth(depth);

        if (graphResult != null && graphResult.isUpToDate()) {
            // dog already exists in graph and is up-to-date
            DogNodeBuilder builder = dogs.dog(id).build(graphResult.getDogNode());
            return new DogFuture(builder, new ImmediateFuture<>(graphResult.getDogNode()));
        }

        // dog should be added or updated with details from dogsearch

        BuilderProgress progress = bulkWriteService.build(id, dogs.dog(dogDetails.getId()).all(dogDetails)); // add dog to graph

        Builder<Node> dogBuilder = progress.getBuilder();

        if (progress.isAlreadyInProgress()) {
            // dog is right now being imported by another thread
            return new DogFuture(dogBuilder, progress.getFuture());
        }

        ts.graphBuildCount.incrementAndGet();

        if (graphResult != null && !graphResult.isUpToDate()) {
            ts.dogsUpdated.incrementAndGet(); // count that dog was updated in graph
        } else {
            ts.dogsAdded.incrementAndGet(); // count that dog was added to graph
        }

        DogAncestry ancestry = dogDetails.getAncestry();
        if (ancestry == null) {
            LOGGER.trace("DOG is missing ancestry {}", id);
            if (graphResult != null) {
                // delete potential parent links in graph
                bulkWriteService.build(dogs.deleteParent().role(ParentRole.FATHER).child(dogBuilder));
                ts.graphBuildCount.incrementAndGet();
                bulkWriteService.build(dogs.deleteParent().role(ParentRole.MOTHER).child(dogBuilder));
                ts.graphBuildCount.incrementAndGet();
            }
            return new DogFuture(dogBuilder, progress.getFuture());
        }

        /*
         * Traverse parents as separate tasks
         */

        Future<DogFuture> fatherFuture = traverserExecutorHelper.submit(createParentTask(graphResult, ancestry.getFather(), ParentRole.FATHER, dogBuilder));

        Future<DogFuture> motherFuture = traverserExecutorHelper.submit(createParentTask(graphResult, ancestry.getMother(), ParentRole.MOTHER, dogBuilder));

        DogLitter litter = ancestry.getLitter();
        addDogInLitter(dogBuilder, litter);


        /*
         * Traverse offspring (only one level down)
         */

        List<Future<Future<Relationship>>> puppyFutures = traverserExecutorHelper.submit(createOffspringTasks(dogBuilder, dogDetails));


        /*
         * Return a future
         */

        DogFuture dogFuture = new DogFuture(dogBuilder, progress.getFuture(), fatherFuture, motherFuture, puppyFutures);

        return dogFuture;
    }

    private void buildHasParentRelationship(Builder<Node> parentBuilder) {
        HasParentRelationshipBuilder hasParentBuilder = dogs.hasParent().child(childBuilder).parent(parentBuilder).role(parentRole);

        bulkWriteService.build(hasParentBuilder);
        ts.graphBuildCount.incrementAndGet();

        if (parentRole == ParentRole.FATHER) {
            ts.fathersAdded.incrementAndGet();
        } else {
            ts.mothersAdded.incrementAndGet();
        }
    }

    private TraversingDogTask createParentTask(GraphDogLookupResult graphResult, DogParent parent, ParentRole role, Builder<Node> dogBuilder) {
        if (parent == null) {
            if (graphResult != null) {
                // delete possible parent relationship in graph
                bulkWriteService.build(dogs.deleteParent().role(role).child(dogBuilder));
                ts.graphBuildCount.incrementAndGet();
            }
            return null;
        }
        return new TraversingDogTask(this, parent.getId(), dogBuilder, role);
    }

    private DogLookupResult lookupDog() {
        // lookup externally on dogsearch
        Future<DogDetails> searchResultFuture = dogSearchClient.findDog(id);

        DogDetails searchResult = null;
        if (searchResultFuture != null) {
            try {
                searchResult = searchResultFuture.get();
            } catch (InterruptedException e) {
                LOGGER.error("", e);
            } catch (ExecutionException e) {
                LOGGER.error("", e);
            }
        }

        if (searchResult == null) {
            LOGGER.trace("LOOKUP - Not found on dogsearch: {}", id);
            ts.dogsearchMiss.incrementAndGet();
            return new DogLookupResult(searchResult);
        }
        ts.dogsearchHit.incrementAndGet();

        /*
         * Lookup dog in graph
         */
        final DogDetails dogDetails = searchResult;
        Future<GraphDogLookupResult> graphQueryFuture = graphQueryExecutor.submit(new Callable<GraphDogLookupResult>() {
            @Override
            public GraphDogLookupResult call() throws Exception {
                return graphQueryService.lookupDogIfUpToDate(id, dogDetails);
            }
        });
        GraphDogLookupResult graphDogLookupResult = null;
        try {
            graphDogLookupResult = graphQueryFuture.get();
        } catch (InterruptedException e) {
            LOGGER.error("", e);
        } catch (ExecutionException e) {
            LOGGER.error("", e);
        }
        if (graphDogLookupResult != null && graphDogLookupResult.isUpToDate()) {
            ts.graphHit.incrementAndGet();
        } else {
            ts.graphMiss.incrementAndGet();
        }

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
        // TODO Disable this linking for now, as it creates way too many dogs connected to some litter nodes.
        // This indicates that there are problems with duplicate litter-ids.
        // bulkWriteService.build(dogs.inLitter().litter(dogs.litter().id(litterId)).puppy(dogBuilder));
    }

    private List<Callable<Future<Relationship>>> createOffspringTasks(Builder<Node> parentBuilder, DogDetails dogDetails) {
        DogOffspring[] offspringArr = dogDetails.getOffspring();
        if (offspringArr == null || offspringArr.length <= 0) {
            return Collections.emptyList(); // no offspring
        }

        ParentRole parentRole = getParentRole(dogDetails.getGender());

        List<Callable<Future<Relationship>>> puppyTasks = new ArrayList<>();

        for (DogOffspring offspring : offspringArr) {
            String litterId = offspring.getId();
            String parentUuid = ((DogNodeBuilder) parentBuilder).uuid();
            Integer count = offspring.getCount();
            String born = offspring.getBorn();
            LitterNodeBuilder litterBuilder = dogs.litter().id(litterId).parentId(parentUuid).count(count).born(born);
            Future<Relationship> hasLitterFuture = bulkWriteService.build(dogs.hasLitter().litter(litterBuilder).parent(parentBuilder).role(parentRole));
            ts.graphBuildCount.incrementAndGet();
            ts.litterCount.incrementAndGet();
            puppyTasks.add(new ImmediateTask<>(hasLitterFuture));
            for (DogPuppy dogPuppy : offspring.getPuppies()) {
                Callable<Future<Relationship>> puppyTask = createPuppyTask(ts, dogDetails, litterBuilder, dogPuppy);
                puppyTasks.add(puppyTask);
            }
        }

        return puppyTasks;
    }

    private ParentRole getParentRole(String gender) {
        ParentRole parentRole = null;
        if (gender != null) {
            if (gender.equalsIgnoreCase("male")) {
                parentRole = ParentRole.FATHER;
            } else if (gender.equalsIgnoreCase("female")) {
                parentRole = ParentRole.MOTHER;
            }
        }
        return parentRole;
    }

    private PuppyTask createPuppyTask(TraversalStatistics ts, DogDetails parentDogDetails, LitterNodeBuilder litterBuilder, DogPuppy dogPuppy) {
        return new PuppyTask(graphQueryService, dogs, breedSynonymNodeCache, dogSearchClient, bulkWriteService, ts, parentDogDetails, litterBuilder, dogPuppy);
    }

    @Override
    public String toString() {
        String optionalParentOfStr = (parentRole == null) ? "" : parentRole.name() + " of " + ((DogNodeBuilder) childBuilder).uuid();
        return String.format("TraversingDogTask (%s) of dog %s", optionalParentOfStr, id);
    }
}
