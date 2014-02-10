package no.nkk.dogpopulation.importer;

import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import no.nkk.dogpopulation.importer.dogsearch.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogImporter.class);
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
    private final Set<String> breeds;
    private final Set<String> ids;

    public DogImporter(GraphDatabaseService graphDb, DogSearchClient dogSearchClient, Set<String> breeds, Set<String> ids) {
        this.graphAdminService = new GraphAdminService(graphDb);
        this.graphQueryService = new GraphQueryService(graphDb);
        this.dogSearchClient = dogSearchClient;
        this.breeds = breeds;
        this.ids = ids;
    }

    public void runDogImport() {
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        for (final String id : ids) {
            executorService.submit(new Runnable() {
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
        }
        for (final String breed : breeds) {
            executorService.submit(new Runnable() {
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
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.HOURS);
        } catch (Exception e) {
            LOGGER.error("", e);
        }

    }

    private int importBreedPedigree(String breed) {
        Map<String, String> alreadySearchedIds = new LinkedHashMap<>();
        int n = 0;
        LOGGER.info("Looking up all UUIDs on dogsearch for breed {}", breed);
        Set<String> breedIds = dogSearchClient.listIdsForBreed(breed);
        LOGGER.info("Found {} {} dogs on dogsearch, importing pedigrees...", breedIds.size(), breed);
        int i=0;
        for (String id : breedIds) {
            TraversalStatistics ts = new TraversalStatistics();
            Set<String> descendants = new LinkedHashSet<>();
            LOGGER.trace("Importing pedigree for {}", id);
            depthFirstDogImport(ts, alreadySearchedIds, descendants, 1, id);
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
        LOGGER.info("Importing Pedigree from DogSearch for DOG {}", id);
        TraversalStatistics ts = new TraversalStatistics();
        Set<String> descendants = new LinkedHashSet<>();
        Map<String, String> alreadySearchedIds = new LinkedHashMap<>();
        depthFirstDogImport(ts, alreadySearchedIds, descendants, 1, id);
        LOGGER.trace(ts.toString());
        return ts.dogCount;
    }

    public String depthFirstDogImport(TraversalStatistics ts, Map<String, String> alreadySearchedIds, Set<String> descendants, int depth, String id) {
        if (alreadySearchedIds.containsKey(id)) {
            return alreadySearchedIds.get(id);
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
        Node dogNode = graphAdminService.addDog(uuid, name, breed);

        alreadySearchedIds.put(id, uuid);
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
            graphQueryService.populateDescendantUuids(dogNode, descendants);
        }

        // perform depth first traversal (father side first)

        DogParent father = dogAncestry.getFather();
        traverseParent(ts, alreadySearchedIds, descendants, depth, uuid, dogDetails, father, ParentRole.FATHER);

        DogParent mother = dogAncestry.getMother();
        traverseParent(ts, alreadySearchedIds, descendants, depth, uuid, dogDetails, mother, ParentRole.MOTHER);

        return uuid;
    }

    private String traverseParent(TraversalStatistics ts, Map<String, String> alreadySearchedIds, Set<String> descendants, int depth, String uuid, DogDetails dogDetails, DogParent parent, ParentRole parentRole) {
        if (parent == null) {
            LOGGER.trace("DOG is missing {}: {}", parentRole, uuid);
            return null;
        }

        descendants.add(uuid);
        try {

            String parentId = depthFirstDogImport(ts, alreadySearchedIds, descendants, depth + 1, parent.getId());
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
