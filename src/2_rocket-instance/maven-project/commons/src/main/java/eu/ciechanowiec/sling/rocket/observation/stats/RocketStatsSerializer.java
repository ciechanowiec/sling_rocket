package eu.ciechanowiec.sling.rocket.observation.stats;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

class RocketStatsSerializer extends JsonSerializer<RocketStats> {

    @Override
    public void serialize(RocketStats value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField(value.name(), value);
        gen.writeEndObject();
    }
}
