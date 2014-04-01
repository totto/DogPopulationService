package no.nkk.dogpopulation.importer.breedupdater;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import no.nkk.dogpopulation.importer.PedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchPedigreeImporterFactory;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchSolrClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides application wide configuration.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class IntegrationtestModule extends AbstractModule {

    @Override
    protected void configure() {
        String graphDbPath = "target/integrationtestdogdb";
        File graphDbFolder = new File(graphDbPath);
        FileUtils.deleteQuietly(graphDbFolder);
        bind(String.class).annotatedWith(Names.named("neo4jFolder")).toInstance(graphDbPath);

        bind(URL.class).annotatedWith(Names.named("breedJsonUrl")).toInstance(toUrl(new File("src/test/resources/breedimport/Raser.json")));

        bind(PedigreeImporterFactory.class).to(DogSearchPedigreeImporterFactory.class);

        bind(String.class).annotatedWith(Names.named("dogServiceUrl")).toInstance("http://dogsearch.nkk.no/dogservice/dogs");
        bind(DogSearchClient.class).to(DogSearchSolrClient.class);
    }

    private static URL toUrl(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
