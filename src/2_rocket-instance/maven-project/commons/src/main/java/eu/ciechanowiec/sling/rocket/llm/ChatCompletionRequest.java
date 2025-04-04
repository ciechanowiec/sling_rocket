package eu.ciechanowiec.sling.rocket.llm;

/**
 * HTTP request to generate {@link ChatCompletion}.
 */
@FunctionalInterface
public interface ChatCompletionRequest {

    /**
     * Executes this {@link ChatCompletionRequest} and returns the generated {@link ChatCompletion}.
     *
     * @return {@link ChatCompletion} generated by this {@link ChatCompletionRequest}
     */
    ChatCompletion execute();
}
