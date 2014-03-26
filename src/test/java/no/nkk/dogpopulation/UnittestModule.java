package no.nkk.dogpopulation;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.jayway.restassured.RestAssured;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.DogTestImporter;
import no.nkk.dogpopulation.importer.dogsearch.FileReadingDogSearchClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides application wide configuration.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class UnittestModule extends AbstractModule {

    @Override
    protected void configure() {
        RestAssured.port = 18051;

        String graphDbPath = "target/unittestdogdb";
        File graphDbFolder = new File(graphDbPath);
        FileUtils.deleteQuietly(graphDbFolder);

        final String hdIndexFolderPath = "target/hdindex-test";
        File hdIndexFolder = new File(hdIndexFolderPath);
        FileUtils.deleteQuietly(hdIndexFolder);

        bind(String.class).annotatedWith(Names.named("neo4jFolder")).toInstance(graphDbPath);

        bind(String.class).annotatedWith(Names.named("hdindex-folder")).toInstance(hdIndexFolderPath);
        bind(URL.class).annotatedWith(Names.named("breedJsonUrl")).toInstance(toUrl(new File("src/test/resources/breedimport/Raser.json")));

        bind(int.class).annotatedWith(Names.named("httpPort")).toInstance(RestAssured.port);
        bind(int.class).annotatedWith(Names.named("maxThreads")).toInstance(3);
        bind(int.class).annotatedWith(Names.named("minThreads")).toInstance(1);

        bind(PedigreeImporter.class).to(DogTestImporter.class);

        bind(String.class).annotatedWith(Names.named("pedigree-test-uuid")).toInstance("93683d2b-3ad9-4531-bb3d-d8c43f9d99f0");
        bind(DogSearchClient.class).to(FileReadingDogSearchClient.class);
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
