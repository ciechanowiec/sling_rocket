package eu.ciechanowiec.sling.rocket.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link ChatCompletionRequestBody}.
 */
@SuppressWarnings("WeakerAccess")
@ToString
public class CCRequestBodyDefault implements ChatCompletionRequestBody {

    private final String model;
    private final List<ChatMessage> messages;
    @ToString.Exclude
    private final Supplier<Optional<Integer>> llmMaxTokens;
    @ToString.Exclude
    private final Supplier<Optional<Integer>> llmMaxCompletionTokens;
    @ToString.Exclude
    private final Supplier<Optional<Float>> llmTemperature;
    @ToString.Exclude
    private final Supplier<Optional<Float>> llmFrequencyPenalty;
    @ToString.Exclude
    private final Supplier<Optional<Float>> llmTopP;

    /**
     * Constructs an instance of this class.
     *
     * @param model                  same as {@link LLMConfig#llm_model()}
     * @param messages               list of consecutive {@link ChatMessage}-s from a single {@link Chat} out of which a
     *                               {@link ChatCompletion} should be generated
     * @param llmMaxTokens           same as {@link LLMConfig#llm_max__tokens()}
     * @param llmMaxCompletionTokens same as {@link LLMConfig#llm_max__completion__tokens()}
     * @param llmTemperature         same as {@link LLMConfig#llm_temperature()}
     * @param llmFrequencyPenalty    same as {@link LLMConfig#llm_frequency__penalty()}
     * @param llmTopP                same as {@link LLMConfig#llm_top__p()}
     */
    @SuppressWarnings({"ConstructorWithTooManyParameters", "ParameterNumber", "PMD.ExcessiveParameterList"})
    public CCRequestBodyDefault(
        String model,
        List<ChatMessage> messages,
        Supplier<Optional<Integer>> llmMaxTokens,
        Supplier<Optional<Integer>> llmMaxCompletionTokens,
        Supplier<Optional<Float>> llmTemperature,
        Supplier<Optional<Float>> llmFrequencyPenalty,
        Supplier<Optional<Float>> llmTopP
    ) {
        this.model = model;
        this.messages = Collections.unmodifiableList(messages);
        this.llmMaxTokens = llmMaxTokens;
        this.llmMaxCompletionTokens = llmMaxCompletionTokens;
        this.llmTemperature = llmTemperature;
        this.llmFrequencyPenalty = llmFrequencyPenalty;
        this.llmTopP = llmTopP;
    }

    /**
     * Constructs an instance of this class.
     *
     * @param model    same as {@link LLMConfig#llm_model()}
     * @param messages list of consecutive {@link ChatMessage}-s from a single {@link Chat} out of which a
     *                 {@link ChatCompletion} should be generated
     */
    public CCRequestBodyDefault(String model, List<ChatMessage> messages) {
        this(model, messages, Optional::empty, Optional::empty, Optional::empty, Optional::empty, Optional::empty);
    }

    @Override
    @JsonProperty("model")
    public String model() {
        return model;
    }

    @Override
    @JsonProperty("messages")
    public List<ChatMessage> messages() {
        return messages;
    }

    @JsonProperty("max_tokens")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Optional<Integer> llmMaxTokens() {
        return llmMaxTokens.get();
    }

    @JsonProperty("max_completion_tokens")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Optional<Integer> llmMaxCompletionTokens() {
        return llmMaxCompletionTokens.get();
    }

    @JsonProperty("temperature")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Optional<Float> llmTemperature() {
        return llmTemperature.get();
    }

    @JsonProperty("frequency_penalty")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Optional<Float> llmFrequencyPenalty() {
        return llmFrequencyPenalty.get();
    }

    @JsonProperty("top_p")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private Optional<Float> llmTopP() {
        return llmTopP.get();
    }

    @SneakyThrows
    @Override
    public String asJSON() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        return objectMapper.writeValueAsString(this);
    }
}
