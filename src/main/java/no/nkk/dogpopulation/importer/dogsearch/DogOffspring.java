package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogOffspring {
    private String id;
    private String born;
    private Integer count;
    private DogPuppy[] puppies;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public DogPuppy[] getPuppies() {
        return puppies;
    }

    public void setPuppies(DogPuppy[] puppies) {
        this.puppies = puppies;
    }
}
