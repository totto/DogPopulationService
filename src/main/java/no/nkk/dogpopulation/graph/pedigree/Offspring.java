package no.nkk.dogpopulation.graph.pedigree;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class Offspring {
    private String born;
    private int count;
    private String id;
    private Puppy[] puppies;

    public String getBorn() {
        return born;
    }

    public void setBorn(String born) {
        this.born = born;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Puppy[] getPuppies() {
        return puppies;
    }

    public void setPuppies(Puppy[] puppies) {
        this.puppies = puppies;
    }
}
