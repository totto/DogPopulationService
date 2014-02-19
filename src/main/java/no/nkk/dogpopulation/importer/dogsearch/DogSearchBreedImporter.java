package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchBreedImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchBreedImporter.class);

    private final ExecutorService executorService;
    private final PedigreeImporter pedigreeImporter;
    private final DogSearchClient dogSearchClient;

    public DogSearchBreedImporter(ExecutorService executorService, PedigreeImporter pedigreeImporter, DogSearchClient dogSearchClient) {
        this.executorService = executorService;
        this.pedigreeImporter = pedigreeImporter;
        this.dogSearchClient = dogSearchClient;
    }

    public Future<Integer> importBreed(String breed, BreedImportStatus progress) {
        return executorService.submit(importBreedTaskFor(breed, progress));
    }

    public Callable<Integer> importBreedTaskFor(final String breed, final BreedImportStatus progress) {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(breed);
                try {
                    return importBreedPedigree(breed, progress);
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        };
    }

    private int importBreedPedigree(String breed, BreedImportStatus progress) {
        LOGGER.info("Looking up all UUIDs on dogsearch for breed {}", breed);
        Set<String> breedIds = dogSearchClient.listIdsForBreed(breed);
        LOGGER.info("Found {} {} dogs on dogsearch, importing pedigrees...", breedIds.size(), breed);
        progress.setOriginalPedigreeCount(breedIds.size());
        progress.updateStartTime();
        final int NTHREADS = 4;
        ExecutorService breedExecutor = Executors.newFixedThreadPool(NTHREADS);
        int n = 0;
        for (String id : breedIds) {
            breedExecutor.submit(createTaskForImportOfPedigree(breed, progress, id));
            n++;
        }
        try {
            breedExecutor.shutdown();
            breedExecutor.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        progress.updateComplete();
        LOGGER.info("Submitted {} pedigree tasks to {} threads.", n, NTHREADS);
        return n;
    }

    private Runnable createTaskForImportOfPedigree(final String breed, final BreedImportStatus progress, final String id) {
        return new Runnable() {
            @Override
            public void run() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(breed);
                try {
                    TraversalStatistics ts = pedigreeImporter.importDogPedigree(id);
                    LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogCount, id);
                    progress.updateWith(ts);
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        };
    }

}
