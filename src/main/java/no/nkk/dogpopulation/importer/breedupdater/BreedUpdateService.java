package no.nkk.dogpopulation.importer.breedupdater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import org.joda.time.LocalDateTime;

import java.util.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class BreedUpdateService {

    private final Map<String, BreedImportStatus> breedImportStatus = new LinkedHashMap<>();

    private final DogSearchClient dogSearchClient;

    private final ExecutorManager executorManager;

    private final PedigreeImporter pedigreeImporter;

    private final GraphQueryService graphQueryService;

    @Inject
    public BreedUpdateService(DogSearchClient dogSearchClient, ExecutorManager executorManager, PedigreeImporter pedigreeImporter, GraphQueryService graphQueryService) {
        this.dogSearchClient = dogSearchClient;
        this.executorManager = executorManager;
        this.pedigreeImporter = pedigreeImporter;
        this.graphQueryService = graphQueryService;
    }


    public void initializeRecurringUpdates() {
        Timer timer = new Timer("Recurring graph updater", true);
        final int initialDelaySeconds = 5;
        final int periodSeconds = 60;
        timer.schedule(createBreedUpdaterTask(), initialDelaySeconds * 1000, periodSeconds * 1000);
    }

    TimerTask createBreedUpdaterTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Map<String, LocalDateTime> breedSynonyms = graphQueryService.mapUpdatedToByBreedSynonymWhereUpdateToIsSet();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime ninetySecondsBeforeNow = now.minusSeconds(90);
                LocalDateTime ninetyMinutesBeforeNow = now.minusMinutes(90);
                LocalDateTime thirtySixHoursBeforeNow = now.minusHours(36);
                for (Map.Entry<String, LocalDateTime> e : breedSynonyms.entrySet()) {
                    String breedSynonym = e.getKey();
                    LocalDateTime updatedTo = e.getValue();
                    if (updatedTo.isAfter(ninetySecondsBeforeNow)) {
                        // data is already updated within last ninety seconds
                        continue;
                    }
                    if (updatedTo.isAfter(ninetyMinutesBeforeNow)) {
                        // data is updated within last ninety minutes
                        importBreed(breedSynonym, 60);
                        continue;
                    }
                    if (updatedTo.isAfter(thirtySixHoursBeforeNow)) {
                        // data is updated within last thirty six hours
                        importBreed(breedSynonym, 60 * 60);
                        continue;
                    }
                    // data is older than thirty six hours
                    importBreed(breedSynonym, 24 * 60 * 60);
                }
            }
        };
    }


    public BreedImportStatusAggregate statusAggregate() {
        List<BreedImportStatus> statusList;
        synchronized (breedImportStatus) {
            statusList = new ArrayList<>(breedImportStatus.values());
        }
        Collections.reverse(statusList); // order by newest entry first in list

        BreedImportStatusAggregate statusAggregate = new BreedImportStatusAggregate(statusList);

        return statusAggregate;
    }


    public BreedImportStatus importBreed(String breed, int timeWindowSeconds) {
        BreedImportStatus progress;
        boolean shouldImport = false;
        synchronized (breedImportStatus) {
            progress = breedImportStatus.get(breed);
            if (progress == null) {
                shouldImport = true;
                progress = new BreedImportStatus(breed);
                breedImportStatus.put(breed, progress);
            }
        }

        if (shouldImport) {
            Runnable postProcessingTask = createPostProcessingTask(breed);
            BreedImporterTask breedImporterTask = new BreedImporterTask(postProcessingTask, pedigreeImporter, executorManager, dogSearchClient, breed, progress, graphQueryService, timeWindowSeconds);
            executorManager.getExecutor(ExecutorManager.BREED_IMPORTER_MAP_KEY).submit(breedImporterTask);
        }
        return progress;
    }

    private Runnable createPostProcessingTask(final String breed) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (breedImportStatus) {
                    breedImportStatus.remove(breed);
                }
            }
        };
    }

}
