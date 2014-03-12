package no.nkk.dogpopulation.breedgroupimport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BreedGroupFile {
    private BreedDefinition[] breed;

    public BreedDefinition[] getBreed() {
        return breed;
    }

    public void setBreed(BreedDefinition[] breed) {
        this.breed = breed;
    }
}
