package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import no.nkk.dogpopulation.graph.dogbuilder.*;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchPedigreeImporter implements PedigreeImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchPedigreeImporter.class);
    private static final Logger DOGSEARCH = LoggerFactory.getLogger("dogsearch");

    private final GraphDatabaseService graphDb;
    private final GraphQueryService graphQueryService;
    private final DogSearchClient dogSearchClient;

    private final Dogs dogs;
    private final CommonNodes commonNodes;

    private final ExecutorService executorService;

    public DogSearchPedigreeImporter(ExecutorService executorService, GraphDatabaseService graphDb, DogSearchClient dogSearchClient, Dogs dogs) {
        this.executorService = executorService;
        this.graphDb = graphDb;
        this.graphQueryService = new GraphQueryService(graphDb);
        this.dogSearchClient = dogSearchClient;
        this.dogs = dogs;
        this.commonNodes = new CommonNodes(graphDb);
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

    TraversalStatistics importDogPedigree(String id) {
        long startTime = System.currentTimeMillis();
        LOGGER.trace("Importing Pedigree from DogSearch for dog {}", id);
        Set<String> descendants = new LinkedHashSet<>();
        DogDetails dogDetails = dogSearchClient.findDog(id);
        if (dogDetails == null) {
            LOGGER.info("Dog does not exist on DogSearch {}", id);
            return null;
        }
        String uuid = dogDetails.getId();
        if (uuid == null) {
            throw new RuntimeException("uuid for " + id + " is null");
        }
        Node dog = graphQueryService.getDog(uuid);
        if (dog != null) {
            graphQueryService.populateDescendantUuids(dog, descendants);
        }
        TraversalStatistics ts = new TraversalStatistics(uuid);
        DogFuture dogFuture = depthFirstDogImport(ts, descendants, 1, dogDetails);
        dogFuture.waitForPedigreeImportToComplete();
        double duration = (System.currentTimeMillis() - startTime) / 1000;
        LOGGER.trace("Imported Pedigree (dogs={}, minDepth={}, maxDepth={}) for dog {} in {} seconds", ts.dogCount, ts.minDepth, ts.maxDepth, id, new DecimalFormat("0.0").format(duration));
        return ts;
    }


    private Node build(NodeBuilder builder) {
        try (Transaction tx = graphDb.beginTx()) {
            Node node = builder.build(graphDb);
            tx.success();
            return node;
        }
    }

    private Relationship build(RelationshipBuilder builder) {
        try (Transaction tx = graphDb.beginTx()) {
            Relationship relationship = builder.build(graphDb);
            tx.success();
            return relationship;
        }
    }

    public DogFuture depthFirstDogImport(TraversalStatistics ts, Set<String> descendants, int depth, DogDetails dogDetails) {
        String uuid = dogDetails.getId();

        Node dog = graphQueryService.getDogIfItHasAtLeastOneParent(uuid);
        if (dog != null) {
            return new DogFuture(dog, null, null); // parents already being/been traversed
        }

        dog = build(dogs.dog().all(dogDetails));

        ts.dogCount.addAndGet(1);
        if (depth > ts.maxDepth.get()) {
            ts.maxDepth.set(depth); // TODO perform this check-then-add operation in atomically
        }

        DogFuture dogFuture;
        DogAncestry dogAncestry = dogDetails.getAncestry();
        if (dogAncestry != null) {
            dogFuture = addAncestry(dog, ts, descendants, depth, uuid, dogAncestry, uuid);
        } else {
            dogFuture = new DogFuture(dog, null, null);
            LOGGER.trace("DOG is missing ancestry {}", uuid);
        }

        /*
        // add offspring, but queue up import requests on each puppy rather than adding them recursively.
        Future<Node>[] puppyTasks = addOffspring(dog, dogDetails);
        ts.dogCount.addAndGet(puppyTasks.length);
        */

        return dogFuture;
    }

    private DogFuture addAncestry(Node dog, TraversalStatistics ts, Set<String> descendants, int depth, String id, DogAncestry dogAncestry, String uuid) {
        DogLitter litter = dogAncestry.getLitter();
        if (litter != null) {
            String litterId = litter.getId();
            if (litterId != null) {
                Node litterNode = build(dogs.litter().id(litterId));
                build(dogs.inLitter().litter(litterNode).puppy(dog));
            }
        }

        // perform depth first traversal (father side first)

        DogParent father = dogAncestry.getFather();
        Future<DogFuture> fatherFuture = addParent(dog, ts, descendants, depth, uuid, father, ParentRole.FATHER);

        DogParent mother = dogAncestry.getMother();
        Future<DogFuture> motherFuture = addParent(dog, ts, descendants, depth, uuid, mother, ParentRole.MOTHER);

        return new DogFuture(dog, fatherFuture, motherFuture);
    }

    private Future<DogFuture> addParent(final Node dog, final TraversalStatistics ts, final Set<String> descendants, final int depth, final String uuid, final DogParent dogParent, final ParentRole parentRole) {
        if (dogParent == null) {
            LOGGER.trace("DOG is missing {}: {}", parentRole, uuid);
            return null;
        }

        final String parentId = dogParent.getId();

        Callable<DogFuture> parentTask = new Callable<DogFuture>() {
            @Override
            public DogFuture call() {
                DogDetails parentDetails = dogSearchClient.findDog(parentId);

                if (parentDetails == null) {
                    if ((depth - 1) < ts.minDepth.get()) {
                        ts.minDepth.set(depth - 1); // TODO perform this check-then-add operation in atomically
                    }
                    LOGGER.trace("{} not found: {}", parentRole, parentId);
                    return null;
                }

                // Dog found on dogsearch

                descendants.add(uuid);
                try {

                    /*
                     * Recursively add ancestors
                     */

                    DogFuture dogFuture = depthFirstDogImport(ts, descendants, depth + 1, parentDetails);

                    Node parent = dogFuture.getDog();

                    HasParentRelationshipBuilder hasParentBuilder = dogs.hasParent().child(dog).parent(parent).role(parentRole);

                    if (descendants.contains(parentId)) {
                        DOGSEARCH.info("DOG cannot be its own ancestor {}: {}", parentRole, uuid);
                        hasParentBuilder.ownAncestor();
                    }

                    build(hasParentBuilder);

                    return dogFuture;

                } finally {
                    descendants.remove(uuid);
                }
            }
        };

        Future<DogFuture> parentFuture = executorService.submit(parentTask);

        return parentFuture;
    }

    private Future<Node>[] addOffspring(final Node parent, DogDetails dogDetails) {
        ParentRole parentRole = null;
        String gender = dogDetails.getGender();
        if (gender != null) {
            if (gender.equalsIgnoreCase("male")) {
                parentRole = ParentRole.FATHER;
            } else if (gender.equalsIgnoreCase("female")) {
                parentRole = ParentRole.MOTHER;
            }
        }

        List<Future<Node>> puppyFutureList = new ArrayList<>();

        DogOffspring[] offspringArr = dogDetails.getOffspring();
        if (offspringArr != null && offspringArr.length > 0) {
            for (DogOffspring offspring : offspringArr) {
                String litterId = offspring.getId();
                LocalDate litterBorn = LocalDate.parse(offspring.getBorn());
                int count = offspring.getCount();
                Node litter = build(dogs.litter().id(litterId).born(litterBorn).count(count));
                build(dogs.hasLitter().litter(litter).parent(parent).role(parentRole));
                for (DogPuppy dogPuppy : offspring.getPuppies()) {
                    Callable<Node> puppyTask = createPuppyTask(dogDetails, litter, dogPuppy);
                    puppyFutureList.add(executorService.submit(puppyTask));
                }
            }
        }

        Future<Node>[] puppyFutures = puppyFutureList.toArray(new Future[puppyFutureList.size()]);
        return puppyFutures;
    }

    private Callable<Node> createPuppyTask(final DogDetails dogDetails, final Node litter, final DogPuppy dogPuppy) {
        return new Callable<Node>() {
            @Override
            public Node call() throws Exception {
                DogDetails puppyDetails = dogSearchClient.findDog(dogPuppy.getId());
                if (puppyDetails == null) {
                    String uuid = dogDetails.getId();
                    LOGGER.warn("Puppy {} cannot be found on dogsearch. Registered as puppy of dog {}", dogPuppy.getId(), uuid);
                    return null;
                }
                Node puppyNode = build(dogs.dog().all(puppyDetails));
                build(dogs.inLitter().puppy(puppyNode).litter(litter));
                return puppyNode;
            }
        };
    }

}
