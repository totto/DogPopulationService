package no.nkk.dogpopulation.graph.dataerror.gender;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"uuid", "born", "parentRole", "otherParentUuid", "otherParentName", "otherParentRole"})
public class ChildRecord {
    private String uuid;
    private String born;
    private String parentRole;
    private String otherParentUuid;
    private String otherParentName;
    private String otherParentRole;

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

    public String getOtherParentUuid() {
        return otherParentUuid;
    }

    public void setOtherParentUuid(String otherParentUuid) {
        this.otherParentUuid = otherParentUuid;
    }

    public String getOtherParentName() {
        return otherParentName;
    }

    public void setOtherParentName(String otherParentName) {
        this.otherParentName = otherParentName;
    }

    public String getOtherParentRole() {
        return otherParentRole;
    }

    public void setOtherParentRole(String otherParentRole) {
        this.otherParentRole = otherParentRole;
    }

    public void setParentRole(String parentRole) {
        this.parentRole = parentRole;
    }

    public String getParentRole() {
        return parentRole;
    }
}
