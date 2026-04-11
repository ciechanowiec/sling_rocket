package eu.ciechanowiec.sling.rocket.observation.stats.consistency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import lombok.SneakyThrows;

class JCRConsistencyUnavailable implements JSON {

    @JsonProperty
    private String status() {
        return "Unable to perform the consistency check";
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
