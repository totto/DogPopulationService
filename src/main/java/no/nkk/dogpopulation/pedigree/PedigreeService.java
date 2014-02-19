package no.nkk.dogpopulation.pedigree;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import no.nkk.dogpopulation.importer.PedigreeImporter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeService {

    private final GraphQueryService graphQueryService;

    private final PedigreeImporter pedigreeImporter;

    public PedigreeService(GraphQueryService graphQueryService, PedigreeImporter pedigreeImporter) {
        this.graphQueryService = graphQueryService;
        this.pedigreeImporter = pedigreeImporter;
    }

    public TopLevelDog getPedigree(String uuid) {
        TopLevelDog dog = graphQueryService.getPedigree(uuid);

        if (dog == null) {
            Future<?> future = pedigreeImporter.importPedigree(uuid);

            try {
                future.get(60, TimeUnit.SECONDS);

                dog = graphQueryService.getPedigree(uuid);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                return null;
            }
        }

        return dog;
    }
}
