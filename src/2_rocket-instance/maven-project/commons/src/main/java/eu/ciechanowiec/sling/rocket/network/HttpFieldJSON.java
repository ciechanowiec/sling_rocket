package eu.ciechanowiec.sling.rocket.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ciechanowiec.sling.rocket.commons.JSON;
import lombok.SneakyThrows;
import org.eclipse.jetty.http.HttpField;

class HttpFieldJSON implements JSON {

    private final HttpField httpField;

    HttpFieldJSON(HttpField httpField) {
        this.httpField = httpField;
    }

    @JsonProperty("httpFieldName")
    String name() {
        return httpField.getName();
    }

    @JsonProperty("httpFieldValue")
    String value() {
        return httpField.getValue();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
