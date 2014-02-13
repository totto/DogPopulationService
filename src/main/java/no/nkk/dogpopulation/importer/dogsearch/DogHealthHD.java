package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogHealthHD {
    private String id;              // Unique HD id
    private String veterinary;
    private String xray;            // Date of X-Ray. YYYY-MM-DD
    private String readDate;        // YYYY-MM-DD
    private String sentToOwnerDate; // YYYY-MM-DD
    private String diagnosis;       // e.g. A1
    private String sekLeft;
    private String sekRight;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVeterinary() {
        return veterinary;
    }

    public void setVeterinary(String veterinary) {
        this.veterinary = veterinary;
    }

    public String getXray() {
        return xray;
    }

    public void setXray(String xray) {
        this.xray = xray;
    }

    public String getReadDate() {
        return readDate;
    }

    public void setReadDate(String readDate) {
        this.readDate = readDate;
    }

    public String getSentToOwnerDate() {
        return sentToOwnerDate;
    }

    public void setSentToOwnerDate(String sentToOwnerDate) {
        this.sentToOwnerDate = sentToOwnerDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getSekLeft() {
        return sekLeft;
    }

    public void setSekLeft(String sekLeft) {
        this.sekLeft = sekLeft;
    }

    public String getSekRight() {
        return sekRight;
    }

    public void setSekRight(String sekRight) {
        this.sekRight = sekRight;
    }
}
