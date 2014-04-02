package no.nkk.dogpopulation.importer.breedupdater;

import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.concurrent.ManageableExecutor;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchPedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.TraversalStatistics;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
    private final PedigreeImporter pedigreeImporter;
    private final DogSearchClient dogSearchClient;
    private final GraphQueryService graphQueryService;


    /*
     * Required context
     */
    private final String breed;
    private final BreedImportStatus progress;

    private final String executorName;

    private final Runnable postProcessingTask;

    private ManageableExecutor manageableExecutor;

    public BreedImporterTask(Runnable postProcessingTask, PedigreeImporter pedigreeImporter, ExecutorManager executorManager, DogSearchClient dogSearchClient, String breed, BreedImportStatus progress, GraphQueryService graphQueryService) {
        this.executorManager = executorManager;
        this.pedigreeImporter = pedigreeImporter;
        this.dogSearchClient = dogSearchClient;
        this.breed = breed;
        this.progress = progress;
        this.graphQueryService = graphQueryService;
        executorName = "breed-importer " + breed;
        this.postProcessingTask = postProcessingTask;
    }


    @Override
    public Integer call() {
        try {
            LocalDateTime from = graphQueryService.getUpdatedTo(breed);
            LOGGER.debug("Starting update of breed {}, from {}", breed, from.toString());
            int timeWindowMinutes = 24 * 60;
            LocalDateTime to = from.plusMinutes(timeWindowMinutes);
            int n = 0;
            try {
                progress.updateStartTime();
                while (LocalDateTime.now().isAfter(to)) {
                    LOGGER.trace("Searching {} dogs timestamped between {} and {}, importing pedigrees...", breed, from, to);
                    Set<String> breedIds = dogSearchClient.listIdsForBreed(breed, from, to);
                    if (!breedIds.isEmpty()) {
                        LOGGER.trace("Found {} {} dogs timestamped between {} and {}, importing pedigrees...", breedIds.size(), breed, from, to);
                        progress.setTotalTasks(progress.getTotalTasks() + breedIds.size());
                        progress.setWindowTasks(breedIds.size());
                        progress.setWindowCompleted(0);
                        ExecutorService breedExecutor = getExecutorService(executorName);
                        List<Future<?>> futures = new ArrayList<>();
                        for (String id : breedIds) {
                            futures.add(breedExecutor.submit(createTaskForImportOfPedigree(breed, progress, id)));
                            n++;
                        }
                        for (Future<?> future : futures) {
                            waitForTaskToComplete(future);
                        }
                    } else {
                        LOGGER.trace("No {} dogs found between {} and {}", breed, from, to);
                    }
                    progress.setUpdatedTo(to.toString() + "Z");
                    graphQueryService.setUpdatedTo(breed, to);
                    from = graphQueryService.getUpdatedTo(breed);
                    to = from.plusMinutes(timeWindowMinutes);
                }
                LOGGER.debug("Completed updating breed {}, to {}", breed, to.toString());
                shutdownExecutor();
                return n;
            } finally {
                postProcessingTask.run();
            }
        } catch (RuntimeException e) {
            LOGGER.error("", e);
            throw e;
        } catch (Error e) {
            LOGGER.error("", e);
            throw e;
        } catch (Throwable e) {
            LOGGER.error("", e);
            throw e;
        }
    }

    private void waitForTaskToComplete(Future<?> future) {
        try {
            future.get();
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
    }

    private void shutdownExecutor() {
        ExecutorService breedExecutor = getExecutorService(executorName);
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
    }

    private ExecutorService getExecutorService(String executorName) {
        if (manageableExecutor == null) {
            final int NTHREADS = 5;
            manageableExecutor = executorManager.addUnboundedQueueExecutor(executorName, NTHREADS);
        }
        return manageableExecutor;
    }

    private Runnable createTaskForImportOfPedigree(final String breed, final BreedImportStatus progress, final String id) {
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
