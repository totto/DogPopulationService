package no.nkk.dogpopulation.dogsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.pedigree.Ancestry;
import no.nkk.dogpopulation.pedigree.Breed;
import no.nkk.dogpopulation.pedigree.Dog;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogSearchTestClient implements DogSearchClient {
    @Override
    public DogSearchResponse findDog(String query) {
        InputStream in = getClass().getClassLoader().getResourceAsStream("afganhund_test.json");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            DogSearchResponse dogSearchResponse = objectMapper.readValue(in, DogSearchResponse.class);
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
