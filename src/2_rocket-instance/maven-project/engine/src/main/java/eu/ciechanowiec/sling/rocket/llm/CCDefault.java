package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link ChatCompletion}.
 */
@ToString
@SuppressWarnings("PMD.DataClass")
public class CCDefault implements ChatCompletion {

    private final String id;
    private final List<Choice> choices;
    private final LocalDateTime created;
    private final String model;
    private final Usage usage;

    /**
     * Constructs an instance of this class.
     *
     * @param id      same as {@link ChatCompletion#id()}
     * @param choices same as {@link ChatCompletion#choices()}
     * @param created same as {@link ChatCompletion#created()}
     * @param model   same as {@link ChatCompletion#model()}
     * @param usage   same as {@link ChatCompletion#usage()}
     */
    @SuppressWarnings({"ConstructorWithTooManyParameters", "ParameterNumber", "PMD.ExcessiveParameterList"})
    public CCDefault(
        @JsonProperty("id")
        String id,
        @JsonProperty("choices")
        List<Choice> choices,
        @JsonProperty("created")
        long created,
        @JsonProperty("model")
        String model,
        @JsonProperty("usage")
        Usage usage
    ) {
        this.id = id;
        this.choices = Collections.unmodifiableList(choices);
        this.created = LocalDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneOffset.UTC);
        this.model = model;
        this.usage = usage;
    }

    @Override
    @JsonProperty("id")
    public String id() {
        return id;
    }

    @Override
    @JsonProperty("choices")
    public List<Choice> choices() {
        return choices;
    }

    @Override
    @JsonProperty("created")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    public LocalDateTime created() {
        return created;
    }

    @Override
    @JsonProperty("model")
    public String model() {
        return model;
    }

    @Override
    @JsonProperty("usage")
    public Usage usage() {
        return usage;
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return objectMapper.writeValueAsString(this);
    }
}
