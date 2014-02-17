package no.nkk.dogpopulation.graph.pedigree;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.ParentRole;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeAlgorithm {


    public TopLevelDog getPedigree(Node node) {
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

}
