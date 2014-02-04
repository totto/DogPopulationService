package no.nkk.dogpopulation.pedigree;

import no.nkk.dogpopulation.dogsearch.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Dog {

    private String name;
    private Breed breed;
    private Map<String, String> ids = new LinkedHashMap<>();
    private int inbreedingCoefficient;
    private Ancestry ancestry;

    public Dog() {
    }

    public Dog(DogDetails dogDetails) {
        for (DogId dogId : dogDetails.getIds()) {
            this.ids.put(dogId.getType(), dogId.getValue());
        }
        this.name = dogDetails.getName();
        DogBreed dogBreed = dogDetails.getBreed();
        this.breed = new Breed(dogBreed.getName());
        this.breed.setId(dogBreed.getId());
        DogAncestry dogAncestry = dogDetails.getAncestry();
        if (dogAncestry != null) {
            this.ancestry = new Ancestry();
            DogParent dogFather = dogAncestry.getFather();
            if (dogFather != null) {
                Dog father = buildParent(dogFather);
                this.ancestry.setFather(father);
            }
            DogParent dogMother = dogAncestry.getMother();
            if (dogMother != null) {
                Dog mother = buildParent(dogMother);
                this.ancestry.setMother(mother);
            }
        }
    }

    private Dog buildParent(DogParent dogParent) {
        Dog parent = new Dog();
        parent.setName(dogParent.getName());
        parent.getIds().put("id", dogParent.getId());
        Breed parentBreed = new Breed(dogParent.getBreed().getName());
        parentBreed.setId(dogParent.getBreed().getId());
        parent.setBreed(parentBreed);
        return parent;
    }

    public Dog(String name, Breed breed) {
        this.name = name;
        this.breed = breed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Breed getBreed() {
        return breed;
    }

    public void setBreed(Breed breed) {
        this.breed = breed;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public void setIds(Map<String, String> ids) {
        this.ids = ids;
    }

    public int getInbreedingCoefficient() {
        return inbreedingCoefficient;
    }

    public void setInbreedingCoefficient(int inbreedingCoefficient) {
        this.inbreedingCoefficient = inbreedingCoefficient;
    }

    public Ancestry getAncestry() {
        return ancestry;
    }

    public void setAncestry(Ancestry ancestry) {
        this.ancestry = ancestry;
    }
}
