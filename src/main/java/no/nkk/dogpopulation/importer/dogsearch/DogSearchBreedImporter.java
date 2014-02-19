package no.nkk.dogpopulation.importer.dogsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchBreedImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogSearchBreedImporter.class);

    private final ExecutorService executorService;
    private final DogSearchPedigreeImporter pedigreeImporter;
    private final DogSearchClient dogSearchClient;

    public DogSearchBreedImporter(ExecutorService executorService, DogSearchPedigreeImporter pedigreeImporter, DogSearchClient dogSearchClient) {
        this.executorService = executorService;
        this.pedigreeImporter = pedigreeImporter;
        this.dogSearchClient = dogSearchClient;
    }

    public Future<Integer> importBreed(String breed) {
        return executorService.submit(importBreedTaskFor(breed));
    }

    public Callable<Integer> importBreedTaskFor(final String breed) {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                final String origThreadName = Thread.currentThread().getName();
                Thread.currentThread().setName(breed);
                try {
                    return importBreedPedigree(breed);
                } finally {
                    Thread.currentThread().setName(origThreadName);
                }
            }
        };
    }

    private int importBreedPedigree(String breed) {
        int n = 0;
        LOGGER.info("Looking up all UUIDs on dogsearch for breed {}", breed);
        Set<String> breedIds = dogSearchClient.listIdsForBreed(breed);
        LOGGER.info("Found {} {} dogs on dogsearch, importing pedigrees...", breedIds.size(), breed);
        int i=0;
        for (String id : breedIds) {
            TraversalStatistics ts = pedigreeImporter.importDogPedigree(id);
            LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogCount, id);
            n += ts.dogCount.get();
            i++;
            if (i%100 == 0) {
                LOGGER.debug("Progress: {} of {} -- {}%", i, breedIds.size(), 100 * i / breedIds.size());
            }
        }
        LOGGER.info("Completed pedigree import of {} {} dogs from dogsearch to graph.", n, breed);
        return n;
    }

}
