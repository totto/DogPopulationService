package no.nkk.dogpopulation.dogsearch;

import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchResponse {
    Map<String, Object> responseHeader;

    String name;
    String breed;
    String id;

    public DogSearchResponse(String name, String breed, String id) {
        this.name = name;
        this.breed = breed;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
