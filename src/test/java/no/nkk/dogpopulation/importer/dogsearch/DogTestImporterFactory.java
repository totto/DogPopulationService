package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogTestImporterFactory implements PedigreeImporterFactory {
    @Override
    public PedigreeImporter createInstance(String context) {
        return new DogTestImporter();
    }
}
