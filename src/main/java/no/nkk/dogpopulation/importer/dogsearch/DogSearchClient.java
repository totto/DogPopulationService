package no.nkk.dogpopulation.importer.dogsearch;


import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface DogSearchClient {

    Future<Set<String>> listIdsForBreed(String breed);
    Future<DogDetails> findDog(String id);

    Set<String> listIdsForLastWeek();

    Set<String> listIdsForLastDay();

    Set<String> listIdsForLastHour();

    Set<String> listIdsForLastMinute();
}
