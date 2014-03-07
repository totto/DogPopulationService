package no.nkk.dogpopulation.graph.hdindex;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class UnknownBreedCodeException extends RuntimeException {
    public UnknownBreedCodeException(String breedName) {
        super(breedName);
    }
}
