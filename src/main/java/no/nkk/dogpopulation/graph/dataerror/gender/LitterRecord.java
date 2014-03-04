package no.nkk.dogpopulation.graph.dataerror.gender;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"id", "born", "role"})
public class LitterRecord {
    private String id;
    private String born;
    private String role;
    private int count;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
