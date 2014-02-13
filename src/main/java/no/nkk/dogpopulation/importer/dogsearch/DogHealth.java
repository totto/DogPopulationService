package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogHealth {
    private DogHealthHD[] hd;

    public DogHealthHD[] getHd() {
        return hd;
    }

    public void setHd(DogHealthHD[] hd) {
        this.hd = hd;
    }
}
