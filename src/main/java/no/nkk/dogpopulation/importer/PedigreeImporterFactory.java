package no.nkk.dogpopulation.importer;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface PedigreeImporterFactory {
    PedigreeImporter createInstance(String context);
}
