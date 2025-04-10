package eu.ciechanowiec.sling.rocket.llm;

import eu.ciechanowiec.sling.rocket.commons.JSON;
import java.util.List;

/**
 * Body of {@link ChatCompletionRequest}.
 */
public interface ChatCompletionRequestBody extends JSON {

    /**
     * {@link LLMConfig#llm_model()}.
     *
     * @return {@link LLMConfig#llm_model()}
     */
    String model();

    /**
     * List of consecutive {@link ChatMessage}-s from a single {@link Chat} out of which a {@link ChatCompletion} should
     * be generated.
     *
     * @return list of consecutive {@link ChatMessage}-s from a single {@link Chat} out of which a
     * {@link ChatCompletion} should be generated
     */
    List<ChatMessage> messages();
}
