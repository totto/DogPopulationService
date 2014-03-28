package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.LocalDateTime;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class FileReadingDogSearchClient implements DogSearchClient {

    private final String folderName;

    @Inject
    public FileReadingDogSearchClient(@Named("pedigree-test-uuid") String folderName) {
        this.folderName = folderName;
    }

    @Override
    public Set<String> listIdsForBreed(String breed, LocalDateTime from, LocalDateTime to) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<DogDetails> findDog(String id) {
        File directory = new File("src/test/resources/dogsearch/" + folderName);
        File file = new File(directory, id + ".json");
        if (!file.isFile()) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return new ImmediateFuture<>(objectMapper.readValue(file, DogDetails.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> listIdsForLastWeek() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> listIdsForLastDay() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> listIdsForLastHour() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> listIdsForLastMinute() {
        throw new UnsupportedOperationException();
    }
}
