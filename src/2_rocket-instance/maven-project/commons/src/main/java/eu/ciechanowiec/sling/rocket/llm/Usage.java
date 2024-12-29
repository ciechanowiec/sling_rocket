package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.ciechanowiec.sling.rocket.commons.JSON;

/**
 * Usage statistics for a {@link ChatCompletionRequest} used to generate a {@link ChatCompletion}.
 */
@SuppressWarnings("ClassReferencesSubclass")
@JsonDeserialize(as = UsageDefault.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Usage extends JSON {

    /**
     * Returns the number of tokens in the prompt.
     * @return number of tokens in the prompt
     */
    int promptTokens();

    /**
     * Returns the number of tokens in the generated {@link ChatCompletion}.
     * @return number of tokens in the generated {@link ChatCompletion}
     */
    int completionTokens();

    /**
     * Returns the total number of tokens used in the {@link ChatCompletion} request (prompt + completion).
     * @return total number of tokens used in the {@link ChatCompletion} request
     */
    int totalTokens();
}
