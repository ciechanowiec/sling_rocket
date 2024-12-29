package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.ciechanowiec.sling.rocket.commons.JSON;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat completion response returned by {@link LLM}, based on the provided input.
 */
@SuppressWarnings("ClassReferencesSubclass")
@JsonDeserialize(as = CCDefault.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ChatCompletion extends JSON {

    /**
     * Unique identifier for this {@link ChatCompletion}.
     * @return unique identifier for this {@link ChatCompletion}
     */
    String id();

    /**
     * List of available choices for this {@link ChatCompletion}.
     * @return list of available choices for this {@link ChatCompletion}
     */
    List<Choice> choices();

    /**
     * Time when this {@link ChatCompletion} was created.
     * @return time when this {@link ChatCompletion} was created
     */
    LocalDateTime created();

    /**
     * ID of an {@link LLM} used to generate this {@link ChatCompletion}.
     * @return ID of an {@link LLM} used to generate this {@link ChatCompletion}
     */
    String model();

    /**
     * Usage statistics for the {@link ChatCompletionRequest} used to generate this {@link ChatCompletion}.
     * @return usage statistics for the {@link ChatCompletionRequest} used to generate this {@link ChatCompletion}
     */
    Usage usage();
}
