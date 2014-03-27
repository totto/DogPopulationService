package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedImporterTask implements Callable<Integer> {


    private static final Logger LOGGER = LoggerFactory.getLogger(BreedImporterTask.class);


    /*
     * Services
     */

    private final ExecutorManager executorManager;
    private final PedigreeImporterFactory pedigreeImporterFactory;
    private final DogSearchClient dogSearchClient;


    /*
     * Required context
     */
    private final String breed;
    private final BreedImportStatus progress;


    public BreedImporterTask(PedigreeImporterFactory pedigreeImporterFactory, ExecutorManager executorManager, DogSearchClient dogSearchClient, String breed, BreedImportStatus progress) {
        this.executorManager = executorManager;
        this.pedigreeImporterFactory = pedigreeImporterFactory;
        this.dogSearchClient = dogSearchClient;
        this.breed = breed;
        this.progress = progress;
    }


    @Override
    public Integer call() {
        LOGGER.info("Looking up all UUIDs on dogsearch for breed {}", breed);
        Future<Set<String>> breedIdsFuture = dogSearchClient.listIdsForBreed(breed);
        Set<String> breedIds = null;
        try {
            breedIds = breedIdsFuture.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
        LOGGER.info("Found {} {} dogs on dogsearch, importing pedigrees...", breedIds.size(), breed);
        progress.setTotalTasks(breedIds.size());
        progress.updateStartTime();
        final int NTHREADS = 5;
        final String executorName = "breed-importer " + breed;
        ExecutorService breedExecutor = executorManager.addUnboundedQueueExecutor(executorName, NTHREADS);
        PedigreeImporter breedSpecificPedigreeImporter = pedigreeImporterFactory.createInstance(breed);
        try {
            int n = 0;
            for (String id : breedIds) {
                breedExecutor.submit(createTaskForImportOfPedigree(breedSpecificPedigreeImporter, breed, progress, id));
                n++;
            }
            breedExecutor.shutdown();
            try {
                if (!breedExecutor.awaitTermination(3, TimeUnit.DAYS)) {
                    breedExecutor.shutdownNow();
                    if (!breedExecutor.awaitTermination(2, TimeUnit.HOURS)) {
                        LOGGER.error("Thread-pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                breedExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executorManager.removeExecutor(executorName);
            progress.updateComplete();
            try {
                Thread.sleep(5 * 60 * 1000); // wait for bulk-writer to complete
            } catch (InterruptedException ignore) {
            }
            LOGGER.info("Submitted {} pedigree tasks to {} threads.", n, NTHREADS);
            return n;
        } finally {
            breedSpecificPedigreeImporter.stop();
        }
    }

    private Runnable createTaskForImportOfPedigree(final PedigreeImporter pedigreeImporter, final String breed, final BreedImportStatus progress, final String id) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    TraversalStatistics ts = ((DogSearchPedigreeImporter) pedigreeImporter).importDogPedigree(id, progress.getTraversalStatistics());
                    LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogsAdded.get() + ts.puppiesAdded.get(), id);
                } catch(RuntimeException e) {
                    LOGGER.error(String.format("breed = %s, id = %s", breed, id), e);
                } finally {
                    progress.recordTaskComplete();
                }
            }

            @Override
            public String toString() {
                return String.format("Import breed %s, id = %s", breed, id);
            }
        };
    }

    @Override
    public String toString() {
        return "BreedImporterTask{" +
                "breed='" + breed + '\'' +
                ", progress=" + progress +
                '}';
    }
}
