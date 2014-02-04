package no.nkk.dogpopulation.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogAncestry {
    private DogParent father;
    private DogParent mother;

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
}
