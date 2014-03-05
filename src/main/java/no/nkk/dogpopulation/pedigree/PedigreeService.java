package no.nkk.dogpopulation.pedigree;

import no.nkk.dogpopulation.graph.GraphQueryService;
import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;
import no.nkk.dogpopulation.graph.pedigree.TopLevelDog;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

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

    public PedigreeService(GraphDatabaseService graphDb, GraphQueryService graphQueryService, PedigreeImporterFactory pedigreeImporterFactory) {
        this.graphQueryService = graphQueryService;
        this.pedigreeImporter = pedigreeImporterFactory.createInstance(new BulkWriteService(graphDb));
    }

    public TopLevelDog getPedigree(String id) {
        if (loadPedigree(id)) {
            return null;
        }

        TopLevelDog dog = graphQueryService.getPedigree(id);

        return dog;
    }

    private boolean loadPedigree(String id) {
        Node dogWithMoreThanOneParent = graphQueryService.getDogIfItHasAtLeastOneParent(id);

        if (dogWithMoreThanOneParent == null) {
            Future<?> future = pedigreeImporter.importPedigree(id);

            try {
                future.get(60, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                return true;
            }
        }
        return false;
    }

    public TopLevelDog getFicticiousPedigree(String uuid, String name, String fatherUuid, String motherUuid) {
        if (loadPedigree(fatherUuid)) {
            return null;
        }
        if (loadPedigree(motherUuid)) {
            return null;
        }

        TopLevelDog dog = graphQueryService.getPedigree(uuid, name, fatherUuid, motherUuid);

        return dog;
    }
}
