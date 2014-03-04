package no.nkk.dogpopulation.graph.dataerror.breed;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"uuid", "breed", "fatherUuid", "fatherBreed", "motherUuid", "motherBreed"})
public class IncorrectBreedRecord {
    private String uuid;
    private String breed;
    private String fatherUuid;
    private String fatherBreed;
    private String motherUuid;
    private String motherBreed;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getFatherUuid() {
        return fatherUuid;
    }

    public void setFatherUuid(String fatherUuid) {
        this.fatherUuid = fatherUuid;
    }

    public String getFatherBreed() {
        return fatherBreed;
    }

    public void setFatherBreed(String fatherBreed) {
        this.fatherBreed = fatherBreed;
    }

    public String getMotherUuid() {
        return motherUuid;
    }

    public void setMotherUuid(String motherUuid) {
        this.motherUuid = motherUuid;
    }

    public String getMotherBreed() {
        return motherBreed;
    }

    public void setMotherBreed(String motherBreed) {
        this.motherBreed = motherBreed;
    }
}
