package no.nkk.dogpopulation.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogSearchResponseBody {

    int numFound;
    int start;

    DogDocument[] docs;

    public DogSearchResponseBody() {
    }

    public int getNumFound() {
        return numFound;
    }

    public void setNumFound(int numFound) {
        this.numFound = numFound;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public DogDocument[] getDocs() {
        return docs;
    }

    public void setDocs(DogDocument[] docs) {
        this.docs = docs;
    }
}
