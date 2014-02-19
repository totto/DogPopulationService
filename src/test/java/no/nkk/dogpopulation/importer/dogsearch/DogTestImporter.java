package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.importer.PedigreeImporter;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogTestImporter implements PedigreeImporter, Callable<String> {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public Future<String> importPedigree(String id) {
        return executorService.submit(this);
    }

    @Override
    public TraversalStatistics importDogPedigree(String id) {
        return new TraversalStatistics(id);
    }

    @Override
    public String call() {
        // used for unit-testing only, do nothing
        return null;
    }
}
