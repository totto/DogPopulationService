package no.nkk.dogpopulation.pedigree;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import no.nkk.dogpopulation.importer.DogImporter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeService {

    private final GraphQueryService graphQueryService;

    private final DogImporter dogImporter;

    public PedigreeService(GraphQueryService graphQueryService, DogImporter dogImporter) {
        this.graphQueryService = graphQueryService;
        this.dogImporter = dogImporter;
    }

    public TopLevelDog getPedigree(String uuid) {
        TopLevelDog dog = graphQueryService.getPedigree(uuid);

        if (dog == null) {
            Future<?> future = dogImporter.importDog(uuid);

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
