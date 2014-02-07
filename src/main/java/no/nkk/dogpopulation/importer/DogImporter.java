package no.nkk.dogpopulation.importer;

import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.dogsearch.*;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
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

    public DogImporter(GraphDatabaseService graphDb) {
        this.graphAdminService = new GraphAdminService(graphDb);
        this.graphQueryService = new GraphQueryService(graphDb);

        dogSearchClient = new DogSearchSolrClient("http://dogsearch.nkk.no/dogservice/dogs");
    }

    public int importBreedPedigree(String breed) {
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
                LOGGER.debug("Progress of {} is {} of {} -- {}%", breed, i, breedIds.size(), 100 * i / breedIds.size());
            }
        }
        LOGGER.info("Completed pedigree import of {} {} dogs from dogsearch to graph.", n, breed);
        return n;
    }

    public int importDogPedigree(String id) {
        LOGGER.trace("Importing Pedigree from DogSearch for DOG {}", id);
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
            if (depth < ts.minDepth) {
                ts.minDepth = depth;
            }
            LOGGER.trace("Unable to find DOG {}", id);
            return null;
        }

        // Dog found

        String uuid = dogDetails.getId();
        String name = dogDetails.getName();
        if (name == null) {
            name = "";
            LOGGER.warn("UNKNOWN name of dog, using empty name. {}.", uuid);
        }
        DogBreed dogBreed = dogDetails.getBreed();
        String breed;
        if (dogBreed == null) {
            breed = "UNKNOWN";
            LOGGER.warn("UNKNOWN breed of dog {}.", uuid);
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
                LOGGER.info("DOG cannot be its own ancestor {}: {}", parentRole, uuid);
                graphAdminService.connectChildAsOwnAncestor(dogDetails.getId(), parentId, parentRole);
                return parentId;
            }

            graphAdminService.connectChildToParent(dogDetails.getId(), parentId, parentRole);

            return parentId;

        } finally {
            descendants.remove(uuid);
        }
    }

    public static void main(String... args) throws InterruptedException {
        final GraphDatabaseService graphDb = Main.createGraphDb("data/dogdb");

        // DogImporter dogImporter = new DogImporter(graphDb);
        // dogImporter.importDogPedigree("NO/24463/05");

        final Set<String> breeds = new LinkedHashSet<>();
        breeds.add("Rottweiler");
        breeds.add("Chow Chow");
        breeds.add("Engelsk Setter");
        breeds.add("Golden Retriever");
        breeds.add("Dobermann");
        breeds.add("Pointer");
        breeds.add("Boxer");
        breeds.add("Dalmatiner");
        breeds.add("Berner Sennenhund");
        breeds.add("Leonberger");


        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (final String breed : breeds) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        DogImporter dogImporter = new DogImporter(graphDb);
                        dogImporter.importBreedPedigree(breed);
                    } catch (Throwable e) {
                        LOGGER.error("", e);
                    }
                }
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.HOURS);
        } catch (Exception e) {
            LOGGER.error("", e);
            graphDb.shutdown();
            System.exit(1);
        }

        graphDb.shutdown();
        System.exit(0);
    }
}
