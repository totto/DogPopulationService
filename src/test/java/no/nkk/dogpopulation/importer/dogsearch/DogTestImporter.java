package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.importer.DogImporter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogTestImporter implements DogImporter, Runnable {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public Future<?> importDog(String id) {
        return executorService.submit(this);
    }

    @Override
    public Future<?> importBreed(String breed) {
        return executorService.submit(this);
    }

    @Override
    public void run() {
        // used for unit-testing only, do nothing
    }
}
