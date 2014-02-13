package no.nkk.dogpopulation.importer;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface DogImporter {
    Future<?> importDog(String id);

    Future<?> importBreed(String breed);
}
