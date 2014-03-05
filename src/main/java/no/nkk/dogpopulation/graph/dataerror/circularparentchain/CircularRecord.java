package no.nkk.dogpopulation.graph.dataerror.circularparentchain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"uuid", "born", "parentRole", "parentUuid", "parentBorn"})
public class CircularRecord {
    private String uuid;
    private String born;
    private String parentRole;
    private String parentUuid;
    private String parentBorn;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public String getParentRole() {
        return parentRole;
    }

    public void setParentRole(String parentRole) {
        this.parentRole = parentRole;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getParentBorn() {
        return parentBorn;
    }

    public void setParentBorn(String parentBorn) {
        this.parentBorn = parentBorn;
    }
}
