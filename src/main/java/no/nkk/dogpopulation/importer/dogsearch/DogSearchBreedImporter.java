package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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

    private int importBreedPedigree(final String breed, BreedImportStatus progress) {
        LOGGER.info("Looking up all UUIDs on dogsearch for breed {}", breed);
        Set<String> breedIds = dogSearchClient.listIdsForBreed(breed);
        LOGGER.info("Found {} {} dogs on dogsearch, importing pedigrees...", breedIds.size(), breed);
        progress.setTotalTasks(breedIds.size());
        progress.updateStartTime();
        final int NTHREADS = 3;
        ExecutorService breedExecutor = Executors.newFixedThreadPool(NTHREADS, new ThreadFactory() {
            private final AtomicInteger nextId = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName(breed + " - breed-importer-" + nextId.getAndIncrement());
                return thread;
            }
        });
        int n = 0;
        for (String id : breedIds) {
            breedExecutor.submit(createTaskForImportOfPedigree(breed, progress, id));
            n++;
        }
        try {
            breedExecutor.shutdown();
            breedExecutor.awaitTermination(3, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        progress.updateComplete();
        try {
            Thread.sleep(5 * 60 * 1000); // wait for bulk-writer to complete
        } catch (InterruptedException ignore) {
        }
        pedigreeImporter.stop();
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
                    TraversalStatistics ts = ((DogSearchPedigreeImporter) pedigreeImporter).importDogPedigree(id, progress.getTraversalStatistics());
                    LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogsAdded.get() + ts.puppiesAdded.get(), id);
                    progress.recordTaskComplete();
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        };
    }

}
