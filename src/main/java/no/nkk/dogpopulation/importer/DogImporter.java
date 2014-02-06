package no.nkk.dogpopulation.importer;

import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.dogsearch.*;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.ParentRole;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
        String breed = dogDetails.getBreed().getName();
        graphAdminService.addDog(uuid, name, breed);

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
            graphQueryService.populateDescendantUuids(uuid, descendants);
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

    public static void main(String... args) {
        GraphDatabaseService graphDb = Main.createGraphDb("data/dogdb");

        DogImporter dogImporter = new DogImporter(graphDb);

        dogImporter.importDogPedigree("NO/24463/05");
    }
}
