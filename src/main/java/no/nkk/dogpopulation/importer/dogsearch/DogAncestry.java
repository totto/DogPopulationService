package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogAncestry {
    private DogParent father;
    private DogParent mother;
    private DogLitter litter;

    public DogParent getFather() {
        return father;
    }

    public void setFather(DogParent father) {
        this.father = father;
    }

    public DogParent getMother() {
        return mother;
    }

    public void setMother(DogParent mother) {
        this.mother = mother;
    }

    public DogLitter getLitter() {
        return litter;
    }

    public void setLitter(DogLitter litter) {
        this.litter = litter;
    }
}
