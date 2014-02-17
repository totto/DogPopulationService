package no.nkk.dogpopulation.graph.pedigree;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Ancestry {
    private Dog father;
    private Dog mother;

    public Ancestry() {
    }

    public Ancestry(Dog father, Dog mother) {
        this.father = father;
        this.mother = mother;
    }

    public Dog getFather() {
        return father;
    }

    public void setFather(Dog father) {
        this.father = father;
    }

    public Dog getMother() {
        return mother;
    }

    public void setMother(Dog mother) {
        this.mother = mother;
    }
}
