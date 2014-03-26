package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.importer.PedigreeImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class UpdatesImporterTask implements Callable<Integer> {


    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatesImporterTask.class);


    /*
     * Services
     */

    private final ExecutorService executorService;
    private final PedigreeImporter pedigreeImporter;
    private final DogSearchClient dogSearchClient;


    /*
     * Required context
     */
    private final Set<String> uuids;

    public UpdatesImporterTask(ExecutorService executorService, PedigreeImporter pedigreeImporter, DogSearchClient dogSearchClient, Set<String> uuids) {
        this.executorService = executorService;
        this.pedigreeImporter = pedigreeImporter;
        this.dogSearchClient = dogSearchClient;
        this.uuids = uuids;
    }


    @Override
    public Integer call() {
        LOGGER.trace("UPDATING graph for {} uuids", uuids.size());
        int n = 0;
        for (String id : uuids) {
            executorService.submit(createTaskForImportOfPedigree(id));
            n++;
        }
        return n;
    }

    private Runnable createTaskForImportOfPedigree(final String id) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    TraversalStatistics ts = ((DogSearchPedigreeImporter) pedigreeImporter).importDogPedigree(id, new TraversalStatistics(id));
                    LOGGER.trace("Imported pedigree({} new dogs) for {}", ts.dogsAdded.get() + ts.puppiesAdded.get(), id);
                } catch(RuntimeException e) {
                    LOGGER.error(String.format("id = %s", id), e);
                }
            }

            @Override
            public String toString() {
                return String.format("Import updates id = %s", id);
            }
        };
    }

    @Override
    public String toString() {
        return String.format("UpdatesImporterTask{uuids.size=%d}", uuids.size());
    }
}
