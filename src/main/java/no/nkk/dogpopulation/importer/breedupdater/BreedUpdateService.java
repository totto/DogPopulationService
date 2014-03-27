package no.nkk.dogpopulation.importer.breedupdater;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import no.nkk.dogpopulation.concurrent.ExecutorManager;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;

import java.util.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class BreedUpdateService {

    private final Map<String, BreedImportStatus> breedImportStatus = new LinkedHashMap<>(); // keep references forever

    private final DogSearchClient dogSearchClient;

    private final ExecutorManager executorManager;

    private final PedigreeImporterFactory pedigreeImporterFactory;


    @Inject
    public BreedUpdateService(DogSearchClient dogSearchClient, ExecutorManager executorManager, PedigreeImporterFactory pedigreeImporterFactory) {
        this.dogSearchClient = dogSearchClient;
        this.executorManager = executorManager;
        this.pedigreeImporterFactory = pedigreeImporterFactory;
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
            BreedImporterTask breedImporterTask = new BreedImporterTask(pedigreeImporterFactory, executorManager, dogSearchClient, breed, progress);
            executorManager.getExecutor(ExecutorManager.BREED_IMPORTER_MAP_KEY).submit(breedImporterTask);
        }
        return progress;
    }

}
