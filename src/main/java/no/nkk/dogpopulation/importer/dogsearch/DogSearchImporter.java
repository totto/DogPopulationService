package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import no.nkk.dogpopulation.importer.DogImporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchImporter implements DogImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchImporter.class);
    private static final Logger DOGSEARCH = LoggerFactory.getLogger("dogsearch");

    static class TraversalStatistics {
        int dogCount;
        int maxDepth;
        int minDepth = Integer.MAX_VALUE;

        @Override
        public String toString() {
            return "TraversalStatistics{" +
                    "dogCount=" + dogCount +
                    ", maxDepth=" + maxDepth +
                    ", minDepth=" + minDepth +
                    '}';
        }
    }

    private final GraphAdminService graphAdminService;
    private final GraphQueryService graphQueryService;
    private final DogSearchClient dogSearchClient;

    private final ConcurrentMap<String, String> alreadyImportedIds = new ConcurrentHashMap<>();

    private final ExecutorService executorService;


    public DogSearchImporter(ExecutorService executorService, GraphDatabaseService graphDb, DogSearchClient dogSearchClient) {
        this.executorService = executorService;
        this.graphAdminService = new GraphAdminService(graphDb);
        this.graphQueryService = new GraphQueryService(graphDb);
        this.dogSearchClient = dogSearchClient;
    }


    @Override
    public Future<?> importDog(final String id) {
        Future<?> future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(id);
                try {
                    importDogPedigree(id);
                } catch (Throwable e) {
                    LOGGER.error("", e);
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
            TraversalStatistics ts = new TraversalStatistics();
            Set<String> descendants = new LinkedHashSet<>();
            LOGGER.trace("Importing pedigree for {}", id);
            depthFirstDogImport(ts, descendants, 1, id);
            n += ts.dogCount;
            LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogCount, id);
            i++;
            if (i%100 == 0) {
                LOGGER.debug("Progress: {} of {} -- {}%", i, breedIds.size(), 100 * i / breedIds.size());
            }
        }
        LOGGER.info("Completed pedigree import of {} {} dogs from dogsearch to graph.", n, breed);
        return n;
    }

    private int importDogPedigree(String id) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Importing Pedigree from DogSearch for dog {}", id);
        TraversalStatistics ts = new TraversalStatistics();
        Set<String> descendants = new LinkedHashSet<>();
        depthFirstDogImport(ts, descendants, 1, id);
        double duration = (System.currentTimeMillis() - startTime) / 1000;
        LOGGER.info("Imported Pedigree (dogs={}, minDepth={}, maxDepth={}) for dog {} in {} seconds", ts.dogCount, ts.minDepth, ts.maxDepth, id, new DecimalFormat("0.0").format(duration));
        return ts.dogCount;
    }

    public String depthFirstDogImport(TraversalStatistics ts, Set<String> descendants, int depth, String id) {
        if (alreadyImportedIds.containsKey(id)) {
            return alreadyImportedIds.get(id); // already imported before by us or another thread
        }

        DogDetails dogDetails = dogSearchClient.findDog(id);

        if (dogDetails == null) {
            if ((depth - 1) < ts.minDepth) {
                ts.minDepth = depth - 1;
            }
            return null;
        }

        // Dog found

        String uuid = dogDetails.getId();

        if ((alreadyImportedIds.putIfAbsent(id, uuid) != null)) {
            return alreadyImportedIds.get(id); // another thread imported the same dog at the same time, let other thread import recursively.
        }

        String name = dogDetails.getName();
        if (name == null) {
            name = "";
            DOGSEARCH.warn("UNKNOWN name of dog, using empty name. {}.", uuid);
        }
        DogBreed dogBreed = dogDetails.getBreed();
        String breed;
        if (dogBreed == null || dogBreed.getName().trim().isEmpty()) {
            DOGSEARCH.warn("UNKNOWN breed of dog {}.", uuid);
            breed = "UNKNOWN";
        } else {
            breed = dogBreed.getName();
        }
        String regNo = null;
        for (DogId dogId : dogDetails.getIds()) {
            if (DogGraphConstants.DOG_REGNO.equalsIgnoreCase(dogId.getType())) {
                regNo = dogId.getValue();
                break;
            }
        }
        graphAdminService.addDog(uuid, regNo, name, breed);

        ts.dogCount++;
        if (depth > ts.maxDepth) {
            ts.maxDepth = depth;
        }

        DogAncestry dogAncestry = dogDetails.getAncestry();

        if (dogAncestry == null) {
            LOGGER.trace("DOG is missing ancestry {}", id);
            return dogDetails.getId();
        }

        if (depth == 1) {
            // will not happen in any of the recursions (assuming an ever increasing depth on recursion stack)
            graphQueryService.populateDescendantUuids(uuid, descendants);
        }

        // perform depth first traversal (father side first)

        DogParent father = dogAncestry.getFather();
        traverseParent(ts, descendants, depth, uuid, dogDetails, father, ParentRole.FATHER);

        DogParent mother = dogAncestry.getMother();
        traverseParent(ts, descendants, depth, uuid, dogDetails, mother, ParentRole.MOTHER);

        return uuid;
    }

    private String traverseParent(TraversalStatistics ts, Set<String> descendants, int depth, String uuid, DogDetails dogDetails, DogParent parent, ParentRole parentRole) {
        if (parent == null) {
            LOGGER.trace("DOG is missing {}: {}", parentRole, uuid);
            return null;
        }

        descendants.add(uuid);
        try {

            String parentId = depthFirstDogImport(ts, descendants, depth + 1, parent.getId());
            if (parentId == null) {
                LOGGER.trace("{} not found: {}", parentRole, parent.getId());
                return null;
            }

            if (descendants.contains(parentId)) {
                DOGSEARCH.info("DOG cannot be its own ancestor {}: {}", parentRole, uuid);
                graphAdminService.connectChildAsOwnAncestor(dogDetails.getId(), parentId, parentRole);
                return parentId;
            }

            graphAdminService.connectChildToParent(dogDetails.getId(), parentId, parentRole);

            return parentId;

        } finally {
            descendants.remove(uuid);
        }
    }

}
