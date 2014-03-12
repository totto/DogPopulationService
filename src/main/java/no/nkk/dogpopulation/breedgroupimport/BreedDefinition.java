package no.nkk.dogpopulation.breedgroupimport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BreedDefinition {
    private String name;
    private String[] thesaurus;
    private BreedId[] ids;
    private String group;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getThesaurus() {
        return thesaurus;
    }

    public void setThesaurus(String[] thesaurus) {
        this.thesaurus = thesaurus;
    }

    public BreedId[] getIds() {
        return ids;
    }

    public void setIds(BreedId[] ids) {
        this.ids = ids;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
