package no.nkk.dogpopulation.graph.dataerror.gender;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"uuid", "breed", "name", "born", "gender", "litter"})
public class IncorrectGenderRecord {
    private String uuid;
    private String name;
    private String gender;
    private String born;
    private List<LitterRecord> litters;
    private List<ChildRecord> children;
    private String breed;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public List<LitterRecord> getLitters() {
        return litters;
    }

    public void setLitters(List<LitterRecord> litters) {
        this.litters = litters;
    }

    public List<ChildRecord> getChildren() {
        return children;
    }

    public void setChildren(List<ChildRecord> children) {
        this.children = children;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getBreed() {
        return breed;
    }
}
