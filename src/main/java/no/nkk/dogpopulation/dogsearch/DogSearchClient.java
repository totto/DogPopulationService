package no.nkk.dogpopulation.dogsearch;


import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface DogSearchClient {

    Set<String> listIdsForBreed(String breed);
    DogDetails findDog(String id);
}
