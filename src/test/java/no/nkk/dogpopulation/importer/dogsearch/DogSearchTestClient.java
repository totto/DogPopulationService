package no.nkk.dogpopulation.importer.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.graph.Ancestry;
import no.nkk.dogpopulation.graph.Breed;
import no.nkk.dogpopulation.graph.Dog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchTestClient implements DogSearchClient {
    @Override
    public Set<String> listIdsForBreed(String breed) {
        return null;
    }

    @Override
    public DogDetails findDog(String id) {
        InputStream in = getClass().getClassLoader().getResourceAsStream("afganhund_test.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            DogDetails dogSearchResponse = objectMapper.readValue(in, DogDetails.class);
            return dogSearchResponse;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void testData() {
        Dog dog = new Dog("Robodoggy", new Breed("Rottweiler"));
        dog.getIds().put("RegNo", "NO/12345/03");
        dog.setInbreedingCoefficient(236);
        Dog father = new Dog("BigDaddy", new Breed("Rottweiler"));
        father.getIds().put("RegNo", "NO/23456/96");
        father.setInbreedingCoefficient(196);
        Dog mother = new Dog("FatMama", new Breed("Rottweiler"));
        mother.getIds().put("RegNo", "NO/34567/98");
        mother.setInbreedingCoefficient(215);
        dog.setAncestry(new Ancestry(father, mother));
    }
}
