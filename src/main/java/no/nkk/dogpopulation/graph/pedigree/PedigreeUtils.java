package no.nkk.dogpopulation.graph.pedigree;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
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
public class PedigreeUtils {

    public static Dog populateDog(Node source, Dog target) {
        String uuid = (String) source.getProperty(DogGraphConstants.DOG_UUID);
        String name = (String) source.getProperty(DogGraphConstants.DOG_NAME);
        Relationship breedRelation = source.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
        Node breedSynonymNode = breedRelation.getEndNode();
        String breedName = (String) breedSynonymNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM);
        Breed breed = new Breed(breedName);
        if (breedSynonymNode.hasRelationship(Direction.OUTGOING, DogGraphRelationshipType.MEMBER_OF)) {
            Relationship breedMemberRelationship = breedSynonymNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            if (breedMemberRelationship != null) {
                Node breedNode = breedMemberRelationship.getEndNode();
                breed.setId((String) breedNode.getProperty(DogGraphConstants.BREED_FCI_BREED_ID));
            }
        }
        target.setUuid(uuid);
        target.setName(name);
        target.setBreed(breed);
        if (source.hasProperty(DogGraphConstants.DOG_REGNO)) {
            String regNo = (String) source.getProperty(DogGraphConstants.DOG_REGNO);
            target.getIds().put(DogGraphConstants.DOG_REGNO, regNo);
        }
        if (source.hasProperty(DogGraphConstants.DOG_CHIPNO)) {
            String chipNo = (String) source.getProperty(DogGraphConstants.DOG_CHIPNO);
            target.getIds().put(DogGraphConstants.DOG_CHIPNO, chipNo);
        }

        if (source.hasProperty(DogGraphConstants.DOG_BORN_YEAR)
                && source.hasProperty(DogGraphConstants.DOG_BORN_MONTH)
                && source.hasProperty(DogGraphConstants.DOG_BORN_DAY)) {
            int year = (Integer) source.getProperty(DogGraphConstants.DOG_BORN_YEAR);
            int month = (Integer) source.getProperty(DogGraphConstants.DOG_BORN_MONTH);
            int day = (Integer) source.getProperty(DogGraphConstants.DOG_BORN_DAY);
            LocalDate bornDate = new LocalDate(year, month, day);
            target.setBorn(DateTimeFormat.forPattern("yyyy-MM-dd").print(bornDate));
        }

        if (source.hasProperty(DogGraphConstants.DOG_HDDIAG)) {
            Health health = new Health();
            String hdDiag = (String) source.getProperty(DogGraphConstants.DOG_HDDIAG);
            health.setHdDiag(hdDiag);
            if (source.hasProperty(DogGraphConstants.DOG_HDYEAR)) {
                int hdYear = (Integer) source.getProperty(DogGraphConstants.DOG_HDYEAR);
                health.setHdYear(hdYear);
            }
            target.setHealth(health);
        }

        return target;
    }


    public static Offspring[] getOffspring(Node dogNode) {
        if (!dogNode.hasRelationship(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
            return null;
        }

        List<Offspring> offspringList = new ArrayList<>();
        for (Relationship hasLitter : dogNode.getRelationships(Direction.OUTGOING, DogGraphRelationshipType.HAS_LITTER)) {
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
                Node puppyNode = inLitter.getStartNode();
                Puppy puppy = new Puppy();
                puppy.setId((String) puppyNode.getProperty(DogGraphConstants.DOG_UUID));
                puppy.setName((String) puppyNode.getProperty(DogGraphConstants.DOG_NAME));
                if (puppyNode.hasProperty(DogGraphConstants.DOG_REGNO)) {
                    puppy.setRegNo((String) puppyNode.getProperty(DogGraphConstants.DOG_REGNO));
                }
                Relationship isBreed = puppyNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
                Node puppyBreedSynonymNode = isBreed.getEndNode();
                Breed puppyBreed = new Breed((String) puppyBreedSynonymNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM));
                if (puppyBreedSynonymNode.hasRelationship(Direction.OUTGOING, DogGraphRelationshipType.MEMBER_OF)) {
                    Relationship breedMemberRelationship = puppyBreedSynonymNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
                    if (breedMemberRelationship != null) {
                        Node breedNode = breedMemberRelationship.getEndNode();
                        puppyBreed.setId((String) breedNode.getProperty(DogGraphConstants.BREED_FCI_BREED_ID));
                    }
                }
                puppy.setBreed(puppyBreed);
                puppyList.add(puppy);
            }
            Puppy[] puppyArr = puppyList.toArray(new Puppy[puppyList.size()]);
            offspring.setPuppies(puppyArr);
            offspringList.add(offspring);
        }
        Offspring[] offspringArr = offspringList.toArray(new Offspring[offspringList.size()]);

        return offspringArr;
    }

}
