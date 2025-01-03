package eu.ciechanowiec.sling.rocket.llm;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.net.URI;

/**
 * Configuration that describes how the associated {@link LLM} should be used.
 */
@ObjectClassDefinition
public @interface LLMConfig {

    /**
     * {@link URI} of the {@link LLM} API to send {@link ChatCompletionRequest}s to.
     * @return {@link URI} of the {@link LLM} API to send {@link ChatCompletionRequest}s to
     */
    @AttributeDefinition(
            name = "LLM API URL",
            description = "URI of the LLM API to send ChatCompletionRequest-s to.",
            defaultValue = "https://api.openai.com/v1/chat/completions",
            type = AttributeType.STRING
    )
    String llm_api_url() default "https://api.openai.com/v1/chat/completions";

    /**
     * Bearer token for authenticating {@link ChatCompletionRequest}s against the {@link LLM} API.
     * @return Bearer token for authenticating {@link ChatCompletionRequest}s against the {@link LLM} API
     */
    @AttributeDefinition(
            name = "LLM API Bearer Token",
            description = "Bearer token for authenticating ChatCompletionRequest-s against the LLM API.",
            defaultValue = StringUtils.EMPTY,
            type = AttributeType.PASSWORD
    )
    String llm_api_bearer_token() default StringUtils.EMPTY;

    /**
     * ID of an {@link LLM} to use to generate {@link ChatCompletion}s.
     * @return ID of an {@link LLM} to use to generate {@link ChatCompletion}s
     */
    @AttributeDefinition(
            name = "model",
            description = "ID of an LLM to use to generate ChatCompletion-s.",
            defaultValue = "gpt-4o",
            type = AttributeType.STRING
    )
    String llm_model() default "gpt-4o";

    /**
     * Maximum number of tokens to generate for a single {@link ChatCompletion}.
     * {@link LLM} may produce fewer tokens than this, but it will never generate more.
     * <p>
     * Either {@code llm.max_tokens} or {@code llm.max_completion_tokens} should be set
     * to a non-zero value (only one of them). Setting both to non-zero values might lead to invalid API responses.
     * @return maximum number of tokens to generate for a single {@link ChatCompletion}
     */
    @AttributeDefinition(
            name = "max_tokens",
            description = "Maximum number of tokens to generate for a single ChatCompletion. "
                        + "LLM may produce fewer tokens than this, but it will never generate more."
                        + "Either 'llm.max_tokens' or 'llm.max_completion_tokens' should be set to a non-zero value "
                        + "(only one of them). Setting both to non-zero values might lead to invalid API responses.",
            defaultValue = "0",
            type = AttributeType.INTEGER,
            min = "0"
    )
    int llm_max__tokens() default 0;

    /**
     * Maximum number of tokens to generate for a single {@link ChatCompletion}.
     * {@link LLM} may produce fewer tokens than this, but it will never generate more.
     * <p>
     * Either {@code llm.max_tokens} or {@code llm.max_completion_tokens} should be set
     * to a non-zero value (only one of them). Setting both to non-zero values might lead to invalid API responses.
     * @return maximum number of tokens to generate for a single {@link ChatCompletion}
     */
    @AttributeDefinition(
            name = "max_completion_tokens",
            description = "Maximum number of tokens to generate for a single ChatCompletion. "
                        + "LLM may produce fewer tokens than this, but it will never generate more."
                        + "Either 'llm.max_tokens' or 'llm.max_completion_tokens' should be set to a non-zero value "
                        + "(only one of them). Setting both to non-zero values might lead to invalid API responses.",
            defaultValue = "0",
            type = AttributeType.INTEGER,
            min = "0"
    )
    int llm_max__completion__tokens() default 0;

    /**
     * Parameter that controls the diversity of generated text by reducing the likelihood of repeated sequences.
     * Higher values decrease repetition, but when the value is too high, incoherent sentences might be generated.
     * @return parameter that controls the diversity of generated text by reducing the likelihood of repeated sequences;
     *         higher values decrease repetition, but when the value is too high,
     *         incoherent sentences might be generated.
     */
    @AttributeDefinition(
            name = "frequency_penalty",
            description = "Parameter that controls the diversity of generated text by reducing the likelihood of "
                    + "repeated sequences. Higher values decrease repetition, but when the value is too high, "
                    + "incoherent sentences might be generated.",
            defaultValue = "0",
            type = AttributeType.FLOAT,
            min = "-2",
            max = "2"
    )
    float llm_frequency__penalty() default 0;

    /**
     * Parameter that determines the degree of randomness in the response. Higher values like will make
     * the output more random, while lower values will make it more focused and deterministic.
     * @return parameter that determines the degree of randomness in the response; higher values like will make
     *         the output more random, while lower values will make it more focused and deterministic.
     */
    @AttributeDefinition(
            name = "temperature",
            description = "Parameter that determines the degree of randomness in the response. Higher values like "
                    + "will make the output more random, while lower values will make "
                    + "it more focused and deterministic.",
            defaultValue = "0",
            type = AttributeType.FLOAT,
            min = "0",
            max = "2"
    )
    float llm_temperature() default 0;

    /**
     * Parameter that tells the {@link LLM} how many of the most likely next words to consider. The {@link LLM} starts
     * with the highest-probability word and adds the next most likely words until their total probability
     * equals or exceeds <i>p</i>. Then it randomly picks from this set.
     * <p>
     * For example, if {@code top_p=0.9}, the model looks at all possible next words, starting with the most likely,
     * and keeps adding words until their combined probability is at least 90%. Any words outside this '90% bubble'
     * are not considered. This method helps strike a balance between picking the single most likely word every
     * time (which can make text feel robotic) and choosing from too wide a range of words (which might make it
     * incoherent). Higher {@code top_p} (e.g., {@code 0.95} or {@code 1.0}) includes more possible words, leading to
     * more varied and creative text, but it can also become less focused. Lower {@code top_p} (e.g., {@code 0.7})
     * limits the choice to fewer words, making text more predictable and consistent, though sometimes too repetitive.
     * @return parameter that tells the {@link LLM} how many of the most likely next words to consider
     */
    @AttributeDefinition(
            name = "top_p",
            description = "Parameter that tells the LLM how many of the most likely next words to consider. "
                    + "The LLM starts with the highest-probability word and adds the next most likely words until "
                    + "their total probability equals or exceeds p. Then it randomly picks from this set. For example, "
                    + "if top_p=0.9, the model looks at all possible next words, starting with the most likely, and "
                    + "keeps adding words until their combined probability is at least 90%. Any words outside this "
                    + "'90% bubble' are not considered. This method helps strike a balance between picking the single "
                    + "most likely word every time (which can make text feel robotic) and choosing from too wide a "
                    + "range of words (which might make it incoherent). Higher top_p (e.g., 0.95 or 1.0) includes "
                    + "more possible words, leading to more varied and creative text, but it can also become less "
                    + "focused. Lower top_p (e.g., 0.7) limits the choice to fewer words, making text more "
                    + "predictable and consistent, though sometimes too repetitive.",
            defaultValue = "1",
            type = AttributeType.FLOAT,
            min = "0",
            max = "1"
    )
    float llm_top__p() default 1;

    @AttributeDefinition(
            name = "JCR Home",
            description = "JCR path where persistent data related to this LLM is stored",
            defaultValue = "/content/rocket/llm/common",
            type = AttributeType.STRING
    )
    String jcr_home() default "/content/rocket/llm/common";
}
