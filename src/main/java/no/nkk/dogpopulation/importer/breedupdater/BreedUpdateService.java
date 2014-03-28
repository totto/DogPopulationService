package no.nkk.dogpopulation.importer.breedupdater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;

import java.util.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class BreedUpdateService {

    private final Map<String, BreedImportStatus> breedImportStatus = new LinkedHashMap<>();

    private final DogSearchClient dogSearchClient;

    private final ExecutorManager executorManager;

    private final PedigreeImporterFactory pedigreeImporterFactory;

    private final GraphQueryService graphQueryService;

    @Inject
    public BreedUpdateService(DogSearchClient dogSearchClient, ExecutorManager executorManager, PedigreeImporterFactory pedigreeImporterFactory, GraphQueryService graphQueryService) {
        this.dogSearchClient = dogSearchClient;
        this.executorManager = executorManager;
        this.pedigreeImporterFactory = pedigreeImporterFactory;
        this.graphQueryService = graphQueryService;
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


    public BreedImportStatus importBreed(String breed) {
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
            BreedImporterTask breedImporterTask = new BreedImporterTask(postProcessingTask, pedigreeImporterFactory, executorManager, dogSearchClient, breed, progress, graphQueryService);
            executorManager.getExecutor(ExecutorManager.BREED_IMPORTER_MAP_KEY).submit(breedImporterTask);
        }
        return progress;
    }

    private Runnable createPostProcessingTask(final String breed) {
        return new Runnable() {
            @Override
            public void run() {
                breedImportStatus.remove(breed);
            }
        };
    }

}
