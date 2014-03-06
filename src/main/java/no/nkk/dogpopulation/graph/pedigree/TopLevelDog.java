package no.nkk.dogpopulation.graph.pedigree;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"uuid", "name", "born", "breed", "gender", "ids", "inbreedingCoefficient3", "inbreedingCoefficient6", "ownAncestor", "health", "offspring", "ancestry"})
public class TopLevelDog extends Dog {
    private Offspring[] offspring;
    private String gender;

    public Offspring[] getOffspring() {
        return offspring;
    }

    public void setOffspring(Offspring[] offspring) {
        this.offspring = offspring;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
