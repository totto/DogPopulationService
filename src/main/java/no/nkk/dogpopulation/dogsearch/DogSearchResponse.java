package no.nkk.dogpopulation.dogsearch;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogSearchResponse {

    DogSearchResponseHeader responseHeader;
    DogSearchResponseBody response;

    public DogSearchResponseHeader getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(DogSearchResponseHeader responseHeader) {
        this.responseHeader = responseHeader;
    }

    public DogSearchResponseBody getResponse() {
        return response;
    }

    public void setResponse(DogSearchResponseBody response) {
        this.response = response;
    }
}
