package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import no.nkk.dogpopulation.importer.DogImporter;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchImporter implements DogImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchImporter.class);
    private static final Logger DOGSEARCH = LoggerFactory.getLogger("dogsearch");

    static class TraversalStatistics {
        final String id;
        final AtomicInteger dogCount = new AtomicInteger();
        final AtomicInteger maxDepth = new AtomicInteger();
        final AtomicInteger minDepth = new AtomicInteger(Integer.MAX_VALUE);

        private TraversalStatistics(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "TraversalStatistics{" +
                    "dogCount=" + dogCount.get() +
                    ", maxDepth=" + maxDepth.get() +
                    ", minDepth=" + minDepth.get() +
                    '}';
        }
    }

    private final GraphAdminService graphAdminService;
    private final GraphQueryService graphQueryService;
    private final DogSearchClient dogSearchClient;

    private final ExecutorService executorService;


    public DogSearchImporter(ExecutorService executorService, GraphDatabaseService graphDb, DogSearchClient dogSearchClient) {
        this.executorService = executorService;
        this.graphAdminService = new GraphAdminService(graphDb);
        this.graphQueryService = new GraphQueryService(graphDb);
        this.dogSearchClient = dogSearchClient;
    }


    @Override
    public Future<String> importDog(final String id) {
        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(id);
                try {
                    return importDogPedigree(id).id;
                } catch (RuntimeException e) {
                    LOGGER.error("", e);
                    throw e;
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        });
        return future;
    }

    @Override
    public Future<?> importBreed(final String breed) {
        Future<?> future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(breed);
                try {
                    importBreedPedigree(breed);
                } catch (Throwable e) {
                    LOGGER.error("", e);
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        });
        return future;
    }

    private int importBreedPedigree(String breed) {
        int n = 0;
        LOGGER.info("Looking up all UUIDs on dogsearch for breed {}", breed);
        Set<String> breedIds = dogSearchClient.listIdsForBreed(breed);
        LOGGER.info("Found {} {} dogs on dogsearch, importing pedigrees...", breedIds.size(), breed);
        int i=0;
        for (String id : breedIds) {
            TraversalStatistics ts = importDogPedigree(id);
            LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogCount, id);
            n += ts.dogCount.get();
            i++;
            if (i%100 == 0) {
                LOGGER.debug("Progress: {} of {} -- {}%", i, breedIds.size(), 100 * i / breedIds.size());
            }
        }
        LOGGER.info("Completed pedigree import of {} {} dogs from dogsearch to graph.", n, breed);
        return n;
    }

    private TraversalStatistics importDogPedigree(String id) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Importing Pedigree from DogSearch for dog {}", id);
        Set<String> descendants = new LinkedHashSet<>();
        DogDetails dogDetails = dogSearchClient.findDog(id);
        if (dogDetails == null) {
            LOGGER.info("Dog does not exist on DogSearch {}", id);
            return null;
        }
        String uuid = dogDetails.getId();
        Node dog = graphQueryService.getDog(uuid);
        if (dog != null) {
            graphQueryService.populateDescendantUuids(dog, descendants);
        }
        TraversalStatistics ts = new TraversalStatistics(uuid);
        DogFuture dogFuture = depthFirstDogImport(ts, descendants, 1, dogDetails);
        dogFuture.waitForPedigreeImportToComplete();
        double duration = (System.currentTimeMillis() - startTime) / 1000;
        LOGGER.info("Imported Pedigree (dogs={}, minDepth={}, maxDepth={}) for dog {} in {} seconds", ts.dogCount, ts.minDepth, ts.maxDepth, id, new DecimalFormat("0.0").format(duration));
        return ts;
    }


    public DogFuture depthFirstDogImport(TraversalStatistics ts, Set<String> descendants, int depth, DogDetails dogDetails) {
        String uuid = dogDetails.getId();

        Node dog = graphQueryService.getDogIfItHasAtLeastOneParent(uuid);
        if (dog != null) {
            return new DogFuture(dog, null, null); // parents already being/been traversed
        }

        dog = addDogToGraph(dogDetails);

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

    /**
     * Will add a dog node to the graph. No relationships will be added by this method, only the dog node itself. The
     * added node will also be populated with relevant available properties.
     *
     * @param dogDetails
     * @return
     */
    private Node addDogToGraph(DogDetails dogDetails) {
        String uuid = dogDetails.getId();
        String name = dogDetails.getName();
        if (name == null) {
            name = "";
            DOGSEARCH.warn("UNKNOWN name of dog, using empty name. {}.", uuid);
        }
        DogBreed dogBreed = dogDetails.getBreed();
        String breed;
        String breedId = null;
        if (dogBreed == null) {
            DOGSEARCH.warn("UNKNOWN breed of dog {}.", uuid);
            breed = "UNKNOWN";
        } else {
            breedId = dogBreed.getId();
            if (dogBreed.getName().trim().isEmpty()) {
                DOGSEARCH.warn("UNKNOWN breed of dog {}.", uuid);
                breed = "UNKNOWN";
            } else {
                breed = dogBreed.getName();
            }
        }

        String regNo = null;
        for (DogId dogId : dogDetails.getIds()) {
            if (DogGraphConstants.DOG_REGNO.equalsIgnoreCase(dogId.getType())) {
                regNo = dogId.getValue();
                break;
            }
        }

        LocalDate bornLocalDate = null;
        String born = dogDetails.getBorn();
        if (born != null) {
            bornLocalDate = LocalDate.parse(born);
        }

        String hdDiag = null;
        LocalDate hdXray = null;
        DogHealth health = dogDetails.getHealth();
        if (health != null) {
            DogHealthHD[] hdArr = health.getHd();
            if (hdArr != null && hdArr.length > 0) {
                DogHealthHD hd = hdArr[0]; // TODO choose HD diagnosis more wisely than just picking the first one.
                hdDiag = hd.getDiagnosis();
                hdXray = LocalDate.parse(hd.getXray());
            }
        }

        Node node = graphAdminService.addDog(uuid, regNo, name, breedId, breed, bornLocalDate, hdDiag, hdXray);

        return node;
    }

    private DogFuture addAncestry(Node dog, TraversalStatistics ts, Set<String> descendants, int depth, String id, DogAncestry dogAncestry, String uuid) {
        DogLitter litter = dogAncestry.getLitter();
        if (litter != null) {
            String litterId = litter.getId();
            if (litterId != null) {
                graphAdminService.addPuppyToLitter(dog, litterId);
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

                    if (descendants.contains(parentId)) {
                        DOGSEARCH.info("DOG cannot be its own ancestor {}: {}", parentRole, uuid);
                        graphAdminService.connectChildAsOwnAncestor(dog, uuid, parent, parentId, parentRole);
                        return dogFuture;
                    }

                    graphAdminService.connectChildToParent(dog, uuid, parent, parentId, parentRole);

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
                Node litter = graphAdminService.addLitter(litterId, litterBorn, count);
                graphAdminService.connectDogAsParentOfLitter(parentRole, parent, litter);
                // TODO find puppies on dogsearch in parallel.
                for (DogPuppy dogPuppy : offspring.getPuppies()) {
                    Callable<Node> puppyTask = createPuppyTask(dogDetails, litterId, dogPuppy);
                    puppyFutureList.add(executorService.submit(puppyTask));
                }
            }
        }

        Future<Node>[] puppyFutures = puppyFutureList.toArray(new Future[puppyFutureList.size()]);
        return puppyFutures;
    }

    private Callable<Node> createPuppyTask(final DogDetails dogDetails, final String litterId, final DogPuppy dogPuppy) {
        return new Callable<Node>() {
            @Override
            public Node call() throws Exception {
                DogDetails puppyDetails = dogSearchClient.findDog(dogPuppy.getId());
                if (puppyDetails == null) {
                    String uuid = dogDetails.getId();
                    LOGGER.warn("Puppy {} cannot be found on dogsearch. Registered as puppy of dog {}", dogPuppy.getId(), uuid);
                    return null;
                }
                Node puppyNode = addDogToGraph(puppyDetails);
                graphAdminService.addPuppyToLitter(puppyNode, litterId);
                return null;
            }
        };
    }

}
