package no.nkk.dogpopulation.importer;

import no.nkk.dogpopulation.graph.bulkwrite.BulkWriteService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface PedigreeImporterFactory {
    PedigreeImporter createInstance(BulkWriteService bulkWriteService);
}
