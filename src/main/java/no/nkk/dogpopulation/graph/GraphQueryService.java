package no.nkk.dogpopulation.graph;

import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompleteness;
import no.nkk.dogpopulation.graph.pedigreecompleteness.PedigreeCompletenessAlgorithm;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * All public methods must wrap access to the graph-database within a transaction. Private methods may assume that
 * they are called within the context of an already open transaction.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class GraphQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQueryService.class);


    private final GraphDatabaseService graphDb;


    public GraphQueryService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }


    public Node getDog(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
            tx.success();
            return dog;
        }
    }


    public List<String> getBreeds() {
        try (Transaction tx = graphDb.beginTx()) {
            Node breedRoot = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            if (breedRoot == null) {
                return Collections.emptyList();
            }
            List<String> breeds = new ArrayList<>(1000);
            for (Path path : graphDb.traversalDescription()
                    .depthFirst()
                    .relationships(DogGraphRelationshipType.MEMBER_OF, Direction.INCOMING)
                    .evaluator(Evaluators.includingDepths(1, 1))
                    .traverse(breedRoot)) {
                Node dogOfBreed = path.endNode();
                breeds.add((String) dogOfBreed.getProperty(DogGraphConstants.BREED_BREED));
            }
            tx.success();
            return breeds;
        }
    }


    public List<String> getBreedList(String breed) {
        try (Transaction tx = graphDb.beginTx()) {
            Node breedRoot = getBreedNode(breed);
            if (breedRoot == null) {
                return Collections.emptyList();
            }
            List<String> dogIds = new ArrayList<>(10000);
            for (Path path : graphDb.traversalDescription()
                    .depthFirst()
                    .relationships(DogGraphRelationshipType.IS_BREED, Direction.INCOMING)
                    .evaluator(Evaluators.includingDepths(1, 1))
                    .traverse(breedRoot)) {
                Node dogOfBreed = path.endNode();
                dogIds.add((String) dogOfBreed.getProperty(DogGraphConstants.DOG_UUID));
            }
            tx.success();
            return dogIds;
        }
    }


    public Node getDogIfItHasAtLeastOneParent(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(uuid);
            if (dog == null) {
                return null;
            }
            Iterable<Relationship> parentRelationships = dog.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
            boolean atLeastOneConnectedParent = parentRelationships.iterator().hasNext();
            tx.success();
            if (atLeastOneConnectedParent) {
                return dog;
            }
            return null; // dog exists but without parents
        }
    }


    public TopLevelDog getPedigree(String uuid) {
        try (Transaction tx = graphDb.beginTx()) {
            Node node = getDogNode(uuid);
            if (node == null) {
                return null; // dog not found
            }
            TopLevelDog dog = getPedigree(node);
            double coi3 = computeCoefficientOfInbreeding(uuid, 3);
            double coi6 = computeCoefficientOfInbreeding(uuid, 6);
            dog.setInbreedingCoefficient3((int) Math.round(10000 * coi3));
            dog.setInbreedingCoefficient6((int) Math.round(10000 * coi6));
            tx.success();
            return dog;
        }
    }


    public void populateDescendantUuids(Node dog, Set<String> descendants) {
        try (Transaction tx = graphDb.beginTx()) {
            populateDescendantIds(dog, descendants);
            tx.success();
        }
    }


    /**
     * Compute the "Coefficient Of Inbreeding" using the method by geneticist Sewall Wright. The computation is done
     * within a single Neo4j transaction.
     *
     * @param uuid the uuid of the dog for which we want the inbreeding coefficient of.
     * @param generations how many generations to use from the pedigree.
     * @return the Coefficient Of Inbreeding.
     */
    public double computeCoefficientOfInbreeding(String uuid, int generations) {
        try (Transaction tx = graphDb.beginTx()) {
            Node dog = getDogNode(uuid);
            double coi = new InbreedingAlgorithm(graphDb, generations).computeSewallWrightCoefficientOfInbreeding(dog);
            tx.success();
            return coi;
        }
    }


    public PedigreeCompleteness getPedigreeCompletenessOfGroup(int generations, Set<String> breedSet, int minYear, int maxYear) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        try (Transaction tx = graphDb.beginTx()) {
            Node categoryBreedNode = getSingleNode(DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREED);
            PedigreeCompletenessAlgorithm algorithm = new PedigreeCompletenessAlgorithm(graphDb, generations);
            PedigreeCompleteness pedigreeCompletenessOfGroup = algorithm.getPedigreeCompletenessOfGroup(categoryBreedNode, breedSet, minYear, maxYear);
            tx.success();
            return pedigreeCompletenessOfGroup;
        }
    }


    private void populateDescendantIds(Node dog, Collection<? super String> descendants) {
        for (Path position : graphDb.traversalDescription()
                .depthFirst()
                .uniqueness(Uniqueness.NODE_PATH)
                .relationships(DogGraphRelationshipType.HAS_PARENT, Direction.INCOMING)
                .evaluator(Evaluators.excludeStartPosition())
                .traverse(dog)) {
            Node descendant = position.endNode();
            String uuid = (String) descendant.getProperty(DogGraphConstants.DOG_UUID);
            if (descendants.contains(uuid)) {
                return; // more than one path to descendant, this is because of inbreeding
            }
            descendants.add(uuid);
        }
    }

    private TopLevelDog getPedigree(Node node) {
        return getPedigreeOfTopLevel(node);
    }

    private TopLevelDog getPedigreeOfTopLevel(Node node) {
        String uuid = (String) node.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) node.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        if (breedNode.hasProperty(DogGraphConstants.BREED_ID)) {
            breed.setId((String) breedNode.getProperty(DogGraphConstants.BREED_ID));
        }
        TopLevelDog dog = new TopLevelDog();
        dog.setName(name);
        dog.setBreed(breed);
        dog.setUuid(uuid);
        if (node.hasProperty(DogGraphConstants.DOG_REGNO)) {
            String regNo = (String) node.getProperty(DogGraphConstants.DOG_REGNO);
            dog.getIds().put(DogGraphConstants.DOG_REGNO, regNo);
        }

        if (node.hasProperty(DogGraphConstants.DOG_BORN_YEAR)
                && node.hasProperty(DogGraphConstants.DOG_BORN_MONTH)
                && node.hasProperty(DogGraphConstants.DOG_BORN_DAY)) {
            int year = (Integer) node.getProperty(DogGraphConstants.DOG_BORN_YEAR);
            int month = (Integer) node.getProperty(DogGraphConstants.DOG_BORN_MONTH);
            int day = (Integer) node.getProperty(DogGraphConstants.DOG_BORN_DAY);
            LocalDate bornDate = new LocalDate(year, month, day);
            dog.setBorn(DateTimeFormat.forPattern("yyyy-MM-dd").print(bornDate));
        }

        if (node.hasProperty(DogGraphConstants.DOG_HDDIAG)) {
            Health health = new Health();
            String hdDiag = (String) node.getProperty(DogGraphConstants.DOG_HDDIAG);
            health.setHdDiag(hdDiag);
            if (node.hasProperty(DogGraphConstants.DOG_HDYEAR)) {
                int hdYear = (Integer) node.getProperty(DogGraphConstants.DOG_HDYEAR);
                health.setHdYear(hdYear);
            }
            dog.setHealth(health);
        }

        Ancestry ancestry = getAncestry(node);
        if (ancestry != null) {
            dog.setAncestry(ancestry);
        }

        if (node.hasRelationship(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
            List<Offspring> offspringList = new ArrayList<>();
            for (Relationship hasLitter : node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
                Offspring offspring = new Offspring();
                Node litterNode = hasLitter.getEndNode();
                String litterId = (String) litterNode.getProperty(DogGraphConstants.LITTER_ID);
                offspring.setId(litterId);
                if (litterNode.hasProperty(DogGraphConstants.LITTER_COUNT)) {
                    int count = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_COUNT);
                    offspring.setCount(count);
                }
                if (litterNode.hasProperty(DogGraphConstants.LITTER_YEAR)
                        && litterNode.hasProperty(DogGraphConstants.LITTER_MONTH)
                        && litterNode.hasProperty(DogGraphConstants.LITTER_DAY)) {
                    int year = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_YEAR);
                    int month = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_MONTH);
                    int day = (Integer) litterNode.getProperty(DogGraphConstants.LITTER_DAY);
                    LocalDate bornDate = new LocalDate(year, month, day);
                    offspring.setBorn(DateTimeFormat.forPattern("yyyy-MM-dd").print(bornDate));
                }
                List<Puppy> puppyList = new ArrayList<>();
                for (Relationship inLitter : litterNode.getRelationships(Direction.INCOMING, DogGraphRelationshipType.IN_LITTER)) {
                    Node puppyNode = inLitter.getEndNode();
                    Puppy puppy = new Puppy();
                    puppy.setId((String) puppyNode.getProperty(DogGraphConstants.DOG_UUID));
                    puppy.setName((String) puppyNode.getProperty(DogGraphConstants.DOG_NAME));
                    if (puppyNode.hasProperty(DogGraphConstants.DOG_REGNO)) {
                        puppy.setRegNo((String) puppyNode.getProperty(DogGraphConstants.DOG_REGNO));
                    }
                    Relationship isBreed = puppyNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
                    Node puppyBreedNode = isBreed.getEndNode();
                    Breed puppyBreed = new Breed((String) puppyBreedNode.getProperty(DogGraphConstants.BREED_BREED));
                    if (puppyBreedNode.hasProperty(DogGraphConstants.BREED_ID)) {
                        puppyBreed.setId((String) puppyBreedNode.getProperty(DogGraphConstants.BREED_ID));
                    }
                    puppy.setBreed(puppyBreed);
                    puppyList.add(puppy);
                }
                Puppy[] puppyArr = puppyList.toArray(new Puppy[puppyList.size()]);
                offspring.setPuppies(puppyArr);
                offspringList.add(offspring);
            }
            Offspring[] offspringArr = offspringList.toArray(new Offspring[offspringList.size()]);
            dog.setOffspring(offspringArr);
        }

        return dog;
    }

    private Dog getPedigreeOfDog(Node node) {
        String uuid = (String) node.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) node.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = node.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        if (breedNode.hasProperty(DogGraphConstants.BREED_ID)) {
            breed.setId((String) breedNode.getProperty(DogGraphConstants.BREED_ID));
        }
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);
        if (node.hasProperty(DogGraphConstants.DOG_REGNO)) {
            String regNo = (String) node.getProperty(DogGraphConstants.DOG_REGNO);
            dog.getIds().put(DogGraphConstants.DOG_REGNO, regNo);
        }

        if (node.hasProperty(DogGraphConstants.DOG_BORN_YEAR)
                && node.hasProperty(DogGraphConstants.DOG_BORN_MONTH)
                && node.hasProperty(DogGraphConstants.DOG_BORN_DAY)) {
            int year = (Integer) node.getProperty(DogGraphConstants.DOG_BORN_YEAR);
            int month = (Integer) node.getProperty(DogGraphConstants.DOG_BORN_MONTH);
            int day = (Integer) node.getProperty(DogGraphConstants.DOG_BORN_DAY);
            LocalDate bornDate = new LocalDate(year, month, day);
            dog.setBorn(DateTimeFormat.forPattern("yyyy-MM-dd").print(bornDate));
        }

        if (node.hasProperty(DogGraphConstants.DOG_HDDIAG)) {
            Health health = new Health();
            String hdDiag = (String) node.getProperty(DogGraphConstants.DOG_HDDIAG);
            health.setHdDiag(hdDiag);
            if (node.hasProperty(DogGraphConstants.DOG_HDYEAR)) {
                int hdYear = (Integer) node.getProperty(DogGraphConstants.DOG_HDYEAR);
                health.setHdYear(hdYear);
            }
            dog.setHealth(health);
        }

        Ancestry ancestry = getAncestry(node);
        if (ancestry != null) {
            dog.setAncestry(ancestry);
        }

        return dog;
    }

    private Ancestry getAncestry(Node node) {
        Dog fatherDog = null;
        Dog motherDog = null;

        Iterable<Relationship> parentRelationIterable = node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_PARENT);
        for (Relationship parentRelation : parentRelationIterable) {
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            switch(parentRole) {
                case FATHER:
                    fatherDog = getParentDog(parentRelation.getEndNode());
                    break;
                case MOTHER:
                    motherDog = getParentDog(parentRelation.getEndNode());
                    break;
            }
        }

        Iterable<Relationship> invalidParentRelationIterable = node.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.OWN_ANCESTOR);
        for (Relationship parentRelation : invalidParentRelationIterable) {
            ParentRole parentRole = ParentRole.valueOf(((String) parentRelation.getProperty(DogGraphConstants.HASPARENT_ROLE)).toUpperCase());
            switch(parentRole) {
                case FATHER:
                    fatherDog = getInvalidAncestorDog(parentRelation.getEndNode());
                    break;
                case MOTHER:
                    motherDog = getInvalidAncestorDog(parentRelation.getEndNode());
                    break;
            }
        }

        if (fatherDog == null && motherDog == null) {
            return null; // no known parents, do not create ancestry
        }

        // at least one known parent in graph

        Ancestry ancestry = new Ancestry(fatherDog, motherDog);
        return ancestry;
    }

    private Dog getParentDog(Node parent) {
        if (parent == null) {
            return null;
        }
        return getPedigreeOfDog(parent);
    }

    private Dog getInvalidAncestorDog(Node parent) {
        if (parent == null) {
            return null;
        }

        String uuid = (String) parent.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) parent.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = parent.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedNode = breedRelation.getEndNode();
        String breedName = (String) breedNode.getProperty(DogGraphConstants.BREED_BREED);
        Breed breed = new Breed(breedName);
        Dog dog = new Dog(name, breed);
        dog.setUuid(uuid);

        dog.setOwnAncestor(true); // makes an invalid ancestor

        return dog;
    }

    private Node getBreedNode(String breed) {
        return getSingleNode(DogGraphLabel.BREED, DogGraphConstants.BREED_BREED, breed);
    }

    Node getSingleNode(DogGraphLabel label, String property, String value) {
        ResourceIterable<Node> breedNodeIterator = graphDb.findNodesByLabelAndProperty(label, property, value);
        try (ResourceIterator<Node> iterator = breedNodeIterator.iterator()) {
            if (!iterator.hasNext()) {
                return null; // node not found
            }
            Node firstMatch = iterator.next();
            if (!iterator.hasNext()) {
                return firstMatch; // only match
            }
            // more than one node match
            LOGGER.warn("More than one node match: label={}, property={}, value={}", label.name(), property, value);
            return firstMatch; // we could throw an exception here
        }
    }

    Node getDogNode(String uuid) {
        return getSingleNode(DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);
    }
}
