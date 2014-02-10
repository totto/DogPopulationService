package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogDetails {
    private String id;
    private String name;
    private DogBreed breed;
    private String born;
    private String postalCode;
    private String gender;
    private String color;
    private String process;
    private DogAncestry ancestry;
    private DogId[] ids;
    private int quality;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DogBreed getBreed() {
        return breed;
    }

    public void setBreed(DogBreed breed) {
        this.breed = breed;
    }

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public DogAncestry getAncestry() {
        return ancestry;
    }

    public void setAncestry(DogAncestry ancestry) {
        this.ancestry = ancestry;
    }

    public DogId[] getIds() {
        return ids;
    }

    public void setIds(DogId[] ids) {
        this.ids = ids;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
}
