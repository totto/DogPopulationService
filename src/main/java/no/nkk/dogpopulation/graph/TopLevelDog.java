package no.nkk.dogpopulation.graph;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class TopLevelDog {
    private String uuid;
    private String name;
    private String born;
    private Breed breed;
    private Map<String, String> ids = new LinkedHashMap<>();
    private Health health;
    private int inbreedingCoefficient3;
    private int inbreedingCoefficient6;
    private Ancestry ancestry;
    private Offspring[] offspring;

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

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public Breed getBreed() {
        return breed;
    }

    public void setBreed(Breed breed) {
        this.breed = breed;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public void setIds(Map<String, String> ids) {
        this.ids = ids;
    }

    public Health getHealth() {
        return health;
    }

    public void setHealth(Health health) {
        this.health = health;
    }

    public int getInbreedingCoefficient3() {
        return inbreedingCoefficient3;
    }

    public void setInbreedingCoefficient3(int inbreedingCoefficient3) {
        this.inbreedingCoefficient3 = inbreedingCoefficient3;
    }

    public int getInbreedingCoefficient6() {
        return inbreedingCoefficient6;
    }

    public void setInbreedingCoefficient6(int inbreedingCoefficient6) {
        this.inbreedingCoefficient6 = inbreedingCoefficient6;
    }

    public Ancestry getAncestry() {
        return ancestry;
    }

    public void setAncestry(Ancestry ancestry) {
        this.ancestry = ancestry;
    }

    public Offspring[] getOffspring() {
        return offspring;
    }

    public void setOffspring(Offspring[] offspring) {
        this.offspring = offspring;
    }
}
