package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

/**
 * Default implementation of {@link Usage}.
 *
 * @param promptTokens     same as {@link Usage#promptTokens()}
 * @param completionTokens same as {@link Usage#completionTokens()}
 * @param totalTokens      same as {@link Usage#totalTokens()}
 */
@SuppressWarnings("WeakerAccess")
public record UsageDefault(
    @JsonProperty("prompt_tokens") int promptTokens,
    @JsonProperty("completion_tokens") int completionTokens,
    @JsonProperty("total_tokens") int totalTokens
) implements Usage {

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this);
    }
}
