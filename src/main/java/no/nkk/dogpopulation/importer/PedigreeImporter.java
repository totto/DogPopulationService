package no.nkk.dogpopulation.importer;

import no.nkk.dogpopulation.importer.dogsearch.TraversalStatistics;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface PedigreeImporter {

    Future<String> importPedigree(final String id);

    TraversalStatistics importDogPedigree(String id);
}
