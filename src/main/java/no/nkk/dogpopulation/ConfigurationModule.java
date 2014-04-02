package no.nkk.dogpopulation;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import no.nkk.dogpopulation.importer.PedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchClient;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchPedigreeImporter;
import no.nkk.dogpopulation.importer.dogsearch.DogSearchSolrClient;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides application wide configuration.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ConfigurationModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("hdindex-folder")).toInstance("hdindex");

        bind(String.class).annotatedWith(Names.named("dogServiceUrl")).toInstance("http://dogsearch.nkk.no/dogservice/dogs");
        bind(URL.class).annotatedWith(Names.named("breedJsonUrl")).toInstance(toUrl("http://dogid.nkk.no/ras/Raser.json"));
        bind(String.class).annotatedWith(Names.named("neo4jFolder")).toInstance("data/dogdb");
        bind(int.class).annotatedWith(Names.named("httpPort")).toInstance(8051);
        bind(int.class).annotatedWith(Names.named("maxThreads")).toInstance(50);
        bind(int.class).annotatedWith(Names.named("minThreads")).toInstance(5);

        bind(PedigreeImporter.class).to(DogSearchPedigreeImporter.class);
        bind(DogSearchClient.class).to(DogSearchSolrClient.class);
    }

    private static URL toUrl(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
