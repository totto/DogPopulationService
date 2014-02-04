package no.nkk.dogpopulation.dogsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DogDocument {
    String json_detailed;

    public String getJson_detailed() {
        return json_detailed;
    }

    public void setJson_detailed(String json_detailed) {
        this.json_detailed = json_detailed;
    }

    public DogDetails toDogDetails(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json_detailed, DogDetails.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
